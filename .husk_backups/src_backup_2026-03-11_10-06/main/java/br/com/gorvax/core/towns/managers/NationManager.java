package br.com.gorvax.core.towns.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * B19 — Gerencia o sistema de Nações (meta-reinos).
 * Nações agrupam múltiplos reinos sob um "Imperador" com banco, buffs e chat
 * compartilhados.
 * Persistência: nations.yml
 */
public class NationManager {

    // ========================================================================
    // Modelo: Nation (inner class)
    // ========================================================================

    /**
     * Representa uma nação — agrupamento de reinos.
     */
    public static class Nation {
        private final String id;
        private String name;
        private String founderKingdomId; // Reino fundador (Imperador)
        private final Set<String> memberKingdomIds; // Inclui o fundador
        private double bankBalance;
        private final long createdAt;

        public Nation(String id, String name, String founderKingdomId) {
            this.id = id;
            this.name = name;
            this.founderKingdomId = founderKingdomId;
            this.memberKingdomIds = ConcurrentHashMap.newKeySet();
            this.memberKingdomIds.add(founderKingdomId);
            this.bankBalance = 0.0;
            this.createdAt = System.currentTimeMillis();
        }

        // Construtor para desserialização
        public Nation(String id, String name, String founderKingdomId,
                Set<String> members, double bank, long created) {
            this.id = id;
            this.name = name;
            this.founderKingdomId = founderKingdomId;
            this.memberKingdomIds = ConcurrentHashMap.newKeySet();
            this.memberKingdomIds.addAll(members);
            this.bankBalance = bank;
            this.createdAt = created;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFounderKingdomId() {
            return founderKingdomId;
        }

        public void setFounderKingdomId(String id) {
            this.founderKingdomId = id;
        }

        public Set<String> getMemberKingdomIds() {
            return Collections.unmodifiableSet(memberKingdomIds);
        }

        public double getBankBalance() {
            return bankBalance;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void addKingdom(String kingdomId) {
            memberKingdomIds.add(kingdomId);
        }

        public void removeKingdom(String kingdomId) {
            memberKingdomIds.remove(kingdomId);
        }

        public boolean hasKingdom(String kingdomId) {
            return memberKingdomIds.contains(kingdomId);
        }

        public int getKingdomCount() {
            return memberKingdomIds.size();
        }

        public void depositBank(double amount) {
            this.bankBalance += amount;
        }

        public boolean withdrawBank(double amount) {
            if (this.bankBalance < amount)
                return false;
            this.bankBalance -= amount;
            return true;
        }
    }

    // ========================================================================
    // Modelo: NationInvite
    // ========================================================================

    /**
     * Convite pendente para um reino entrar em uma nação.
     */
    public static class NationInvite {
        private final String nationId;
        private final String kingdomId;
        private final long expireAt;

        public NationInvite(String nationId, String kingdomId, long expireAt) {
            this.nationId = nationId;
            this.kingdomId = kingdomId;
            this.expireAt = expireAt;
        }

        public String getNationId() {
            return nationId;
        }

        public String getKingdomId() {
            return kingdomId;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        public long getRemainingSeconds() {
            long remain = (expireAt - System.currentTimeMillis()) / 1000;
            return Math.max(0, remain);
        }
    }

    // ========================================================================
    // Campos
    // ========================================================================

    private final GorvaxCore plugin;
    private final Map<String, Nation> nations = new ConcurrentHashMap<>(); // nationId -> Nation
    private final Map<String, String> kingdomToNation = new ConcurrentHashMap<>(); // kingdomId -> nationId
    private final Map<String, NationInvite> pendingInvites = new ConcurrentHashMap<>(); // kingdomId -> invite

    // Configurações carregadas do config.yml
    private double creationCost;
    private int inviteExpireSeconds;
    private int maxKingdoms;
    private double visitCostReduction;
    private String buffLevel2;
    private String buffLevel3;

    private File dataFile;

    // ========================================================================
    // Inicialização
    // ========================================================================

    public NationManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        plugin.getLogger().info("[GorvaxCore] NationManager inicializado (B19 — Sistema de Nações).");
    }

    /**
     * Carrega configurações do config.yml.
     */
    private void loadConfig() {
        var config = plugin.getConfig();
        this.creationCost = config.getDouble("nations.creation_cost", 50000.0);
        this.inviteExpireSeconds = config.getInt("nations.invite_expire", 60);
        this.maxKingdoms = config.getInt("nations.max_kingdoms", 10);
        this.visitCostReduction = config.getDouble("nations.visit_cost_reduction", 0.5);
        this.buffLevel2 = config.getString("nations.buffs.level_2", "SPEED");
        this.buffLevel3 = config.getString("nations.buffs.level_3", "DAMAGE_RESISTANCE");
    }

    /**
     * Recarrega configurações e dados.
     */
    public void reload() {
        loadConfig();
        loadData();
    }

    // ========================================================================
    // Persistência (nations.yml)
    // ========================================================================

    /**
     * Carrega dados de nations.yml.
     */
    private void loadData() {
        this.dataFile = new File(plugin.getDataFolder(), "nations.yml");
        if (!dataFile.exists())
            return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection nationsSection = yaml.getConfigurationSection("nations");
        if (nationsSection == null)
            return;

        nations.clear();
        kingdomToNation.clear();

        for (String nationId : nationsSection.getKeys(false)) {
            ConfigurationSection ns = nationsSection.getConfigurationSection(nationId);
            if (ns == null)
                continue;

            String name = ns.getString("name", "Desconhecida");
            String founder = ns.getString("founder");
            double bank = ns.getDouble("bank", 0.0);
            long created = ns.getLong("created", System.currentTimeMillis());
            List<String> members = ns.getStringList("members");

            Nation nation = new Nation(nationId, name, founder,
                    new HashSet<>(members), bank, created);
            nations.put(nationId, nation);

            for (String kid : members) {
                kingdomToNation.put(kid, nationId);
            }
        }
        plugin.getLogger().info("[GorvaxCore] Carregadas " + nations.size() + " nações.");
    }

    /**
     * Salva dados em nations.yml (assíncrono).
     */
    public void save() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    /**
     * Salva dados em nations.yml (síncrono — usado no onDisable).
     */
    public void saveSync() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<String, Nation> entry : nations.entrySet()) {
            String path = "nations." + entry.getKey();
            Nation n = entry.getValue();
            yaml.set(path + ".name", n.getName());
            yaml.set(path + ".founder", n.getFounderKingdomId());
            yaml.set(path + ".bank", n.getBankBalance());
            yaml.set(path + ".created", n.getCreatedAt());
            yaml.set(path + ".members", new ArrayList<>(n.getMemberKingdomIds()));
        }

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[GorvaxCore] Erro ao salvar nations.yml", e);
        }
    }

    // ========================================================================
    // CRUD — Criação, Dissolução, Convite, Saída
    // ========================================================================

    /**
     * Cria uma nova nação. Cobra o custo do jogador.
     * 
     * @return a nação criada, ou null se falhou
     */
    public Nation createNation(Player player, String name) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        // Verificar se jogador tem reino
        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return null;
        }

        // Verificar se é o Rei
        String kingdomId = kingdom.getId();
        if (!plugin.getKingdomManager().isRei(kingdomId, uuid)) {
            msg.send(player, "nation.error_not_king");
            return null;
        }

        // Verificar se já está em uma nação
        if (kingdomToNation.containsKey(kingdomId)) {
            msg.send(player, "nation.error_already_in_nation");
            return null;
        }

        // Validar nome
        if (name == null || name.length() < 3 || name.length() > 20
                || !name.matches("[a-zA-Z0-9_]+")) {
            msg.send(player, "nation.error_invalid_name");
            return null;
        }

        // Verificar nome duplicado
        if (getNationByName(name) != null) {
            msg.send(player, "nation.error_name_taken");
            return null;
        }

        // Cobrar custo
        Economy econ = GorvaxCore.getEconomy();
        if (econ == null || !econ.has(player, creationCost)) {
            msg.send(player, "nation.error_no_money", String.format("%.2f", creationCost));
            return null;
        }
        econ.withdrawPlayer(player, creationCost);

        // Criar nação
        String nationId = UUID.randomUUID().toString().substring(0, 8);
        Nation nation = new Nation(nationId, name, kingdomId);
        nations.put(nationId, nation);
        kingdomToNation.put(kingdomId, nationId);

        save();

        msg.send(player, "nation.created", name);
        Bukkit.broadcast(LegacyComponentSerializer.legacySection()
                .deserialize(msg.get("nation.created_broadcast", name, player.getName())));

        return nation;
    }

    /**
     * Dissolve uma nação. Apenas o Imperador pode.
     */
    public boolean disbandNation(Player player) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();
        Nation nation = getNationByKingdom(kingdomId);
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return false;
        }

        // Apenas o Imperador (fundador) pode dissolver
        if (!nation.getFounderKingdomId().equals(kingdomId)) {
            msg.send(player, "nation.error_not_emperor");
            return false;
        }

        String nationName = nation.getName();

        // Remover todos os reinos do mapeamento
        for (String kid : nation.getMemberKingdomIds()) {
            kingdomToNation.remove(kid);
        }
        nations.remove(nation.getId());

        save();

        msg.send(player, "nation.disbanded", nationName);
        Bukkit.broadcast(LegacyComponentSerializer.legacySection()
                .deserialize(msg.get("nation.disbanded_broadcast", nationName)));

        return true;
    }

    /**
     * Convida um reino para a nação.
     */
    public boolean inviteKingdom(Player player, String targetKingdomName) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();
        Nation nation = getNationByKingdom(kingdomId);
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return false;
        }

        // Apenas Imperador pode convidar
        if (!nation.getFounderKingdomId().equals(kingdomId)) {
            msg.send(player, "nation.error_not_emperor");
            return false;
        }

        // Verificar limite de reinos
        if (nation.getKingdomCount() >= maxKingdoms) {
            msg.send(player, "nation.error_max_kingdoms", String.valueOf(maxKingdoms));
            return false;
        }

        // Encontrar o reino-alvo
        String targetKingdomId = plugin.getKingdomManager().tryFindKingdomIdByName(targetKingdomName);
        if (targetKingdomId == null) {
            msg.send(player, "nation.error_kingdom_not_found");
            return false;
        }

        // Verificar se já está em uma nação
        if (kingdomToNation.containsKey(targetKingdomId)) {
            msg.send(player, "nation.error_target_in_nation");
            return false;
        }

        // Verificar se já tem convite pendente
        NationInvite existing = pendingInvites.get(targetKingdomId);
        if (existing != null && !existing.isExpired()) {
            msg.send(player, "nation.error_invite_pending");
            return false;
        }

        // Criar convite
        long expireAt = System.currentTimeMillis() + (inviteExpireSeconds * 1000L);
        NationInvite invite = new NationInvite(nation.getId(), targetKingdomId, expireAt);
        pendingInvites.put(targetKingdomId, invite);

        msg.send(player, "nation.invite_sent", targetKingdomName,
                String.valueOf(inviteExpireSeconds));

        // Notificar o Rei do reino-alvo
        UUID targetKingUuid = plugin.getKingdomManager().getRei(targetKingdomId);
        if (targetKingUuid != null) {
            Player targetKing = Bukkit.getPlayer(targetKingUuid);
            if (targetKing != null) {
                msg.send(targetKing, "nation.invite_received", nation.getName());
            }
        }

        // Agendar expiração
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            NationInvite pending = pendingInvites.get(targetKingdomId);
            if (pending != null && pending.isExpired()) {
                pendingInvites.remove(targetKingdomId);
            }
        }, inviteExpireSeconds * 20L);

        return true;
    }

    /**
     * Aceita um convite de nação (chamado pelo Rei do reino convidado).
     */
    public boolean acceptInvite(Player player) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();

        // Verificar se é o Rei
        if (!plugin.getKingdomManager().isRei(kingdomId, uuid)) {
            msg.send(player, "nation.error_not_king");
            return false;
        }

        NationInvite invite = pendingInvites.get(kingdomId);
        if (invite == null || invite.isExpired()) {
            pendingInvites.remove(kingdomId);
            msg.send(player, "nation.error_no_invite");
            return false;
        }

        Nation nation = nations.get(invite.getNationId());
        if (nation == null) {
            pendingInvites.remove(kingdomId);
            msg.send(player, "nation.error_nation_disbanded");
            return false;
        }

        // Pode ter limite agora
        if (nation.getKingdomCount() >= maxKingdoms) {
            pendingInvites.remove(kingdomId);
            msg.send(player, "nation.error_max_kingdoms", String.valueOf(maxKingdoms));
            return false;
        }

        // Aceitar
        nation.addKingdom(kingdomId);
        kingdomToNation.put(kingdomId, nation.getId());
        pendingInvites.remove(kingdomId);

        save();

        String kingdomName = plugin.getKingdomManager().getNome(kingdomId);
        msg.send(player, "nation.invite_accepted", nation.getName());

        // Notificar Imperador
        UUID emperorUuid = plugin.getKingdomManager().getRei(nation.getFounderKingdomId());
        if (emperorUuid != null) {
            Player emperor = Bukkit.getPlayer(emperorUuid);
            if (emperor != null) {
                msg.send(emperor, "nation.invite_accepted_notify",
                        kingdomName != null ? kingdomName : "Desconhecido");
            }
        }

        return true;
    }

    /**
     * Recusa um convite de nação.
     */
    public boolean denyInvite(Player player) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();
        NationInvite invite = pendingInvites.get(kingdomId);
        if (invite == null || invite.isExpired()) {
            pendingInvites.remove(kingdomId);
            msg.send(player, "nation.error_no_invite");
            return false;
        }

        String nationName = "Desconhecida";
        Nation n = nations.get(invite.getNationId());
        if (n != null)
            nationName = n.getName();

        pendingInvites.remove(kingdomId);
        msg.send(player, "nation.invite_denied", nationName);

        return true;
    }

    /**
     * Reino sai da nação (o Rei decide).
     */
    public boolean leaveNation(Player player) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();

        // Verificar se é o Rei
        if (!plugin.getKingdomManager().isRei(kingdomId, uuid)) {
            msg.send(player, "nation.error_not_king");
            return false;
        }

        Nation nation = getNationByKingdom(kingdomId);
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return false;
        }

        // Imperador não pode sair — tem que dissolver
        if (nation.getFounderKingdomId().equals(kingdomId)) {
            msg.send(player, "nation.error_emperor_cannot_leave");
            return false;
        }

        nation.removeKingdom(kingdomId);
        kingdomToNation.remove(kingdomId);

        save();

        String kingdomName = plugin.getKingdomManager().getNome(kingdomId);
        msg.send(player, "nation.left", nation.getName());

        // Notificar Imperador
        UUID emperorUuid = plugin.getKingdomManager().getRei(nation.getFounderKingdomId());
        if (emperorUuid != null) {
            Player emperor = Bukkit.getPlayer(emperorUuid);
            if (emperor != null) {
                msg.send(emperor, "nation.left_notify",
                        kingdomName != null ? kingdomName : "Desconhecido");
            }
        }

        return true;
    }

    /**
     * Imperador expulsa um reino da nação.
     */
    public boolean kickKingdom(Player player, String targetKingdomName) {
        var msg = plugin.getMessageManager();
        UUID uuid = player.getUniqueId();

        Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();
        Nation nation = getNationByKingdom(kingdomId);
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return false;
        }

        if (!nation.getFounderKingdomId().equals(kingdomId)) {
            msg.send(player, "nation.error_not_emperor");
            return false;
        }

        String targetKingdomId = plugin.getKingdomManager().tryFindKingdomIdByName(targetKingdomName);
        if (targetKingdomId == null || !nation.hasKingdom(targetKingdomId)) {
            msg.send(player, "nation.error_kingdom_not_in_nation");
            return false;
        }

        if (targetKingdomId.equals(nation.getFounderKingdomId())) {
            msg.send(player, "nation.error_cannot_kick_self");
            return false;
        }

        nation.removeKingdom(targetKingdomId);
        kingdomToNation.remove(targetKingdomId);

        save();

        msg.send(player, "nation.kicked", targetKingdomName);

        // Notificar quem foi expulso
        UUID targetKingUuid = plugin.getKingdomManager().getRei(targetKingdomId);
        if (targetKingUuid != null) {
            Player targetKing = Bukkit.getPlayer(targetKingUuid);
            if (targetKing != null) {
                msg.send(targetKing, "nation.kicked_notify", nation.getName());
            }
        }

        return true;
    }

    // ========================================================================
    // Banco da Nação
    // ========================================================================

    /**
     * Deposita dinheiro no banco da nação.
     */
    public boolean depositBank(Player player, double amount) {
        var msg = plugin.getMessageManager();

        if (amount <= 0) {
            msg.send(player, "nation.error_invalid_value");
            return false;
        }

        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        Nation nation = getNationByKingdom(kingdom.getId());
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return false;
        }

        Economy econ = GorvaxCore.getEconomy();
        if (econ == null || !econ.has(player, amount)) {
            msg.send(player, "nation.error_insufficient_funds");
            return false;
        }

        econ.withdrawPlayer(player, amount);
        nation.depositBank(amount);

        save();

        msg.send(player, "nation.bank_deposit_success",
                String.format("%.2f", amount), String.format("%.2f", nation.getBankBalance()));

        return true;
    }

    /**
     * Saca dinheiro do banco da nação (apenas Imperador).
     */
    public boolean withdrawBank(Player player, double amount) {
        var msg = plugin.getMessageManager();

        if (amount <= 0) {
            msg.send(player, "nation.error_invalid_value");
            return false;
        }

        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) {
            msg.send(player, "nation.error_no_kingdom");
            return false;
        }

        String kingdomId = kingdom.getId();
        Nation nation = getNationByKingdom(kingdomId);
        if (nation == null) {
            msg.send(player, "nation.error_not_in_nation");
            return false;
        }

        // Apenas Imperador
        if (!nation.getFounderKingdomId().equals(kingdomId)) {
            msg.send(player, "nation.error_not_emperor");
            return false;
        }

        if (!nation.withdrawBank(amount)) {
            msg.send(player, "nation.error_bank_insufficient");
            return false;
        }

        Economy econ = GorvaxCore.getEconomy();
        if (econ != null) {
            econ.depositPlayer(player, amount);
        }

        save();

        msg.send(player, "nation.bank_withdraw_success",
                String.format("%.2f", amount), String.format("%.2f", nation.getBankBalance()));

        return true;
    }

    // ========================================================================
    // Buffs por Nível
    // ========================================================================

    /**
     * Calcula o nível da nação (baseado na quantidade total de membros
     * de todos os reinos na nação).
     */
    public int getNationLevel(Nation nation) {
        int totalMembers = 0;
        for (String kid : nation.getMemberKingdomIds()) {
            totalMembers += plugin.getKingdomManager().getSuditosCount(kid);
            // Contar o Rei também
            if (plugin.getKingdomManager().getRei(kid) != null) {
                totalMembers++;
            }
        }
        // Nível = floor(totalMembers / 5), mínimo 1
        return Math.max(1, totalMembers / 5);
    }

    /**
     * Aplica os buffs da nação para um jogador online (chamado periodicamente
     * pela KingdomEffectsTask ou similar).
     */
    public void applyBuffs(Player player) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null)
            return;

        Nation nation = getNationByKingdom(kingdom.getId());
        if (nation == null)
            return;

        int level = getNationLevel(nation);

        // Nível 2+: buff primário (SPEED por padrão)
        if (level >= 2) {
            try {
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(buffLevel2.toLowerCase()));
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, 100, 0, true, false));
                }
            } catch (Exception ignored) {
            }
        }

        // Nível 3+: buff secundário (DAMAGE_RESISTANCE por padrão)
        if (level >= 3) {
            try {
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(buffLevel3.toLowerCase()));
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, 100, 0, true, false));
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ========================================================================
    // Consultas
    // ========================================================================

    /**
     * Retorna a nação que contém o reino, ou null.
     */
    public Nation getNationByKingdom(String kingdomId) {
        String nationId = kingdomToNation.get(kingdomId);
        if (nationId == null)
            return null;
        return nations.get(nationId);
    }

    /**
     * Busca nação por nome (case-insensitive).
     */
    public Nation getNationByName(String name) {
        if (name == null)
            return null;
        for (Nation n : nations.values()) {
            if (n.getName().equalsIgnoreCase(name))
                return n;
        }
        return null;
    }

    /**
     * Retorna a nação pelo ID.
     */
    public Nation getNation(String nationId) {
        return nations.get(nationId);
    }

    /**
     * Retorna todas as nações.
     */
    public Collection<Nation> getAllNations() {
        return Collections.unmodifiableCollection(nations.values());
    }

    /**
     * Retorna os nomes de todas as nações.
     */
    public List<String> getNationNames() {
        return nations.values().stream()
                .map(Nation::getName)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se dois reinos estão na mesma nação.
     */
    public boolean areInSameNation(String kingdomId1, String kingdomId2) {
        String n1 = kingdomToNation.get(kingdomId1);
        String n2 = kingdomToNation.get(kingdomId2);
        return n1 != null && n1.equals(n2);
    }

    /**
     * Retorna o desconto de custo de visita para membros da mesma nação.
     */
    public double getVisitCostReduction() {
        return visitCostReduction;
    }

    /**
     * Retorna o custo de criação.
     */
    public double getCreationCost() {
        return creationCost;
    }
}
