package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B17.2 — Sistema de Votação no Reino.
 * Permite que o Rei crie votações e membros votem sim/não.
 * Persistência em votes.yml com expiração automática.
 */
public class VoteManager {

    private final GorvaxCore plugin;
    private final File votesFile;
    private final Map<String, KingdomVote> activeVotes = new ConcurrentHashMap<>();
    private final int expireHours;

    public VoteManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.votesFile = new File(plugin.getDataFolder(), "votes.yml");
        this.expireHours = plugin.getConfig().getInt("vote.expire_hours", 24);
        loadVotes();

        // Verificar expiração a cada minuto
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiration, 1200L, 1200L);
    }

    // --- Modelo de Dados ---

    public static class KingdomVote {
        public final String kingdomId;
        public final String question;
        public final UUID creatorUUID;
        public final long createdAt;
        public final long expiresAt;
        public final Map<UUID, Boolean> votes;

        public KingdomVote(String kingdomId, String question, UUID creatorUUID,
                           long createdAt, long expiresAt) {
            this.kingdomId = kingdomId;
            this.question = question;
            this.creatorUUID = creatorUUID;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.votes = new ConcurrentHashMap<>();
        }

        public int getYesCount() {
            return (int) votes.values().stream().filter(v -> v).count();
        }

        public int getNoCount() {
            return (int) votes.values().stream().filter(v -> !v).count();
        }

        public int getTotalVotes() {
            return votes.size();
        }

        public boolean hasVoted(UUID playerUUID) {
            return votes.containsKey(playerUUID);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }

        public String getRemainingTime() {
            long remaining = expiresAt - System.currentTimeMillis();
            if (remaining <= 0) return "Expirada";
            long hours = remaining / (1000 * 3600);
            long minutes = (remaining % (1000 * 3600)) / (1000 * 60);
            if (hours > 0) return hours + "h " + minutes + "m";
            return minutes + "m";
        }
    }

    // --- API Pública ---

    /**
     * Cria uma nova votação no reino.
     * 
     * @return true se criado com sucesso, false se já há votação ativa
     */
    public boolean createVote(String kingdomId, String question, UUID creatorUUID) {
        if (activeVotes.containsKey(kingdomId)) return false;

        long now = System.currentTimeMillis();
        long expiresAt = now + (expireHours * 3600L * 1000L);

        KingdomVote vote = new KingdomVote(kingdomId, question, creatorUUID, now, expiresAt);
        activeVotes.put(kingdomId, vote);
        return true;
    }

    /**
     * Registra um voto.
     * 
     * @return 0=sucesso, 1=sem votação ativa, 2=já votou, 3=expirada
     */
    public int castVote(String kingdomId, UUID playerUUID, boolean voteYes) {
        KingdomVote vote = activeVotes.get(kingdomId);
        if (vote == null) return 1;
        if (vote.isExpired()) {
            activeVotes.remove(kingdomId);
            return 3;
        }
        if (vote.hasVoted(playerUUID)) return 2;

        vote.votes.put(playerUUID, voteYes);
        return 0;
    }

    /**
     * Retorna a votação ativa de um reino, ou null.
     */
    public KingdomVote getActiveVote(String kingdomId) {
        KingdomVote vote = activeVotes.get(kingdomId);
        if (vote != null && vote.isExpired()) {
            activeVotes.remove(kingdomId);
            return null;
        }
        return vote;
    }

    /**
     * Cancela a votação de um reino.
     * 
     * @return true se cancelado, false se não havia votação
     */
    public boolean cancelVote(String kingdomId) {
        return activeVotes.remove(kingdomId) != null;
    }

    // --- Expiração ---

    private void checkExpiration() {
        activeVotes.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                // Broadcast resultado para membros online do reino
                broadcastResult(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void broadcastResult(KingdomVote vote) {
        var msg = plugin.getMessageManager();
        var km = plugin.getKingdomManager();

        // Notificar rei
        UUID rei = km.getRei(vote.kingdomId);
        if (rei != null) {
            var king = Bukkit.getPlayer(rei);
            if (king != null && king.isOnline()) {
                msg.send(king, "vote.expired_result",
                        vote.question, vote.getYesCount(), vote.getNoCount());
            }
        }

        // Notificar súditos
        for (String memberStr : km.getSuditosList(vote.kingdomId)) {
            try {
                UUID memberUUID = UUID.fromString(memberStr);
                var player = Bukkit.getPlayer(memberUUID);
                if (player != null && player.isOnline()) {
                    msg.send(player, "vote.expired_result",
                            vote.question, vote.getYesCount(), vote.getNoCount());
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // --- Persistência ---

    private void loadVotes() {
        if (!votesFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(votesFile);
        ConfigurationSection votesSection = yaml.getConfigurationSection("votes");
        if (votesSection == null) return;

        for (String kingdomId : votesSection.getKeys(false)) {
            ConfigurationSection section = votesSection.getConfigurationSection(kingdomId);
            if (section == null) continue;

            try {
                String question = section.getString("question", "");
                UUID creatorUUID = UUID.fromString(section.getString("creator", ""));
                long createdAt = section.getLong("created_at", 0);
                long expiresAt = section.getLong("expires_at", 0);

                // Pular se já expirou
                if (System.currentTimeMillis() >= expiresAt) continue;

                KingdomVote vote = new KingdomVote(kingdomId, question, creatorUUID, createdAt, expiresAt);

                ConfigurationSection votesData = section.getConfigurationSection("votes_data");
                if (votesData != null) {
                    for (String uuidStr : votesData.getKeys(false)) {
                        vote.votes.put(UUID.fromString(uuidStr), votesData.getBoolean(uuidStr));
                    }
                }

                activeVotes.put(kingdomId, vote);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[VoteManager] Erro ao carregar votação: " + kingdomId);
            }
        }

        plugin.getLogger().info("[VoteManager] Carregadas " + activeVotes.size() + " votações ativas.");
    }

    public void saveSync() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<String, KingdomVote> entry : activeVotes.entrySet()) {
            KingdomVote vote = entry.getValue();
            if (vote.isExpired()) continue;

            String path = "votes." + entry.getKey();
            yaml.set(path + ".question", vote.question);
            yaml.set(path + ".creator", vote.creatorUUID.toString());
            yaml.set(path + ".created_at", vote.createdAt);
            yaml.set(path + ".expires_at", vote.expiresAt);

            for (Map.Entry<UUID, Boolean> voteEntry : vote.votes.entrySet()) {
                yaml.set(path + ".votes_data." + voteEntry.getKey().toString(), voteEntry.getValue());
            }
        }

        try {
            yaml.save(votesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[VoteManager] Erro ao salvar votes.yml: " + e.getMessage());
        }
    }

    public void reload() {
        activeVotes.clear();
        loadVotes();
    }
}
