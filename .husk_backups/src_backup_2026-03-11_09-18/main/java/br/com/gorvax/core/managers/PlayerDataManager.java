package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager implements Listener {

    private final GorvaxCore plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private final java.util.concurrent.atomic.AtomicBoolean dirty = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    public PlayerDataManager(GorvaxCore plugin) {
        this.plugin = plugin;
        setup();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startSaveTask();
    }

    private void startSaveTask() {
        // TASK SÍNCRONA: Gera o snapshot dos dados na thread principal para evitar
        // ConcurrencyException
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (dirty.getAndSet(false)) {
                performSafeSave();
            }
        }, 600L, 600L);
    }

    /**
     * Gera um snapshot dos dados (String YAML) na Main Thread e grava no disco em
     * Async.
     * Isso previne bloqueio da Main Thread por I/O e evita Race Conditions no
     * objeto Config.
     */
    private void performSafeSave() {
        final String yamlData;
        synchronized (this) {
            yamlData = dataConfig.saveToString();
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Escrita atômica (ou quase) usando NIO
                java.nio.file.Files.write(dataFile.toPath(),
                        yamlData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao salvar playerdata.yml via Snapshot: " + e.getMessage());
            }
        });
    }

    /**
     * B7 — Retorna a FileConfiguration do playerdata.yml para leitura (ex:
     * LeaderboardManager).
     */
    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar arquivo playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public PlayerData getData(UUID uuid) {
        PlayerData cached = playerDataMap.get(uuid);
        if (cached != null)
            return cached;

        int initialBlocks = 100; // Default Starting Blocks
        String path = uuid.toString();
        if (dataConfig.contains(path)) {
            int blocks = dataConfig.getInt(path + ".blocks");

            // Migration Logic: Check for king_rank, fallback to mayor_rank
            boolean king = false;
            if (dataConfig.contains(path + ".king_rank")) {
                king = dataConfig.getBoolean(path + ".king_rank");
            } else if (dataConfig.contains(path + ".mayor_rank")) {
                king = dataConfig.getBoolean(path + ".mayor_rank", false);
            }

            PlayerData pd = new PlayerData(uuid, blocks);
            pd.setKingRank(king);

            // B3 — Carregar estatísticas expandidas
            pd.setFirstJoin(dataConfig.getLong(path + ".first_join", 0L));
            pd.setTotalPlayTime(dataConfig.getLong(path + ".total_play_time", 0L));
            pd.setLastLogin(dataConfig.getLong(path + ".last_login", 0L));
            pd.setTotalBlocksBroken(dataConfig.getInt(path + ".total_blocks_broken", 0));
            pd.setTotalBlocksPlaced(dataConfig.getInt(path + ".total_blocks_placed", 0));
            pd.setTotalKills(dataConfig.getInt(path + ".total_kills", 0));
            pd.setTotalDeaths(dataConfig.getInt(path + ".total_deaths", 0));
            pd.setBossesKilled(dataConfig.getInt(path + ".bosses_killed", 0));
            pd.setBossTopDamage(dataConfig.getInt(path + ".boss_top_damage", 0));
            pd.setTotalMoneyEarned(dataConfig.getDouble(path + ".total_money_earned", 0.0));
            pd.setTotalMoneySpent(dataConfig.getDouble(path + ".total_money_spent", 0.0));
            pd.setActiveTitle(dataConfig.getString(path + ".active_title", ""));
            List<String> titles = dataConfig.getStringList(path + ".unlocked_titles");
            pd.setUnlockedTitles(new HashSet<>(titles));

            // B9 — Som de fronteira
            pd.setBorderSound(dataConfig.getBoolean(path + ".border_sound", true));

            // B9 — Jogadores ignorados
            List<String> ignoredStrs = dataConfig.getStringList(path + ".ignored_players");
            java.util.Set<java.util.UUID> ignored = new HashSet<>();
            for (String s : ignoredStrs) {
                try {
                    ignored.add(java.util.UUID.fromString(s));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[PlayerData] UUID inválido em ignored_players de " + uuid + ": " + s);
                }
            }
            pd.setIgnoredPlayers(ignored);

            // B12 — Conquistas
            if (dataConfig.contains(path + ".achievements")) {
                java.util.Map<String, Long> achMap = new java.util.HashMap<>();
                for (String achId : dataConfig.getConfigurationSection(path + ".achievements").getKeys(false)) {
                    achMap.put(achId, dataConfig.getLong(path + ".achievements." + achId, 0L));
                }
                pd.setAchievements(achMap);
            }

            // B2 — Kill Streak
            pd.setHighestKillStreak(dataConfig.getInt(path + ".highest_kill_streak", 0));

            // B4 — Tutorial Interativo + Welcome Kit
            pd.setTutorialStep(dataConfig.getInt(path + ".tutorial_step", 0));
            pd.setHasReceivedKit(dataConfig.getBoolean(path + ".has_received_kit", false));
            pd.setTutorialCompleted(dataConfig.getBoolean(path + ".tutorial_completed", false));

            // B5 — Daily Rewards & Login Streak
            pd.setLoginStreak(dataConfig.getInt(path + ".login_streak", 0));
            pd.setLastDailyReward(dataConfig.getLong(path + ".last_daily_reward", 0L));
            pd.setDailyRewardPending(dataConfig.getBoolean(path + ".daily_reward_pending", false));

            // B12 — Crate Keys
            if (dataConfig.contains(path + ".crate_keys")) {
                var keysSection = dataConfig.getConfigurationSection(path + ".crate_keys");
                if (keysSection != null) {
                    java.util.Map<String, Integer> keys = new java.util.HashMap<>();
                    for (String keyType : keysSection.getKeys(false)) {
                        keys.put(keyType, keysSection.getInt(keyType, 0));
                    }
                    pd.setCrateKeys(keys);
                }
            }

            // B13 — Cosméticos
            List<String> cosmeticsList = dataConfig.getStringList(path + ".unlocked_cosmetics");
            pd.setUnlockedCosmetics(new HashSet<>(cosmeticsList));
            if (dataConfig.contains(path + ".active_cosmetics")) {
                var cosSection = dataConfig.getConfigurationSection(path + ".active_cosmetics");
                if (cosSection != null) {
                    java.util.Map<String, String> actives = new java.util.HashMap<>();
                    for (String cosType : cosSection.getKeys(false)) {
                        actives.put(cosType, cosSection.getString(cosType, ""));
                    }
                    pd.setActiveCosmetics(actives);
                }
            }

            // B15 — Battle Pass
            pd.setBattlePassLevel(dataConfig.getInt(path + ".battle_pass_level", 0));
            pd.setBattlePassXp(dataConfig.getInt(path + ".battle_pass_xp", 0));
            pd.setBattlePassPremium(dataConfig.getBoolean(path + ".battle_pass_premium", false));
            pd.setBattlePassSeason(dataConfig.getInt(path + ".battle_pass_season", 0));
            pd.setBattlePassLastLoginXp(dataConfig.getLong(path + ".battle_pass_last_login_xp", 0L));

            List<String> claimedFreeStrs = dataConfig.getStringList(path + ".battle_pass_claimed_free");
            java.util.Set<Integer> claimedFree = new HashSet<>();
            for (String s : claimedFreeStrs) {
                try {
                    claimedFree.add(Integer.parseInt(s));
                } catch (NumberFormatException ex) {
                }
            }
            pd.setBattlePassClaimedFree(claimedFree);

            List<String> claimedPremStrs = dataConfig.getStringList(path + ".battle_pass_claimed_premium");
            java.util.Set<Integer> claimedPrem = new HashSet<>();
            for (String s : claimedPremStrs) {
                try {
                    claimedPrem.add(Integer.parseInt(s));
                } catch (NumberFormatException ex) {
                }
            }
            pd.setBattlePassClaimedPremium(claimedPrem);

            // B16 — Quests Diárias e Semanais
            pd.setLastDailyQuestReset(dataConfig.getLong(path + ".last_daily_quest_reset", 0L));
            pd.setLastWeeklyQuestReset(dataConfig.getLong(path + ".last_weekly_quest_reset", 0L));

            if (dataConfig.contains(path + ".daily_quest_progress")) {
                var dqSection = dataConfig.getConfigurationSection(path + ".daily_quest_progress");
                if (dqSection != null) {
                    java.util.Map<String, Integer> dqMap = new java.util.HashMap<>();
                    for (String qid : dqSection.getKeys(false)) {
                        dqMap.put(qid, dqSection.getInt(qid, 0));
                    }
                    pd.setDailyQuestProgress(dqMap);
                }
            }
            List<String> claimedDailyStrs = dataConfig.getStringList(path + ".claimed_daily_quests");
            pd.setClaimedDailyQuests(new HashSet<>(claimedDailyStrs));

            if (dataConfig.contains(path + ".weekly_quest_progress")) {
                var wqSection = dataConfig.getConfigurationSection(path + ".weekly_quest_progress");
                if (wqSection != null) {
                    java.util.Map<String, Integer> wqMap = new java.util.HashMap<>();
                    for (String qid : wqSection.getKeys(false)) {
                        wqMap.put(qid, wqSection.getInt(qid, 0));
                    }
                    pd.setWeeklyQuestProgress(wqMap);
                }
            }
            List<String> claimedWeeklyStrs = dataConfig.getStringList(path + ".claimed_weekly_quests");
            pd.setClaimedWeeklyQuests(new HashSet<>(claimedWeeklyStrs));

            // B18 — Karma
            pd.setKarma(dataConfig.getInt(path + ".karma", 0));

            // Lore — Quests narrativas permanentes
            if (dataConfig.contains(path + ".lore_quest_step")) {
                var lqsSection = dataConfig.getConfigurationSection(path + ".lore_quest_step");
                if (lqsSection != null) {
                    java.util.Map<String, Integer> lqsMap = new java.util.HashMap<>();
                    for (String qid : lqsSection.getKeys(false)) {
                        lqsMap.put(qid, lqsSection.getInt(qid, 0));
                    }
                    pd.setLoreQuestStep(lqsMap);
                }
            }
            if (dataConfig.contains(path + ".lore_quest_step_progress")) {
                var lqpSection = dataConfig.getConfigurationSection(path + ".lore_quest_step_progress");
                if (lqpSection != null) {
                    java.util.Map<String, Integer> lqpMap = new java.util.HashMap<>();
                    for (String qid : lqpSection.getKeys(false)) {
                        lqpMap.put(qid, lqpSection.getInt(qid, 0));
                    }
                    pd.setLoreQuestStepProgress(lqpMap);
                }
            }
            List<String> completedLoreStrs = dataConfig.getStringList(path + ".completed_lore_quests");
            pd.setCompletedLoreQuests(new HashSet<>(completedLoreStrs));

            // B28 — Códex de Gorvax
            List<String> codexStrs = dataConfig.getStringList(path + ".unlocked_codex");
            pd.setUnlockedCodex(new HashSet<>(codexStrs));

            playerDataMap.put(uuid, pd);
            return pd;
        } else {
            PlayerData pd = new PlayerData(uuid, initialBlocks);
            playerDataMap.put(uuid, pd);
            return pd;
        }
    }

    public void saveData(UUID uuid) {
        synchronized (this) {
            setPlayerData(uuid);
        }
        dirty.set(true);
    }

    private synchronized void setPlayerData(UUID uuid) {
        PlayerData pd = playerDataMap.get(uuid);
        if (pd == null)
            return;

        String path = uuid.toString();
        dataConfig.set(path + ".blocks", pd.getClaimBlocks());
        dataConfig.set(path + ".king_rank", pd.hasKingRank());

        if (dataConfig.contains(path + ".mayor_rank")) {
            dataConfig.set(path + ".mayor_rank", null);
        }

        // B3 — Salvar estatísticas expandidas
        dataConfig.set(path + ".first_join", pd.getFirstJoin());
        dataConfig.set(path + ".total_play_time", pd.getTotalPlayTime());
        dataConfig.set(path + ".last_login", pd.getLastLogin());
        dataConfig.set(path + ".total_blocks_broken", pd.getTotalBlocksBroken());
        dataConfig.set(path + ".total_blocks_placed", pd.getTotalBlocksPlaced());
        dataConfig.set(path + ".total_kills", pd.getTotalKills());
        dataConfig.set(path + ".total_deaths", pd.getTotalDeaths());
        dataConfig.set(path + ".bosses_killed", pd.getBossesKilled());
        dataConfig.set(path + ".boss_top_damage", pd.getBossTopDamage());
        dataConfig.set(path + ".total_money_earned", pd.getTotalMoneyEarned());
        dataConfig.set(path + ".total_money_spent", pd.getTotalMoneySpent());
        dataConfig.set(path + ".active_title", pd.getActiveTitle());
        dataConfig.set(path + ".unlocked_titles", new java.util.ArrayList<>(pd.getUnlockedTitles()));

        // B9 — Som de fronteira
        dataConfig.set(path + ".border_sound", pd.isBorderSound());

        // B9 — Jogadores ignorados
        java.util.List<String> ignoredStrs = new java.util.ArrayList<>();
        for (java.util.UUID ignoredUuid : pd.getIgnoredPlayers()) {
            ignoredStrs.add(ignoredUuid.toString());
        }
        dataConfig.set(path + ".ignored_players", ignoredStrs);

        // B12 — Conquistas
        dataConfig.set(path + ".achievements", null); // Limpar antes de salvar
        for (java.util.Map.Entry<String, Long> entry : pd.getAchievements().entrySet()) {
            dataConfig.set(path + ".achievements." + entry.getKey(), entry.getValue());
        }

        // B2 — Kill Streak
        dataConfig.set(path + ".highest_kill_streak", pd.getHighestKillStreak());

        // B4 — Tutorial Interativo + Welcome Kit
        dataConfig.set(path + ".tutorial_step", pd.getTutorialStep());
        dataConfig.set(path + ".has_received_kit", pd.hasReceivedKit());
        dataConfig.set(path + ".tutorial_completed", pd.isTutorialCompleted());

        // B5 — Daily Rewards & Login Streak
        dataConfig.set(path + ".login_streak", pd.getLoginStreak());
        dataConfig.set(path + ".last_daily_reward", pd.getLastDailyReward());
        dataConfig.set(path + ".daily_reward_pending", pd.isDailyRewardPending());

        // B12 — Crate Keys
        dataConfig.set(path + ".crate_keys", null); // Limpar antes de salvar
        for (java.util.Map.Entry<String, Integer> entry : pd.getCrateKeys().entrySet()) {
            dataConfig.set(path + ".crate_keys." + entry.getKey(), entry.getValue());
        }

        // B13 — Cosméticos
        dataConfig.set(path + ".unlocked_cosmetics", new java.util.ArrayList<>(pd.getUnlockedCosmetics()));
        dataConfig.set(path + ".active_cosmetics", null); // Limpar antes de salvar
        for (java.util.Map.Entry<String, String> entry : pd.getActiveCosmetics().entrySet()) {
            dataConfig.set(path + ".active_cosmetics." + entry.getKey(), entry.getValue());
        }

        // B15 — Battle Pass
        dataConfig.set(path + ".battle_pass_level", pd.getBattlePassLevel());
        dataConfig.set(path + ".battle_pass_xp", pd.getBattlePassXp());
        dataConfig.set(path + ".battle_pass_premium", pd.isBattlePassPremium());
        dataConfig.set(path + ".battle_pass_season", pd.getBattlePassSeason());
        dataConfig.set(path + ".battle_pass_last_login_xp", pd.getBattlePassLastLoginXp());

        java.util.List<String> claimedFreeStrs = new java.util.ArrayList<>();
        for (int lvl : pd.getBattlePassClaimedFree()) {
            claimedFreeStrs.add(String.valueOf(lvl));
        }
        dataConfig.set(path + ".battle_pass_claimed_free", claimedFreeStrs);

        java.util.List<String> claimedPremStrs = new java.util.ArrayList<>();
        for (int lvl : pd.getBattlePassClaimedPremium()) {
            claimedPremStrs.add(String.valueOf(lvl));
        }
        dataConfig.set(path + ".battle_pass_claimed_premium", claimedPremStrs);

        // B16 — Quests Diárias e Semanais
        dataConfig.set(path + ".last_daily_quest_reset", pd.getLastDailyQuestReset());
        dataConfig.set(path + ".last_weekly_quest_reset", pd.getLastWeeklyQuestReset());

        dataConfig.set(path + ".daily_quest_progress", null); // Limpar antes de salvar
        for (java.util.Map.Entry<String, Integer> entry : pd.getDailyQuestProgress().entrySet()) {
            dataConfig.set(path + ".daily_quest_progress." + entry.getKey(), entry.getValue());
        }
        dataConfig.set(path + ".claimed_daily_quests", new java.util.ArrayList<>(pd.getClaimedDailyQuests()));

        dataConfig.set(path + ".weekly_quest_progress", null); // Limpar antes de salvar
        for (java.util.Map.Entry<String, Integer> entry : pd.getWeeklyQuestProgress().entrySet()) {
            dataConfig.set(path + ".weekly_quest_progress." + entry.getKey(), entry.getValue());
        }
        dataConfig.set(path + ".claimed_weekly_quests", new java.util.ArrayList<>(pd.getClaimedWeeklyQuests()));

        // B18 — Karma
        dataConfig.set(path + ".karma", pd.getKarma());

        // Lore — Quests narrativas permanentes
        dataConfig.set(path + ".lore_quest_step", null);
        for (java.util.Map.Entry<String, Integer> entry : pd.getLoreQuestStep().entrySet()) {
            dataConfig.set(path + ".lore_quest_step." + entry.getKey(), entry.getValue());
        }
        dataConfig.set(path + ".lore_quest_step_progress", null);
        for (java.util.Map.Entry<String, Integer> entry : pd.getLoreQuestStepProgress().entrySet()) {
            dataConfig.set(path + ".lore_quest_step_progress." + entry.getKey(), entry.getValue());
        }
        dataConfig.set(path + ".completed_lore_quests", new java.util.ArrayList<>(pd.getCompletedLoreQuests()));

        // B28 — Códex de Gorvax
        dataConfig.set(path + ".unlocked_codex", new java.util.ArrayList<>(pd.getUnlockedCodex()));
    }

    private void saveSync() {
        // Método mantido apenas para fallback ou uso no onDisable (que espera ser
        // bloqueante)
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar playerdata.yml: " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : playerDataMap.keySet()) {
            setPlayerData(uuid);
        }
        saveSync();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        unloadData(e.getPlayer().getUniqueId());
    }

    /**
     * Limpa o cache de um jogador que saiu do servidor para evitar vazamento de
     * memória.
     */
    public synchronized void unloadData(UUID uuid) {
        if (playerDataMap.containsKey(uuid)) {
            // No quit, apenas movemos para o config e marcamos como dirty.
            // O saveTask cuidará de persistir no disco de forma assíncrona.
            setPlayerData(uuid);
            dirty.set(true);
            playerDataMap.remove(uuid);
        }
    }
}
