package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B15 — Gerencia o sistema de Battle Pass Sazonal.
 * Temporada de 30 dias com 30 níveis, track Free + Premium.
 */
public class BattlePassManager {

    private final GorvaxCore plugin;
    private File configFile;
    private FileConfiguration config;

    // Configurações carregadas
    private boolean enabled;
    private int seasonNumber;
    private String seasonName;
    private int durationDays;
    private LocalDate startDate;

    // XP por ação
    private final Map<String, Integer> xpSources = new HashMap<>();

    // XP por nível
    private int xpBase;
    private int xpIncrement;

    // Rewards por nível: nível → track → lista de rewards
    private final Map<Integer, List<RewardEntry>> freeRewards = new HashMap<>();
    private final Map<Integer, List<RewardEntry>> premiumRewards = new HashMap<>();

    private static final int MAX_LEVEL = 30;

    public BattlePassManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Carrega ou recarrega a configuração do Battle Pass.
     */
    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "battlepass.yml");
        if (!configFile.exists()) {
            plugin.saveResource("battlepass.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("enabled", true);
        seasonNumber = config.getInt("season.number", 1);
        seasonName = config.getString("season.name", "§6⚔ Temporada 1");
        durationDays = config.getInt("season.duration_days", 30);

        // Data de início
        String dateStr = config.getString("season.start_date", "");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                startDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e) {
                plugin.getLogger().warning("[BattlePass] Data de início inválida: " + dateStr + ". Usando hoje.");
                startDate = LocalDate.now();
            }
        } else {
            startDate = LocalDate.now();
        }

        // XP por ação
        xpSources.clear();
        ConfigurationSection xpSection = config.getConfigurationSection("xp_sources");
        if (xpSection != null) {
            for (String key : xpSection.getKeys(false)) {
                xpSources.put(key, xpSection.getInt(key, 0));
            }
        }

        // XP por nível
        xpBase = config.getInt("xp_per_level.base", 100);
        xpIncrement = config.getInt("xp_per_level.increment", 20);

        // Rewards
        freeRewards.clear();
        premiumRewards.clear();
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String levelStr : rewardsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelStr);
                    List<RewardEntry> freeList = parseRewardList(rewardsSection.getMapList(levelStr + ".free"));
                    List<RewardEntry> premList = parseRewardList(rewardsSection.getMapList(levelStr + ".premium"));
                    freeRewards.put(level, freeList);
                    premiumRewards.put(level, premList);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[BattlePass] Nível inválido no rewards: " + levelStr);
                }
            }
        }

        plugin.getLogger().info("[BattlePass] Configuração carregada: Temporada " + seasonNumber
                + " (" + seasonName + ") — " + freeRewards.size() + " níveis configurados.");
    }

    /**
     * Recarrega configuração do Battle Pass.
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Parseia uma lista de mapas do YAML em RewardEntry.
     */
    @SuppressWarnings("unchecked")
    private List<RewardEntry> parseRewardList(List<?> mapList) {
        List<RewardEntry> list = new ArrayList<>();
        if (mapList == null)
            return list;

        for (Object obj : mapList) {
            if (obj instanceof Map<?, ?> rawMap) {
                Map<String, Object> map = (Map<String, Object>) rawMap;
                String type = String.valueOf(map.getOrDefault("type", ""));
                int amount = 0;
                if (map.containsKey("amount")) {
                    amount = ((Number) map.get("amount")).intValue();
                }
                String extra = "";
                if (map.containsKey("key_type"))
                    extra = String.valueOf(map.get("key_type"));
                else if (map.containsKey("title_id"))
                    extra = String.valueOf(map.get("title_id"));
                else if (map.containsKey("cosmetic_id"))
                    extra = String.valueOf(map.get("cosmetic_id"));
                else if (map.containsKey("item_id"))
                    extra = String.valueOf(map.get("item_id"));

                list.add(new RewardEntry(type, amount, extra));
            }
        }
        return list;
    }

    // ===== OPERAÇÕES PRINCIPAIS =====

    /**
     * Verifica se o Battle Pass está habilitado e a temporada está ativa.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Verifica se a temporada atual está ativa (dentro do período).
     */
    public boolean isSeasonActive() {
        if (!enabled)
            return false;
        LocalDate now = LocalDate.now();
        LocalDate endDate = startDate.plusDays(durationDays);
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * Retorna os dias restantes da temporada.
     */
    public int getDaysRemaining() {
        LocalDate endDate = startDate.plusDays(durationDays);
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return Math.max(0, (int) days);
    }

    /**
     * Retorna o XP necessário para um determinado nível.
     */
    public int getXpForLevel(int level) {
        return xpBase + (level - 1) * xpIncrement;
    }

    /**
     * Retorna o XP para uma determinada fonte de ação.
     */
    public int getXpForSource(String source) {
        return xpSources.getOrDefault(source, 0);
    }

    /**
     * Adiciona XP ao jogador e verifica level-up.
     */
    public void addXp(Player player, int amount, String source) {
        if (!enabled || !isSeasonActive())
            return;
        if (amount <= 0)
            return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        // Verificar se jogador está na temporada atual
        checkSeasonReset(pd);

        int currentLevel = pd.getBattlePassLevel();
        if (currentLevel >= MAX_LEVEL)
            return; // Já no nível máximo

        int currentXp = pd.getBattlePassXp();
        currentXp += amount;

        // Verificar level-up
        int xpNeeded = getXpForLevel(currentLevel + 1);
        boolean leveledUp = false;

        while (currentXp >= xpNeeded && currentLevel < MAX_LEVEL) {
            currentXp -= xpNeeded;
            currentLevel++;
            leveledUp = true;
            xpNeeded = getXpForLevel(currentLevel + 1);
        }

        pd.setBattlePassXp(currentXp);
        pd.setBattlePassLevel(currentLevel);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());

        if (leveledUp) {
            onLevelUp(player, currentLevel);
        }

        // Notificação de XP ganho (Action Bar — sutil, sem poluir chat)
        var msg = plugin.getMessageManager();
        msg.sendActionBar(player, "battlepass.xp_gained", amount, source);
    }

    /**
     * Verifica se o jogador precisa ser resetado para uma nova temporada.
     */
    private void checkSeasonReset(PlayerData pd) {
        if (pd.getBattlePassSeason() != seasonNumber) {
            pd.setBattlePassLevel(0);
            pd.setBattlePassXp(0);
            pd.setBattlePassPremium(false);
            pd.getBattlePassClaimedFree().clear();
            pd.getBattlePassClaimedPremium().clear();
            pd.setBattlePassSeason(seasonNumber);
        }
    }

    /**
     * Callback ao subir de nível.
     */
    private void onLevelUp(Player player, int newLevel) {
        var msg = plugin.getMessageManager();

        // Título na tela
        Component title = LegacyComponentSerializer.legacySection()
                .deserialize(msg.get("battlepass.levelup_title", newLevel));
        Component subtitle = LegacyComponentSerializer.legacySection()
                .deserialize(msg.get("battlepass.levelup_subtitle"));
        player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));

        // Som
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * Tenta resgatar a recompensa de um nível para o jogador.
     * 
     * @param premium true para track premium, false para free
     * @return true se resgatou com sucesso
     */
    public boolean claimReward(Player player, int level, boolean premium) {
        if (!enabled)
            return false;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        checkSeasonReset(pd);

        var msg = plugin.getMessageManager();

        // Verificar nível
        if (level < 1 || level > MAX_LEVEL) {
            player.sendMessage(msg.get("battlepass.invalid_level"));
            return false;
        }

        if (pd.getBattlePassLevel() < level) {
            player.sendMessage(msg.get("battlepass.level_too_low", level));
            return false;
        }

        if (premium && !pd.isBattlePassPremium()) {
            player.sendMessage(msg.get("battlepass.no_premium"));
            return false;
        }

        // Verificar se já resgatou
        if (premium) {
            if (pd.getBattlePassClaimedPremium().contains(level)) {
                player.sendMessage(msg.get("battlepass.already_claimed"));
                return false;
            }
        } else {
            if (pd.getBattlePassClaimedFree().contains(level)) {
                player.sendMessage(msg.get("battlepass.already_claimed"));
                return false;
            }
        }

        // Buscar rewards
        Map<Integer, List<RewardEntry>> rewardMap = premium ? premiumRewards : freeRewards;
        List<RewardEntry> rewards = rewardMap.get(level);
        if (rewards == null || rewards.isEmpty()) {
            player.sendMessage(msg.get("battlepass.no_reward"));
            return false;
        }

        // Aplicar rewards
        for (RewardEntry reward : rewards) {
            applyReward(player, pd, reward);
        }

        // Marcar como resgatado
        if (premium) {
            pd.getBattlePassClaimedPremium().add(level);
        } else {
            pd.getBattlePassClaimedFree().add(level);
        }

        plugin.getPlayerDataManager().saveData(player.getUniqueId());

        // Feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage(msg.get("battlepass.reward_claimed", level, premium ? "§dPremium" : "§aFree"));

        return true;
    }

    /**
     * Aplica uma recompensa individual ao jogador.
     */
    private void applyReward(Player player, PlayerData pd, RewardEntry reward) {
        switch (reward.type()) {
            case "money" -> {
                GorvaxCore.getEconomy().depositPlayer(player, reward.amount());
                pd.addMoneyEarned(reward.amount());
            }
            case "claim_blocks" -> {
                pd.addClaimBlocks(reward.amount());
            }
            case "crate_key" -> {
                pd.addCrateKey(reward.extra(), reward.amount());
            }
            case "title" -> {
                pd.addUnlockedTitle(reward.extra());
                player.sendMessage("§b[Gorvax] §eTítulo desbloqueado: §f" + reward.extra());
            }
            case "cosmetic" -> {
                pd.unlockCosmetic(reward.extra());
                player.sendMessage("§b[Gorvax] §eCosmético desbloqueado: §f" + reward.extra());
            }
            case "custom_item" -> {
                if (plugin.getCustomItemManager() != null) {
                    org.bukkit.inventory.ItemStack customItem = plugin.getCustomItemManager().getItem(reward.extra());
                    if (customItem != null) {
                        var overflow = player.getInventory().addItem(customItem);
                        for (var drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
            }
        }
    }

    /**
     * Ativa premium para um jogador.
     */
    public void activatePremium(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        checkSeasonReset(pd);
        pd.setBattlePassPremium(true);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    /**
     * Reseta progresso de um jogador.
     */
    public void resetPlayer(Player player) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.setBattlePassLevel(0);
        pd.setBattlePassXp(0);
        pd.setBattlePassPremium(false);
        pd.getBattlePassClaimedFree().clear();
        pd.getBattlePassClaimedPremium().clear();
        pd.setBattlePassSeason(seasonNumber);
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    // ===== GETTERS =====

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    public List<RewardEntry> getFreeRewards(int level) {
        return freeRewards.getOrDefault(level, List.of());
    }

    public List<RewardEntry> getPremiumRewards(int level) {
        return premiumRewards.getOrDefault(level, List.of());
    }

    /**
     * Retorna a descrição legível de uma reward.
     */
    public String getRewardDescription(RewardEntry reward) {
        return switch (reward.type()) {
            case "money" -> "§a$" + reward.amount();
            case "claim_blocks" -> "§e" + reward.amount() + " blocos";
            case "crate_key" -> "§b" + reward.amount() + "x Key " + capitalize(reward.extra());
            case "title" -> "§6Título: " + reward.extra();
            case "cosmetic" -> "§dCosmético: " + reward.extra();
            case "custom_item" -> "§cItem: " + reward.extra();
            default -> "§7Recompensa";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ===== RECORD =====

    /**
     * Representa uma recompensa do Battle Pass.
     */
    public record RewardEntry(String type, int amount, String extra) {
    }
}
