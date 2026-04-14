package br.com.gorvax.core.storage;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.*;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B18 — Implementação YAML do DataStore.
 * Mantém o comportamento exato do sistema de persistência legado
 * como fallback e para facilitar migração.
 */
public class YamlDataStore implements DataStore {

    private final GorvaxCore plugin;

    public YamlDataStore(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        plugin.getLogger().info("[Storage] Backend YAML inicializado (modo legado/fallback).");
    }

    @Override
    public void shutdown() {
        // Nada especial — YAML não mantém conexões
    }

    @Override
    public StorageType getType() {
        return StorageType.YAML;
    }

    // === Claims ===

    @Override
    public List<Claim> loadClaims() {
        List<Claim> result = new ArrayList<>();
        File file = new File(plugin.getDataFolder(), "claims.yml");
        if (!file.exists()) return result;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("claims")) return result;

        for (String id : config.getConfigurationSection("claims").getKeys(false)) {
            try {
                String ownerStr = config.getString("claims." + id + ".owner");
                if (ownerStr == null || ownerStr.isEmpty()) continue;

                UUID owner;
                try {
                    owner = UUID.fromString(ownerStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                String world = config.getString("claims." + id + ".world");
                int minX = config.getInt("claims." + id + ".minX");
                int minZ = config.getInt("claims." + id + ".minZ");
                int maxX = config.getInt("claims." + id + ".maxX");
                int maxZ = config.getInt("claims." + id + ".maxZ");
                boolean isCity = config.getBoolean("claims." + id + ".isCity");
                String name = config.getString("claims." + id + ".name");

                Claim claim = new Claim(id, owner, world, minX, minZ, maxX, maxZ, isCity, name);

                if (config.contains("claims." + id + ".cityName")) {
                    claim.setKingdomName(config.getString("claims." + id + ".cityName"));
                }
                claim.setWelcomeColor(config.getString("claims." + id + ".welcomeColor", "§b"));
                claim.setChatColor(config.getString("claims." + id + ".chatColor", "§f"));
                claim.setTagColor(config.getString("claims." + id + ".tagColor", "§e"));
                claim.setTag(config.getString("claims." + id + ".tag", null));
                claim.setTax(config.getDouble("claims." + id + ".tax", 5.0));

                claim.setEnterTitle(config.getString("claims." + id + ".msg.enterTitle", null));
                claim.setEnterSubtitle(config.getString("claims." + id + ".msg.enterSubtitle", null));
                claim.setExitTitle(config.getString("claims." + id + ".msg.exitTitle", null));
                claim.setExitSubtitle(config.getString("claims." + id + ".msg.exitSubtitle", null));

                if (config.contains("claims." + id + ".type")) {
                    try {
                        claim.setType(Claim.Type.valueOf(config.getString("claims." + id + ".type")));
                    } catch (Exception e) {
                        plugin.getLogger().fine("Tipo de claim inválido (YAML): " + e.getMessage());
                    }
                }
                claim.setPvp(config.getBoolean("claims." + id + ".pvp", false));
                claim.setResidentsPvp(config.getBoolean("claims." + id + ".residentsPvp", false));
                claim.setResidentsPvpOutside(config.getBoolean("claims." + id + ".residentsPvpOutside", false));
                claim.setPublic(config.getBoolean("claims." + id + ".isPublic", true));

                if (config.contains("claims." + id + ".parentKingdomId")) {
                    claim.setParentKingdomId(config.getString("claims." + id + ".parentKingdomId"));
                }

                if (config.contains("claims." + id + ".residentsBuild"))
                    claim.setResidentsBuild(config.getBoolean("claims." + id + ".residentsBuild"));
                if (config.contains("claims." + id + ".residentsContainer"))
                    claim.setResidentsContainer(config.getBoolean("claims." + id + ".residentsContainer"));
                if (config.contains("claims." + id + ".residentsSwitch"))
                    claim.setResidentsSwitch(config.getBoolean("claims." + id + ".residentsSwitch"));

                // Trusts
                loadClaimTrusts(config, id, claim);
                // SubPlots
                loadSubPlots(config, id, claim);

                result.add(claim);
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao carregar claim " + id + " (YAML): " + e.getMessage());
            }
        }
        return result;
    }

    private void loadClaimTrusts(YamlConfiguration config, String id, Claim claim) {
        if (!config.contains("claims." + id + ".trusts")) return;
        for (String puuid : config.getConfigurationSection("claims." + id + ".trusts").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(puuid);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Object trustData = config.get("claims." + id + ".trusts." + puuid);
            parseTrustData(claim, uuid, trustData);
        }
    }

    private void parseTrustData(Claim claim, UUID uuid, Object trustData) {
        if (trustData instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) trustData;
            for (String t : types) {
                try {
                    claim.addTrust(uuid, Claim.TrustType.valueOf(t));
                } catch (IllegalArgumentException e) {
                    if (t.equals("GERAL")) claim.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                }
            }
        } else if (trustData instanceof String typeStr) {
            Claim.TrustType type = null;
            try {
                type = Claim.TrustType.valueOf(typeStr);
            } catch (IllegalArgumentException ex) {
                if (typeStr.equals("BUILD") || typeStr.equals("GERAL")) type = Claim.TrustType.CONSTRUCAO;
                else if (typeStr.equals("ACCESS")) type = Claim.TrustType.ACESSO;
                else if (typeStr.equals("CONTAINER")) type = Claim.TrustType.CONTEINER;
                else if (typeStr.equals("MANAGER")) type = Claim.TrustType.VICE;
            }
            if (type != null) {
                applyTrustMigration(claim, uuid, type);
            }
        }
    }

    private void applyTrustMigration(Claim claim, UUID uuid, Claim.TrustType type) {
        switch (type) {
            case VICE -> claim.addTrust(uuid, Claim.TrustType.VICE);
            case GERAL -> claim.addTrust(uuid, Claim.TrustType.GERAL);
            case CONSTRUCAO -> {
                claim.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                claim.addTrust(uuid, Claim.TrustType.CONTEINER);
                claim.addTrust(uuid, Claim.TrustType.ACESSO);
            }
            case CONTEINER -> {
                claim.addTrust(uuid, Claim.TrustType.CONTEINER);
                claim.addTrust(uuid, Claim.TrustType.ACESSO);
            }
            case ACESSO -> claim.addTrust(uuid, Claim.TrustType.ACESSO);
        }
    }

    private void loadSubPlots(YamlConfiguration config, String id, Claim claim) {
        if (!config.contains("claims." + id + ".subplots")) return;
        for (String sid : config.getConfigurationSection("claims." + id + ".subplots").getKeys(false)) {
            String p = "claims." + id + ".subplots." + sid;
            String sname = config.getString(p + ".name");
            int sminX = config.getInt(p + ".minX");
            int sminZ = config.getInt(p + ".minZ");
            int smaxX = config.getInt(p + ".maxX");
            int smaxZ = config.getInt(p + ".maxZ");
            double sprice = config.getDouble(p + ".price");
            double srentPrice = config.getDouble(p + ".rentPrice");
            boolean sforSale = config.getBoolean(p + ".forSale");
            boolean sforRent = config.getBoolean(p + ".forRent");

            UUID sowner = parseUuidSafe(config.getString(p + ".owner"));
            UUID srenter = parseUuidSafe(config.getString(p + ".renter"));
            long srentExpire = config.getLong(p + ".rentExpire");

            SubPlot plot = new SubPlot(sid, sname, sminX, sminZ, smaxX, smaxZ,
                    sprice, srentPrice, sforSale, sforRent, sowner, srenter, srentExpire);

            // SubPlot Trusts
            if (config.contains(p + ".trusts")) {
                for (String tUuid : config.getConfigurationSection(p + ".trusts").getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(tUuid);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    Object tData = config.get(p + ".trusts." + tUuid);
                    if (tData instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> types = (List<String>) tData;
                        for (String t : types) {
                            try {
                                plot.addTrust(uuid, Claim.TrustType.valueOf(t));
                            } catch (IllegalArgumentException e) {
                                if (t.equals("GERAL")) plot.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                            }
                        }
                    }
                }
            }
            claim.addSubPlot(plot);
        }
    }

    @Override
    public void saveClaims(List<Claim> claims) {
        File file = new File(plugin.getDataFolder(), "claims.yml");
        File tmpFile = new File(plugin.getDataFolder(), "claims.yml.tmp");
        YamlConfiguration config = new YamlConfiguration();

        for (Claim c : claims) {
            String path = "claims." + c.getId();
            config.set(path + ".owner", c.getOwner().toString());
            config.set(path + ".world", c.getWorldName());
            config.set(path + ".minX", c.getMinX());
            config.set(path + ".minZ", c.getMinZ());
            config.set(path + ".maxX", c.getMaxX());
            config.set(path + ".maxZ", c.getMaxZ());
            config.set(path + ".isCity", c.isKingdom());
            config.set(path + ".name", c.getName());
            if (c.isKingdom() && c.getKingdomName() != null) {
                config.set(path + ".cityName", c.getKingdomName());
            }
            config.set(path + ".type", c.getType().name());
            config.set(path + ".pvp", c.isPvp());
            config.set(path + ".residentsPvp", c.isResidentsPvp());
            config.set(path + ".residentsPvpOutside", c.isResidentsPvpOutside());
            config.set(path + ".isPublic", c.isPublic());
            config.set(path + ".residentsBuild", c.isResidentsBuild());
            config.set(path + ".residentsContainer", c.isResidentsContainer());
            config.set(path + ".residentsSwitch", c.isResidentsSwitch());

            if (c.isOutpost() && c.getParentKingdomId() != null) {
                config.set(path + ".parentKingdomId", c.getParentKingdomId());
            }

            config.set(path + ".welcomeColor", c.getWelcomeColor());
            config.set(path + ".chatColor", c.getChatColor());
            config.set(path + ".tagColor", c.getTagColor());
            config.set(path + ".tag", c.getTag());
            config.set(path + ".tax", c.getTax());

            config.set(path + ".msg.enterTitle", c.getEnterTitle());
            config.set(path + ".msg.enterSubtitle", c.getEnterSubtitle());
            config.set(path + ".msg.exitTitle", c.getExitTitle());
            config.set(path + ".msg.exitSubtitle", c.getExitSubtitle());

            for (Map.Entry<UUID, Set<Claim.TrustType>> entry : c.getTrustedPlayers().entrySet()) {
                List<String> perms = new ArrayList<>();
                for (Claim.TrustType t : entry.getValue()) perms.add(t.name());
                config.set(path + ".trusts." + entry.getKey().toString(), perms);
            }

            for (SubPlot plot : c.getSubPlots()) {
                String sp = path + ".subplots." + plot.getId();
                config.set(sp + ".name", plot.getName());
                config.set(sp + ".minX", plot.getMinX());
                config.set(sp + ".minZ", plot.getMinZ());
                config.set(sp + ".maxX", plot.getMaxX());
                config.set(sp + ".maxZ", plot.getMaxZ());
                config.set(sp + ".price", plot.getPrice());
                config.set(sp + ".rentPrice", plot.getRentPrice());
                config.set(sp + ".forSale", plot.isForSale());
                config.set(sp + ".forRent", plot.isForRent());
                config.set(sp + ".owner", plot.getOwner() != null ? plot.getOwner().toString() : null);
                config.set(sp + ".renter", plot.getRenter() != null ? plot.getRenter().toString() : null);
                config.set(sp + ".rentExpire", plot.getRentExpire());

                if (!plot.getTrustedPlayers().isEmpty()) {
                    for (Map.Entry<UUID, Set<Claim.TrustType>> entry : plot.getTrustedPlayers().entrySet()) {
                        List<String> perms = new ArrayList<>();
                        for (Claim.TrustType t : entry.getValue()) perms.add(t.name());
                        config.set(sp + ".trusts." + entry.getKey().toString(), perms);
                    }
                }
            }
        }

        try {
            config.save(tmpFile);
            try {
                java.nio.file.Files.move(tmpFile.toPath(), file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                java.nio.file.Files.move(tmpFile.toPath(), file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar claims.yml (YamlDataStore): " + e.getMessage());
        }
    }

    // === Player Data ===

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = uuid.toString();
        if (!config.contains(path)) return null;
        return readPlayerData(config, uuid, path);
    }

    @Override
    public Map<UUID, PlayerData> loadAllPlayerData() {
        Map<UUID, PlayerData> result = new LinkedHashMap<>();
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) return result;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData pd = readPlayerData(config, uuid, key);
                if (pd != null) result.put(uuid, pd);
            } catch (IllegalArgumentException e) {
                // Ignora chaves inválidas
            }
        }
        return result;
    }

    private PlayerData readPlayerData(YamlConfiguration config, UUID uuid, String path) {
        int blocks = config.getInt(path + ".blocks", 100);
        boolean king = config.getBoolean(path + ".king_rank",
                config.getBoolean(path + ".mayor_rank", false));

        PlayerData pd = new PlayerData(uuid, blocks);
        pd.setKingRank(king);
        pd.setFirstJoin(config.getLong(path + ".first_join", 0L));
        pd.setTotalPlayTime(config.getLong(path + ".total_play_time", 0L));
        pd.setLastLogin(config.getLong(path + ".last_login", 0L));
        pd.setTotalBlocksBroken(config.getInt(path + ".total_blocks_broken", 0));
        pd.setTotalBlocksPlaced(config.getInt(path + ".total_blocks_placed", 0));
        pd.setTotalKills(config.getInt(path + ".total_kills", 0));
        pd.setTotalDeaths(config.getInt(path + ".total_deaths", 0));
        pd.setBossesKilled(config.getInt(path + ".bosses_killed", 0));
        pd.setBossTopDamage(config.getInt(path + ".boss_top_damage", 0));
        pd.setTotalMoneyEarned(config.getDouble(path + ".total_money_earned", 0.0));
        pd.setTotalMoneySpent(config.getDouble(path + ".total_money_spent", 0.0));
        pd.setActiveTitle(config.getString(path + ".active_title", ""));
        pd.setUnlockedTitles(new HashSet<>(config.getStringList(path + ".unlocked_titles")));
        pd.setBorderSound(config.getBoolean(path + ".border_sound", true));

        if (config.contains(path + ".achievements")) {
            Map<String, Long> achMap = new HashMap<>();
            for (String achId : config.getConfigurationSection(path + ".achievements").getKeys(false)) {
                achMap.put(achId, config.getLong(path + ".achievements." + achId, 0L));
            }
            pd.setAchievements(achMap);
        }

        // B4 — Tutorial Interativo + Welcome Kit
        pd.setTutorialStep(config.getInt(path + ".tutorial_step", 0));
        pd.setHasReceivedKit(config.getBoolean(path + ".has_received_kit", false));
        pd.setTutorialCompleted(config.getBoolean(path + ".tutorial_completed", false));

        return pd;
    }

    @Override
    public void savePlayerData(UUID uuid, PlayerData pd) {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        writePlayerData(config, uuid, pd);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar playerdata.yml: " + e.getMessage());
        }
    }

    @Override
    public void saveAllPlayerData(Map<UUID, PlayerData> dataMap) {
        File file = new File(plugin.getDataFolder(), "playerdata.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : dataMap.entrySet()) {
            writePlayerData(config, entry.getKey(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar playerdata.yml: " + e.getMessage());
        }
    }

    private void writePlayerData(YamlConfiguration config, UUID uuid, PlayerData pd) {
        String path = uuid.toString();
        config.set(path + ".blocks", pd.getClaimBlocks());
        config.set(path + ".king_rank", pd.hasKingRank());
        config.set(path + ".first_join", pd.getFirstJoin());
        config.set(path + ".total_play_time", pd.getTotalPlayTime());
        config.set(path + ".last_login", pd.getLastLogin());
        config.set(path + ".total_blocks_broken", pd.getTotalBlocksBroken());
        config.set(path + ".total_blocks_placed", pd.getTotalBlocksPlaced());
        config.set(path + ".total_kills", pd.getTotalKills());
        config.set(path + ".total_deaths", pd.getTotalDeaths());
        config.set(path + ".bosses_killed", pd.getBossesKilled());
        config.set(path + ".boss_top_damage", pd.getBossTopDamage());
        config.set(path + ".total_money_earned", pd.getTotalMoneyEarned());
        config.set(path + ".total_money_spent", pd.getTotalMoneySpent());
        config.set(path + ".active_title", pd.getActiveTitle());
        config.set(path + ".unlocked_titles", new ArrayList<>(pd.getUnlockedTitles()));
        config.set(path + ".border_sound", pd.isBorderSound());
        config.set(path + ".achievements", null);
        for (Map.Entry<String, Long> entry : pd.getAchievements().entrySet()) {
            config.set(path + ".achievements." + entry.getKey(), entry.getValue());
        }

        // B4 — Tutorial Interativo + Welcome Kit
        config.set(path + ".tutorial_step", pd.getTutorialStep());
        config.set(path + ".has_received_kit", pd.hasReceivedKit());
        config.set(path + ".tutorial_completed", pd.isTutorialCompleted());
    }

    // === Audit Log ===

    @Override
    public List<AuditManager.AuditEntry> loadAuditEntries() {
        List<AuditManager.AuditEntry> result = new ArrayList<>();
        File file = new File(plugin.getDataFolder(), "audit_log.yml");
        if (!file.exists()) return result;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Formato original do AuditManager: getMapList("entries")
        List<Map<?, ?>> list = config.getMapList("entries");
        for (Map<?, ?> map : list) {
            try {
                long timestamp = ((Number) map.get("timestamp")).longValue();
                AuditManager.AuditAction action = AuditManager.AuditAction.valueOf((String) map.get("action"));
                UUID uuid = UUID.fromString((String) map.get("uuid"));
                String name = (String) map.get("name");
                String details = (String) map.get("details");
                double value = map.containsKey("value") ? ((Number) map.get("value")).doubleValue() : 0.0;
                result.add(new AuditManager.AuditEntry(timestamp, action, uuid, name, details, value));
            } catch (Exception e) {
                // Ignora entradas corrompidas
            }
        }
        return result;
    }

    @Override
    public void saveAuditEntries(List<AuditManager.AuditEntry> entries) {
        File file = new File(plugin.getDataFolder(), "audit_log.yml");
        YamlConfiguration config = new YamlConfiguration();

        // Formato original do AuditManager: List<Map<String, Object>>
        List<Map<String, Object>> list = new ArrayList<>();
        for (AuditManager.AuditEntry entry : entries) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", entry.timestamp);
            map.put("action", entry.action.name());
            map.put("uuid", entry.playerUUID != null ? entry.playerUUID.toString() : "");
            map.put("name", entry.playerName);
            map.put("details", entry.details);
            if (entry.value != 0.0) map.put("value", entry.value);
            list.add(map);
        }
        config.set("entries", list);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar audit_log.yml: " + e.getMessage());
        }
    }

    // === Mail ===

    @Override
    public Map<UUID, List<MailManager.MailEntry>> loadAllMail() {
        Map<UUID, List<MailManager.MailEntry>> result = new ConcurrentHashMap<>();
        File file = new File(plugin.getDataFolder(), "mail.yml");
        if (!file.exists()) return result;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("mail")) return result;

        for (String targetStr : config.getConfigurationSection("mail").getKeys(false)) {
            try {
                UUID targetUUID = UUID.fromString(targetStr);
                List<MailManager.MailEntry> entries = new ArrayList<>();
                ConfigurationSection sec = config.getConfigurationSection("mail." + targetStr);
                if (sec == null) continue;
                for (String idx : sec.getKeys(false)) {
                    String p = "mail." + targetStr + "." + idx;
                    UUID senderUUID = UUID.fromString(config.getString(p + ".sender_uuid", ""));
                    String senderName = config.getString(p + ".sender_name", "");
                    String message = config.getString(p + ".message", "");
                    long timestamp = config.getLong(p + ".timestamp", 0L);
                    boolean read = config.getBoolean(p + ".read", false);
                    entries.add(new MailManager.MailEntry(senderUUID, senderName, message, timestamp, read));
                }
                result.put(targetUUID, entries);
            } catch (Exception e) {
                // Ignora entradas corrompidas
            }
        }
        return result;
    }

    @Override
    public void saveAllMail(Map<UUID, List<MailManager.MailEntry>> mailMap) {
        File file = new File(plugin.getDataFolder(), "mail.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, List<MailManager.MailEntry>> entry : mailMap.entrySet()) {
            String targetStr = entry.getKey().toString();
            int i = 0;
            for (MailManager.MailEntry mail : entry.getValue()) {
                String p = "mail." + targetStr + "." + i;
                config.set(p + ".sender_uuid", mail.senderUUID.toString());
                config.set(p + ".sender_name", mail.senderName);
                config.set(p + ".message", mail.message);
                config.set(p + ".timestamp", mail.timestamp);
                config.set(p + ".read", mail.read);
                i++;
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar mail.yml: " + e.getMessage());
        }
    }

    // === Bounties ===

    @Override
    public Map<UUID, BountyManager.Bounty> loadBounties() {
        Map<UUID, BountyManager.Bounty> result = new ConcurrentHashMap<>();
        File file = new File(plugin.getDataFolder(), "bounties.yml");
        if (!file.exists()) return result;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("bounties")) return result;

        for (String key : config.getConfigurationSection("bounties").getKeys(false)) {
            try {
                UUID targetUUID = UUID.fromString(key);
                String p = "bounties." + key;
                String targetName = config.getString(p + ".target_name", "");
                BountyManager.Bounty bounty = new BountyManager.Bounty(targetUUID, targetName);
                bounty.totalValue = config.getDouble(p + ".total_value", 0);
                bounty.lastUpdated = config.getLong(p + ".last_updated", System.currentTimeMillis());

                if (config.contains(p + ".contributors")) {
                    for (String cUuid : config.getConfigurationSection(p + ".contributors").getKeys(false)) {
                        bounty.contributors.put(UUID.fromString(cUuid),
                                config.getDouble(p + ".contributors." + cUuid, 0));
                    }
                }
                result.put(targetUUID, bounty);
            } catch (Exception e) {
                // Ignora
            }
        }
        return result;
    }

    @Override
    public void saveBounties(Map<UUID, BountyManager.Bounty> bounties) {
        File file = new File(plugin.getDataFolder(), "bounties.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, BountyManager.Bounty> entry : bounties.entrySet()) {
            String p = "bounties." + entry.getKey().toString();
            BountyManager.Bounty b = entry.getValue();
            config.set(p + ".target_name", b.targetName);
            config.set(p + ".total_value", b.totalValue);
            config.set(p + ".last_updated", b.lastUpdated);
            for (Map.Entry<UUID, Double> c : b.contributors.entrySet()) {
                config.set(p + ".contributors." + c.getKey().toString(), c.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar bounties.yml: " + e.getMessage());
        }
    }

    // === Votes ===

    @Override
    public Map<String, VoteManager.KingdomVote> loadVotes() {
        Map<String, VoteManager.KingdomVote> result = new ConcurrentHashMap<>();
        File file = new File(plugin.getDataFolder(), "votes.yml");
        if (!file.exists()) return result;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("votes")) return result;

        for (String kingdomId : config.getConfigurationSection("votes").getKeys(false)) {
            try {
                String p = "votes." + kingdomId;
                String question = config.getString(p + ".question", "");
                UUID creatorUUID = UUID.fromString(config.getString(p + ".creator_uuid", ""));
                long createdAt = config.getLong(p + ".created_at", 0L);
                long expiresAt = config.getLong(p + ".expires_at", 0L);

                VoteManager.KingdomVote vote = new VoteManager.KingdomVote(
                        kingdomId, question, creatorUUID, createdAt, expiresAt);

                if (config.contains(p + ".votes")) {
                    for (String vUuid : config.getConfigurationSection(p + ".votes").getKeys(false)) {
                        vote.votes.put(UUID.fromString(vUuid),
                                config.getBoolean(p + ".votes." + vUuid));
                    }
                }
                result.put(kingdomId, vote);
            } catch (Exception e) {
                // Ignora
            }
        }
        return result;
    }

    @Override
    public void saveVotes(Map<String, VoteManager.KingdomVote> votes) {
        File file = new File(plugin.getDataFolder(), "votes.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, VoteManager.KingdomVote> entry : votes.entrySet()) {
            String p = "votes." + entry.getKey();
            VoteManager.KingdomVote v = entry.getValue();
            config.set(p + ".question", v.question);
            config.set(p + ".creator_uuid", v.creatorUUID.toString());
            config.set(p + ".created_at", v.createdAt);
            config.set(p + ".expires_at", v.expiresAt);
            for (Map.Entry<UUID, Boolean> ve : v.votes.entrySet()) {
                config.set(p + ".votes." + ve.getKey().toString(), ve.getValue());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar votes.yml: " + e.getMessage());
        }
    }

    // === Auctions ===

    @Override
    public List<AuctionManager.Auction> loadAuctions() {
        // Delegado para o AuctionManager existente (estrutura complexa com ItemStack)
        return new ArrayList<>();
    }

    @Override
    public void saveAuctions(List<AuctionManager.Auction> auctions) {
        // Delegado para o AuctionManager existente
    }

    @Override
    public Map<UUID, List<Map<String, Object>>> loadPendingCollections() {
        return new HashMap<>();
    }

    @Override
    public void savePendingCollections(Map<UUID, List<Map<String, Object>>> pendingMap) {
        // Delegado para o AuctionManager existente
    }

    // === Price History ===

    @Override
    public Map<String, List<PriceHistoryManager.PriceSnapshot>> loadPriceHistory() {
        Map<String, List<PriceHistoryManager.PriceSnapshot>> result = new ConcurrentHashMap<>();
        File file = new File(plugin.getDataFolder(), "price_history.yml");
        if (!file.exists()) return result;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("history")) return result;

        for (String itemId : config.getConfigurationSection("history").getKeys(false)) {
            List<PriceHistoryManager.PriceSnapshot> snapshots = new ArrayList<>();
            ConfigurationSection sec = config.getConfigurationSection("history." + itemId);
            if (sec == null) continue;
            for (String idx : sec.getKeys(false)) {
                long timestamp = config.getLong("history." + itemId + "." + idx + ".timestamp", 0);
                double buyPrice = config.getDouble("history." + itemId + "." + idx + ".buy", 0);
                double sellPrice = config.getDouble("history." + itemId + "." + idx + ".sell", 0);
                snapshots.add(new PriceHistoryManager.PriceSnapshot(timestamp, buyPrice, sellPrice));
            }
            result.put(itemId, snapshots);
        }
        return result;
    }

    @Override
    public void savePriceHistory(Map<String, List<PriceHistoryManager.PriceSnapshot>> history) {
        File file = new File(plugin.getDataFolder(), "price_history.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, List<PriceHistoryManager.PriceSnapshot>> entry : history.entrySet()) {
            int i = 0;
            for (PriceHistoryManager.PriceSnapshot snap : entry.getValue()) {
                String p = "history." + entry.getKey() + "." + i;
                config.set(p + ".timestamp", snap.timestamp);
                config.set(p + ".buy", snap.buyPrice);
                config.set(p + ".sell", snap.sellPrice);
                i++;
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar price_history.yml: " + e.getMessage());
        }
    }

    // === Utilitários ===

    private UUID parseUuidSafe(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
