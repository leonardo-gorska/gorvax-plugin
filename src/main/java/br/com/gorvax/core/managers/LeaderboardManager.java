package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B7 — Gerencia rankings / leaderboards com cache periódico.
 * Cada categoria mantém um top-N ordenado, atualizado a cada X minutos
 * (configurável em config.yml → leaderboard.refresh_interval_minutes).
 */
public class LeaderboardManager {

    /**
     * Representa uma entrada no leaderboard.
     */
    public record LeaderboardEntry(UUID uuid, String name, double value) {}

    private final GorvaxCore plugin;
    private final Map<String, List<LeaderboardEntry>> cache = new ConcurrentHashMap<>();
    private int topSize = 10;

    // Categorias suportadas
    public static final String[] CATEGORIES = {
            "kills", "mortes", "kdr", "riqueza",
            "playtime", "bosses", "reinos", "streak"
    };

    public LeaderboardManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.topSize = plugin.getConfig().getInt("leaderboard.top_size", 10);

        int refreshMinutes = plugin.getConfig().getInt("leaderboard.refresh_interval_minutes", 5);
        long refreshTicks = refreshMinutes * 60L * 20L;

        // Primeiro rebuild 10s após o servidor ligar (200 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::rebuild, 200L, refreshTicks);

        plugin.getLogger().info("[GorvaxCore] LeaderboardManager carregado (Refresh: " + refreshMinutes + "min, Top: " + topSize + ").");
    }

    /**
     * Reconstrói todo o cache de rankings a partir dos dados de playerdata.yml.
     * Executado na Main Thread para evitar race conditions com o YAML config.
     */
    public void rebuild() {
        PlayerDataManager pdm = plugin.getPlayerDataManager();
        FileConfiguration dataConfig = pdm.getDataConfig();
        if (dataConfig == null) return;

        List<LeaderboardEntry> killsList = new ArrayList<>();
        List<LeaderboardEntry> deathsList = new ArrayList<>();
        List<LeaderboardEntry> kdrList = new ArrayList<>();
        List<LeaderboardEntry> wealthList = new ArrayList<>();
        List<LeaderboardEntry> playtimeList = new ArrayList<>();
        List<LeaderboardEntry> bossesList = new ArrayList<>();
        List<LeaderboardEntry> streakList = new ArrayList<>();

        // Iterar sobre todos os jogadores registrados no playerdata.yml
        for (String uuidStr : dataConfig.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue; // Ignorar chaves que não são UUIDs
            }

            String path = uuidStr;
            String name = plugin.getPlayerName(uuid);

            int kills = dataConfig.getInt(path + ".total_kills", 0);
            int deaths = dataConfig.getInt(path + ".total_deaths", 0);
            double kdr = deaths > 0 ? (double) kills / deaths : kills;
            long playTime = dataConfig.getLong(path + ".total_play_time", 0L);
            int bossesKilled = dataConfig.getInt(path + ".bosses_killed", 0);
            int highestStreak = dataConfig.getInt(path + ".highest_kill_streak", 0);

            killsList.add(new LeaderboardEntry(uuid, name, kills));
            deathsList.add(new LeaderboardEntry(uuid, name, deaths));
            kdrList.add(new LeaderboardEntry(uuid, name, kdr));
            playtimeList.add(new LeaderboardEntry(uuid, name, playTime));
            bossesList.add(new LeaderboardEntry(uuid, name, bossesKilled));
            streakList.add(new LeaderboardEntry(uuid, name, highestStreak));

            // Riqueza via Vault
            if (GorvaxCore.getEconomy() != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                double balance = GorvaxCore.getEconomy().getBalance(op);
                wealthList.add(new LeaderboardEntry(uuid, name, balance));
            }
        }

        // Ordenar cada lista desc e truncar para top-N
        Comparator<LeaderboardEntry> desc = (a, b) -> Double.compare(b.value(), a.value());

        cache.put("kills", trimAndSort(killsList, desc));
        cache.put("mortes", trimAndSort(deathsList, desc));
        cache.put("kdr", trimAndSort(kdrList, desc));
        cache.put("riqueza", trimAndSort(wealthList, desc));
        cache.put("playtime", trimAndSort(playtimeList, desc));
        cache.put("bosses", trimAndSort(bossesList, desc));
        cache.put("streak", trimAndSort(streakList, desc));

        // Reinos — ranking especial baseado em membros × chunks
        rebuildKingdomRanking();
    }

    /**
     * Reconstrói o ranking de reinos baseado em (membros × chunks protegidos).
     */
    private void rebuildKingdomRanking() {
        var km = plugin.getKingdomManager();
        if (km == null || km.getData() == null) {
            cache.put("reinos", Collections.emptyList());
            return;
        }

        ConfigurationSection kingdoms = km.getData().getConfigurationSection("kingdoms");
        if (kingdoms == null) {
            cache.put("reinos", Collections.emptyList());
            return;
        }

        List<LeaderboardEntry> reinoList = new ArrayList<>();
        for (String kingdomId : kingdoms.getKeys(false)) {
            String nome = km.getNome(kingdomId);
            if (nome == null || nome.isEmpty()) nome = kingdomId;

            int membros = km.getSuditosCount(kingdomId);
            // Calcular chunks do reino — usar o claim principal
            int chunks = kingdoms.getInt(kingdomId + ".chunks", 0);
            if (chunks == 0) {
                // Fallback: estimar como área / 256 (16×16 chunks)
                int area = kingdoms.getInt(kingdomId + ".area", 0);
                chunks = Math.max(1, area / 256);
            }

            double score = membros * chunks;
            // Usar UUID aleatório baseado no kingdomId para consistência
            UUID fakeUuid;
            try {
                fakeUuid = UUID.nameUUIDFromBytes(kingdomId.getBytes());
            } catch (Exception e) {
                fakeUuid = UUID.randomUUID();
            }
            reinoList.add(new LeaderboardEntry(fakeUuid, nome, score));
        }

        Comparator<LeaderboardEntry> desc = (a, b) -> Double.compare(b.value(), a.value());
        cache.put("reinos", trimAndSort(reinoList, desc));
    }

    /**
     * Ordena e limita a lista ao topSize.
     */
    private List<LeaderboardEntry> trimAndSort(List<LeaderboardEntry> list, Comparator<LeaderboardEntry> comp) {
        list.sort(comp);
        if (list.size() > topSize) {
            return Collections.unmodifiableList(new ArrayList<>(list.subList(0, topSize)));
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /**
     * Retorna o top para a categoria especificada.
     *
     * @param category Nome da categoria (kills, mortes, kdr, riqueza, playtime, bosses, reinos, streak)
     * @return Lista de entradas ordenadas desc, ou lista vazia se a categoria não existe
     */
    public List<LeaderboardEntry> getTop(String category) {
        return cache.getOrDefault(category.toLowerCase(), Collections.emptyList());
    }

    /**
     * Retorna a posição de um jogador em uma categoria (1-indexed).
     * Retorna -1 se o jogador não está no ranking completo.
     * Nota: como o cache só guarda top-N, apenas verifica dentro do top.
     */
    public int getPosition(String category, UUID uuid) {
        List<LeaderboardEntry> list = getTop(category);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Verifica se a categoria é válida.
     */
    public boolean isValidCategory(String category) {
        for (String cat : CATEGORIES) {
            if (cat.equalsIgnoreCase(category)) return true;
        }
        return false;
    }

    /**
     * Retorna o tamanho configurado do top.
     */
    public int getTopSize() {
        return topSize;
    }
}
