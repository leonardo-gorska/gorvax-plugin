package br.com.gorvax.core.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int claimBlocks;
    private boolean hasKingRank; // Persistence for king rank purchase (formerly mayor)

    // B3 — Estatísticas expandidas
    private long firstJoin;
    private long totalPlayTime; // Tempo total jogado em milissegundos
    private long lastLogin;
    private int totalBlocksBroken;
    private int totalBlocksPlaced;
    private int totalKills;
    private int totalDeaths;
    private int bossesKilled;
    private int bossTopDamage;
    private double totalMoneyEarned;
    private double totalMoneySpent;
    private String activeTitle;
    private final Set<String> unlockedTitles = new HashSet<>();
    private final Map<String, Long> achievements = new HashMap<>(); // B12 — id → timestamp de desbloqueio
    private boolean borderSound = true; // B9 — Som ao cruzar fronteira (ligado por padrão)
    private final Set<UUID> ignoredPlayers = new HashSet<>(); // B9 — Jogadores ignorados

    // B13 — Outpost creation flow (transient, not saved)
    private boolean nextClaimIsOutpost = false;
    private String outpostParentKingdomId = null;

    // B2 — Kill Streak máxima histórica
    private int highestKillStreak;

    // B3 — Estatísticas de Duelos
    private int duelWins;
    private int duelLosses;
    private double duelMoneyWon;

    // B4 — Tutorial Interativo + Welcome Kit
    private int tutorialStep; // 0=não iniciado, 1-6=em progresso, 7=completo
    private boolean hasReceivedKit; // Flag para evitar kit duplicado
    private boolean tutorialCompleted; // Se finalizou ou pulou o tutorial

    // B5 — Daily Rewards & Login Streak
    private int loginStreak; // Sequência atual (0-7)
    private long lastDailyReward; // Timestamp do último resgate
    private boolean dailyRewardPending; // Se o jogador tem reward pendente

    // B12 — Crate Keys (tipo → quantidade)
    private final Map<String, Integer> crateKeys = new HashMap<>();

    // B13 — Cosméticos desbloqueados e ativos
    private final Set<String> unlockedCosmetics = new HashSet<>();
    private final Map<String, String> activeCosmetics = new HashMap<>(); // tipo → id

    // B15 — Battle Pass Sazonal
    private int battlePassLevel;
    private int battlePassXp;
    private boolean battlePassPremium;
    private final Set<Integer> battlePassClaimedFree = new HashSet<>();
    private final Set<Integer> battlePassClaimedPremium = new HashSet<>();
    private int battlePassSeason;
    private long battlePassLastLoginXp;

    // B16 — Quests Diárias e Semanais
    private final Map<String, Integer> dailyQuestProgress = new HashMap<>();
    private final Set<String> claimedDailyQuests = new HashSet<>();
    private long lastDailyQuestReset;
    private final Map<String, Integer> weeklyQuestProgress = new HashMap<>();
    private final Set<String> claimedWeeklyQuests = new HashSet<>();
    private long lastWeeklyQuestReset;

    // B18 — Sistema de Reputação / Karma
    private int karma;

    // Lore — Quests narrativas permanentes (multi-step)
    private final Map<String, Integer> loreQuestStep = new HashMap<>(); // questId → step index (0-based)
    private final Map<String, Integer> loreQuestStepProgress = new HashMap<>(); // questId → progress in current step
    private final Set<String> completedLoreQuests = new HashSet<>(); // questIds finalizadas

    // B28 — Códex de Gorvax (Enciclopédia)
    private final Set<String> unlockedCodex = new HashSet<>(); // "categoria.entryId"

    public PlayerData(UUID uuid, int initialBlocks) {
        this.uuid = uuid;
        this.claimBlocks = initialBlocks;
        this.hasKingRank = false;
        this.firstJoin = 0L;
        this.totalPlayTime = 0L;
        this.lastLogin = 0L;
        this.activeTitle = "";
    }

    public UUID getUuid() {
        return uuid;
    }

    public synchronized int getClaimBlocks() {
        return claimBlocks;
    }

    public synchronized void addClaimBlocks(int amount) {
        this.claimBlocks += amount;
    }

    public synchronized boolean removeClaimBlocks(int amount) {
        if (claimBlocks >= amount) {
            this.claimBlocks -= amount;
            return true;
        }
        return false;
    }

    public boolean hasKingRank() {
        return hasKingRank;
    }

    public void setKingRank(boolean hasKingRank) {
        this.hasKingRank = hasKingRank;
    }

    // --- B3: Getters e Setters de Estatísticas ---

    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public synchronized long getTotalPlayTime() {
        return totalPlayTime;
    }

    public synchronized void addPlayTime(long millis) {
        this.totalPlayTime += millis;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public synchronized int getTotalBlocksBroken() {
        return totalBlocksBroken;
    }

    public synchronized void incrementBlocksBroken() {
        this.totalBlocksBroken++;
    }

    public synchronized int getTotalBlocksPlaced() {
        return totalBlocksPlaced;
    }

    public synchronized void incrementBlocksPlaced() {
        this.totalBlocksPlaced++;
    }

    public synchronized int getTotalKills() {
        return totalKills;
    }

    public synchronized void incrementKills() {
        this.totalKills++;
    }

    public synchronized int getTotalDeaths() {
        return totalDeaths;
    }

    public synchronized void incrementDeaths() {
        this.totalDeaths++;
    }

    public synchronized int getBossesKilled() {
        return bossesKilled;
    }

    public synchronized void incrementBossesKilled() {
        this.bossesKilled++;
    }

    public synchronized int getBossTopDamage() {
        return bossTopDamage;
    }

    public synchronized void incrementBossTopDamage() {
        this.bossTopDamage++;
    }

    public synchronized double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    public synchronized void addMoneyEarned(double amount) {
        this.totalMoneyEarned += amount;
    }

    public synchronized double getTotalMoneySpent() {
        return totalMoneySpent;
    }

    public synchronized void addMoneySpent(double amount) {
        this.totalMoneySpent += amount;
    }

    public String getActiveTitle() {
        return activeTitle;
    }

    public void setActiveTitle(String activeTitle) {
        this.activeTitle = activeTitle != null ? activeTitle : "";
    }

    public Set<String> getUnlockedTitles() {
        return Collections.unmodifiableSet(unlockedTitles);
    }

    public void addUnlockedTitle(String title) {
        this.unlockedTitles.add(title);
    }

    // B9 — Som de fronteira
    public boolean isBorderSound() {
        return borderSound;
    }

    public void setBorderSound(boolean borderSound) {
        this.borderSound = borderSound;
    }

    public void setUnlockedTitles(Set<String> titles) {
        this.unlockedTitles.clear();
        if (titles != null) {
            this.unlockedTitles.addAll(titles);
        }
    }

    // Setters diretos para carregamento do YAML
    public void setTotalBlocksBroken(int val) {
        this.totalBlocksBroken = val;
    }

    public void setTotalBlocksPlaced(int val) {
        this.totalBlocksPlaced = val;
    }

    public void setTotalKills(int val) {
        this.totalKills = val;
    }

    public void setTotalDeaths(int val) {
        this.totalDeaths = val;
    }

    public void setBossesKilled(int val) {
        this.bossesKilled = val;
    }

    public void setBossTopDamage(int val) {
        this.bossTopDamage = val;
    }

    public void setTotalMoneyEarned(double val) {
        this.totalMoneyEarned = val;
    }

    public void setTotalMoneySpent(double val) {
        this.totalMoneySpent = val;
    }

    public void setTotalPlayTime(long val) {
        this.totalPlayTime = val;
    }

    // --- B12: Conquistas ---
    public Map<String, Long> getAchievements() {
        return Collections.unmodifiableMap(achievements);
    }

    public boolean hasAchievement(String id) {
        return achievements.containsKey(id);
    }

    public void unlockAchievement(String id) {
        if (!achievements.containsKey(id)) {
            achievements.put(id, System.currentTimeMillis());
        }
    }

    public void setAchievements(Map<String, Long> map) {
        this.achievements.clear();
        if (map != null) {
            this.achievements.putAll(map);
        }
    }

    // --- B13: Outpost creation flow (transient) ---
    public boolean isNextClaimIsOutpost() {
        return nextClaimIsOutpost;
    }

    public void setNextClaimIsOutpost(boolean val) {
        this.nextClaimIsOutpost = val;
    }

    public String getOutpostParentKingdomId() {
        return outpostParentKingdomId;
    }

    public void setOutpostParentKingdomId(String id) {
        this.outpostParentKingdomId = id;
    }

    // --- B2: Kill Streak ---
    public int getHighestKillStreak() {
        return highestKillStreak;
    }

    public void setHighestKillStreak(int highestKillStreak) {
        this.highestKillStreak = highestKillStreak;
    }

    // --- B3: Duelos ---
    public synchronized int getDuelWins() {
        return duelWins;
    }

    public synchronized void incrementDuelWins() {
        this.duelWins++;
    }

    public void setDuelWins(int val) {
        this.duelWins = val;
    }

    public synchronized int getDuelLosses() {
        return duelLosses;
    }

    public synchronized void incrementDuelLosses() {
        this.duelLosses++;
    }

    public void setDuelLosses(int val) {
        this.duelLosses = val;
    }

    public synchronized double getDuelMoneyWon() {
        return duelMoneyWon;
    }

    public synchronized void addDuelMoneyWon(double amount) {
        this.duelMoneyWon += amount;
    }

    public void setDuelMoneyWon(double val) {
        this.duelMoneyWon = val;
    }

    // --- B4: Tutorial Interativo + Welcome Kit ---
    public int getTutorialStep() {
        return tutorialStep;
    }

    public void setTutorialStep(int step) {
        this.tutorialStep = step;
    }

    public boolean hasReceivedKit() {
        return hasReceivedKit;
    }

    public void setHasReceivedKit(boolean hasReceivedKit) {
        this.hasReceivedKit = hasReceivedKit;
    }

    public boolean isTutorialCompleted() {
        return tutorialCompleted;
    }

    public void setTutorialCompleted(boolean tutorialCompleted) {
        this.tutorialCompleted = tutorialCompleted;
    }

    // --- B5: Daily Rewards & Login Streak ---
    public int getLoginStreak() {
        return loginStreak;
    }

    public void setLoginStreak(int loginStreak) {
        this.loginStreak = loginStreak;
    }

    public long getLastDailyReward() {
        return lastDailyReward;
    }

    public void setLastDailyReward(long lastDailyReward) {
        this.lastDailyReward = lastDailyReward;
    }

    public boolean isDailyRewardPending() {
        return dailyRewardPending;
    }

    public void setDailyRewardPending(boolean dailyRewardPending) {
        this.dailyRewardPending = dailyRewardPending;
    }
    // ===== B9 — Ignored Players =====

    public Set<UUID> getIgnoredPlayers() {
        return Collections.unmodifiableSet(ignoredPlayers);
    }

    public void setIgnoredPlayers(Set<UUID> players) {
        ignoredPlayers.clear();
        if (players != null) {
            ignoredPlayers.addAll(players);
        }
    }

    /**
     * Adiciona um jogador à lista de ignorados.
     * 
     * @return true se adicionou, false se já estava ignorado ou limite atingido.
     */
    public boolean addIgnoredPlayer(UUID target) {
        if (ignoredPlayers.size() >= 50)
            return false; // Limite de 50 jogadores ignorados
        return ignoredPlayers.add(target);
    }

    /**
     * Remove um jogador da lista de ignorados.
     * 
     * @return true se removeu, false se não estava ignorado.
     */
    public boolean removeIgnoredPlayer(UUID target) {
        return ignoredPlayers.remove(target);
    }

    // ===== B12 — Crate Keys =====

    public Map<String, Integer> getCrateKeys() {
        return crateKeys;
    }

    public void setCrateKeys(Map<String, Integer> keys) {
        crateKeys.clear();
        if (keys != null) {
            crateKeys.putAll(keys);
        }
    }

    public void addCrateKey(String type, int amount) {
        crateKeys.merge(type, amount, Integer::sum);
    }

    /**
     * Remove 1 key de um tipo. Retorna true se tinha key e foi removida.
     */
    public boolean removeCrateKey(String type) {
        int current = crateKeys.getOrDefault(type, 0);
        if (current <= 0)
            return false;
        if (current == 1) {
            crateKeys.remove(type);
        } else {
            crateKeys.put(type, current - 1);
        }
        return true;
    }

    // ===== B13 — Cosméticos =====

    public Set<String> getUnlockedCosmetics() {
        return unlockedCosmetics;
    }

    public void setUnlockedCosmetics(Set<String> cosmetics) {
        unlockedCosmetics.clear();
        if (cosmetics != null) {
            unlockedCosmetics.addAll(cosmetics);
        }
    }

    public void unlockCosmetic(String id) {
        unlockedCosmetics.add(id);
    }

    public boolean hasCosmetic(String id) {
        return unlockedCosmetics.contains(id);
    }

    public Map<String, String> getActiveCosmetics() {
        return activeCosmetics;
    }

    public void setActiveCosmetics(Map<String, String> cosmetics) {
        activeCosmetics.clear();
        if (cosmetics != null) {
            activeCosmetics.putAll(cosmetics);
        }
    }

    public void setActiveCosmetic(String type, String id) {
        activeCosmetics.put(type, id);
    }

    public void clearActiveCosmetic(String type) {
        activeCosmetics.remove(type);
    }

    // ===== B15 — Battle Pass =====

    public int getBattlePassLevel() {
        return battlePassLevel;
    }

    public void setBattlePassLevel(int level) {
        this.battlePassLevel = level;
    }

    public int getBattlePassXp() {
        return battlePassXp;
    }

    public void setBattlePassXp(int xp) {
        this.battlePassXp = xp;
    }

    public boolean isBattlePassPremium() {
        return battlePassPremium;
    }

    public void setBattlePassPremium(boolean premium) {
        this.battlePassPremium = premium;
    }

    public Set<Integer> getBattlePassClaimedFree() {
        return battlePassClaimedFree;
    }

    public void setBattlePassClaimedFree(Set<Integer> claimed) {
        battlePassClaimedFree.clear();
        if (claimed != null)
            battlePassClaimedFree.addAll(claimed);
    }

    public Set<Integer> getBattlePassClaimedPremium() {
        return battlePassClaimedPremium;
    }

    public void setBattlePassClaimedPremium(Set<Integer> claimed) {
        battlePassClaimedPremium.clear();
        if (claimed != null)
            battlePassClaimedPremium.addAll(claimed);
    }

    public int getBattlePassSeason() {
        return battlePassSeason;
    }

    public void setBattlePassSeason(int season) {
        this.battlePassSeason = season;
    }

    public long getBattlePassLastLoginXp() {
        return battlePassLastLoginXp;
    }

    public void setBattlePassLastLoginXp(long timestamp) {
        this.battlePassLastLoginXp = timestamp;
    }

    // ===== B16 — Quests Diárias e Semanais =====

    public Map<String, Integer> getDailyQuestProgress() {
        return dailyQuestProgress;
    }

    public void setDailyQuestProgress(Map<String, Integer> progress) {
        dailyQuestProgress.clear();
        if (progress != null)
            dailyQuestProgress.putAll(progress);
    }

    public Set<String> getClaimedDailyQuests() {
        return claimedDailyQuests;
    }

    public void setClaimedDailyQuests(Set<String> claimed) {
        claimedDailyQuests.clear();
        if (claimed != null)
            claimedDailyQuests.addAll(claimed);
    }

    public long getLastDailyQuestReset() {
        return lastDailyQuestReset;
    }

    public void setLastDailyQuestReset(long timestamp) {
        this.lastDailyQuestReset = timestamp;
    }

    public Map<String, Integer> getWeeklyQuestProgress() {
        return weeklyQuestProgress;
    }

    public void setWeeklyQuestProgress(Map<String, Integer> progress) {
        weeklyQuestProgress.clear();
        if (progress != null)
            weeklyQuestProgress.putAll(progress);
    }

    public Set<String> getClaimedWeeklyQuests() {
        return claimedWeeklyQuests;
    }

    public void setClaimedWeeklyQuests(Set<String> claimed) {
        claimedWeeklyQuests.clear();
        if (claimed != null)
            claimedWeeklyQuests.addAll(claimed);
    }

    public long getLastWeeklyQuestReset() {
        return lastWeeklyQuestReset;
    }

    public void setLastWeeklyQuestReset(long timestamp) {
        this.lastWeeklyQuestReset = timestamp;
    }

    // ===== B18 — Reputação / Karma =====

    public synchronized int getKarma() {
        return karma;
    }

    public synchronized void setKarma(int karma) {
        this.karma = Math.max(-200, Math.min(200, karma));
    }

    /**
     * Adiciona (ou remove se negativo) karma, respeitando os limites -200..200.
     */
    public synchronized void addKarma(int amount) {
        this.karma = Math.max(-200, Math.min(200, this.karma + amount));
    }

    // ===== Lore — Quests Narrativas Permanentes =====

    public Map<String, Integer> getLoreQuestStep() {
        return loreQuestStep;
    }

    public void setLoreQuestStep(Map<String, Integer> map) {
        loreQuestStep.clear();
        if (map != null)
            loreQuestStep.putAll(map);
    }

    public Map<String, Integer> getLoreQuestStepProgress() {
        return loreQuestStepProgress;
    }

    public void setLoreQuestStepProgress(Map<String, Integer> map) {
        loreQuestStepProgress.clear();
        if (map != null)
            loreQuestStepProgress.putAll(map);
    }

    public Set<String> getCompletedLoreQuests() {
        return completedLoreQuests;
    }

    public void setCompletedLoreQuests(Set<String> set) {
        completedLoreQuests.clear();
        if (set != null)
            completedLoreQuests.addAll(set);
    }

    public boolean isLoreQuestCompleted(String questId) {
        return completedLoreQuests.contains(questId);
    }

    // ===== B28 — Códex de Gorvax =====

    public Set<String> getUnlockedCodex() {
        return unlockedCodex;
    }

    public void setUnlockedCodex(Set<String> set) {
        unlockedCodex.clear();
        if (set != null)
            unlockedCodex.addAll(set);
    }

    public boolean hasCodexEntry(String key) {
        return unlockedCodex.contains(key);
    }

    public void unlockCodexEntry(String key) {
        unlockedCodex.add(key);
    }
}
