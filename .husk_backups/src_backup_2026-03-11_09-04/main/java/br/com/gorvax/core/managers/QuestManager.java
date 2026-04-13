package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B16 — Gerenciador de Quests Diárias e Semanais.
 * Seleciona quests aleatórias por dia/semana (seed determinística),
 * rastreia progresso dos jogadores e entrega recompensas.
 */
public class QuestManager {

    private final GorvaxCore plugin;
    private File questFile;
    private FileConfiguration questConfig;

    // Pools carregados do YAML
    private final Map<String, QuestDefinition> dailyPool = new LinkedHashMap<>();
    private final Map<String, QuestDefinition> weeklyPool = new LinkedHashMap<>();
    private final Map<String, LoreQuestDefinition> loreQuestPool = new LinkedHashMap<>();

    // Quests ativas para hoje/semana (selecionadas deterministicamente)
    private List<String> activeDailyIds = new ArrayList<>();
    private String activeWeeklyId = null;

    // Cache de progresso por jogador — carregado/salvo via PlayerData
    // Não mantemos cache separado — usamos PlayerData diretamente

    private int dailyCount = 3;

    public QuestManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        selectActiveQuests();
    }

    // ===== Configuração =====

    private void loadConfig() {
        questFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questFile.exists()) {
            plugin.saveResource("quests.yml", false);
        }
        questConfig = YamlConfiguration.loadConfiguration(questFile);

        dailyPool.clear();
        weeklyPool.clear();

        // Configurações gerais
        dailyCount = questConfig.getInt("settings.daily_count", 3);

        // Carregar pool diário
        ConfigurationSection dailySec = questConfig.getConfigurationSection("daily_pool");
        if (dailySec != null) {
            for (String id : dailySec.getKeys(false)) {
                QuestDefinition def = parseQuest(id, dailySec.getConfigurationSection(id));
                if (def != null)
                    dailyPool.put(id, def);
            }
        }

        // Carregar pool semanal
        ConfigurationSection weeklySec = questConfig.getConfigurationSection("weekly_pool");
        if (weeklySec != null) {
            for (String id : weeklySec.getKeys(false)) {
                QuestDefinition def = parseQuest(id, weeklySec.getConfigurationSection(id));
                if (def != null)
                    weeklyPool.put(id, def);
            }
        }

        plugin.getLogger()
                .info("[B16] Quests carregadas: " + dailyPool.size() + " diárias, " + weeklyPool.size() + " semanais.");

        // Carregar lore quests
        loadLoreQuests();
    }

    @SuppressWarnings("unchecked")
    private void loadLoreQuests() {
        loreQuestPool.clear();

        ConfigurationSection loreSec = questConfig.getConfigurationSection("lore_quests");
        if (loreSec == null)
            return;

        for (String questId : loreSec.getKeys(false)) {
            ConfigurationSection sec = loreSec.getConfigurationSection(questId);
            if (sec == null)
                continue;

            String name = sec.getString("name", "§eQuest " + questId);
            String description = sec.getString("description", "");
            Material icon = Material.PAPER;
            try {
                icon = Material.valueOf(sec.getString("icon", "PAPER").toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }

            // Parse steps
            List<LoreQuestStep> steps = new ArrayList<>();
            List<?> stepList = sec.getList("steps");
            if (stepList != null) {
                for (Object obj : stepList) {
                    if (obj instanceof Map<?, ?> map) {
                        Map<String, Object> stepMap = (Map<String, Object>) map;
                        QuestType type;
                        try {
                            type = QuestType
                                    .valueOf(String.valueOf(stepMap.getOrDefault("type", "KILL_MOB")).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        String target = String.valueOf(stepMap.getOrDefault("target", "ANY"));
                        int amount = stepMap.containsKey("amount") ? ((Number) stepMap.get("amount")).intValue() : 1;
                        String dialogue = String.valueOf(stepMap.getOrDefault("dialogue", ""));
                        steps.add(new LoreQuestStep(type, target, amount, dialogue));
                    }
                }
            }

            // Parse reward
            ConfigurationSection rewardSec = sec.getConfigurationSection("reward");
            double money = 0;
            int karma = 0;
            String title = null;
            String book = null;
            if (rewardSec != null) {
                money = rewardSec.getDouble("money", 0);
                karma = rewardSec.getInt("karma", 0);
                title = rewardSec.getString("title", null);
                book = rewardSec.getString("book", null);
            }

            loreQuestPool.put(questId,
                    new LoreQuestDefinition(questId, name, description, icon, steps, money, karma, title, book));
        }

        plugin.getLogger().info("[Lore] " + loreQuestPool.size() + " lore quests carregadas.");
    }

    private QuestDefinition parseQuest(String id, ConfigurationSection sec) {
        if (sec == null)
            return null;

        String typeStr = sec.getString("type", "KILL_MOB");
        QuestType type;
        try {
            type = QuestType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[B16] Tipo de quest inválido: " + typeStr + " em " + id);
            return null;
        }

        String target = sec.getString("target", "ANY");
        int amount = sec.getInt("amount", 1);
        String name = sec.getString("name", "§eQuest " + id);
        String description = sec.getString("description", "");

        Material icon = Material.PAPER;
        try {
            icon = Material.valueOf(sec.getString("icon", "PAPER").toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // Recompensas
        ConfigurationSection rewardSec = sec.getConfigurationSection("reward");
        double money = 0;
        int claimBlocks = 0;
        String crateKey = null;
        String title = null;

        if (rewardSec != null) {
            money = rewardSec.getDouble("money", 0);
            claimBlocks = rewardSec.getInt("claim_blocks", 0);
            crateKey = rewardSec.getString("crate_key", null);
            title = rewardSec.getString("title", null);
        }

        return new QuestDefinition(id, type, target, amount, name, description, icon,
                money, claimBlocks, crateKey, title);
    }

    // ===== Seleção de Quests Ativas =====

    private void selectActiveQuests() {
        LocalDate today = LocalDate.now();

        // Seed determinística para o dia — todos os jogadores veem as mesmas quests
        long dailySeed = today.getYear() * 10000L + today.getDayOfYear();
        Random dailyRng = new Random(dailySeed);

        List<String> poolIds = new ArrayList<>(dailyPool.keySet());
        Collections.shuffle(poolIds, dailyRng);
        activeDailyIds = poolIds.subList(0, Math.min(dailyCount, poolIds.size()));

        // Seed determinística para a semana
        int weekNum = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        long weeklySeed = today.getYear() * 100L + weekNum;
        Random weeklyRng = new Random(weeklySeed);

        List<String> weeklyIds = new ArrayList<>(weeklyPool.keySet());
        if (!weeklyIds.isEmpty()) {
            activeWeeklyId = weeklyIds.get(weeklyRng.nextInt(weeklyIds.size()));
        }

        plugin.getLogger().info("[B16] Quests ativas — Diárias: " + activeDailyIds + " | Semanal: " + activeWeeklyId);
    }

    // ===== Acesso às quests ativas =====

    public List<String> getActiveDailyIds() {
        return Collections.unmodifiableList(activeDailyIds);
    }

    public String getActiveWeeklyId() {
        return activeWeeklyId;
    }

    public QuestDefinition getQuest(String id) {
        QuestDefinition def = dailyPool.get(id);
        if (def != null)
            return def;
        return weeklyPool.get(id);
    }

    public LoreQuestDefinition getLoreQuest(String id) {
        return loreQuestPool.get(id);
    }

    public Map<String, LoreQuestDefinition> getAllLoreQuests() {
        return Collections.unmodifiableMap(loreQuestPool);
    }

    public boolean isDailyQuest(String id) {
        return dailyPool.containsKey(id);
    }

    public boolean isWeeklyQuest(String id) {
        return weeklyPool.containsKey(id);
    }

    // ===== Progresso =====

    /**
     * Adiciona progresso a uma quest para um jogador.
     * Verifica se a quest está ativa e se o jogador ainda não a completou/resgatou.
     */
    public void addProgress(UUID uuid, QuestType type, String target, int amount) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);

        // Verificar reset necessário
        checkAndResetIfNeeded(pd);

        // Verificar quests diárias ativas
        for (String questId : activeDailyIds) {
            QuestDefinition def = dailyPool.get(questId);
            if (def == null)
                continue;
            if (def.type() != type)
                continue;
            if (!matchesTarget(def, target))
                continue;

            // Já resgatou?
            if (pd.getClaimedDailyQuests().contains(questId))
                continue;

            int current = pd.getDailyQuestProgress().getOrDefault(questId, 0);
            if (current >= def.amount())
                continue; // Já completou (mas não resgatou)

            pd.getDailyQuestProgress().put(questId, Math.min(current + amount, def.amount()));
            plugin.getPlayerDataManager().saveData(uuid);

            // Notificar se completou
            if (pd.getDailyQuestProgress().get(questId) >= def.amount()) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    plugin.getMessageManager().send(player, "quests.quest_completed",
                            def.name());
                }
            }
        }

        // Verificar quest semanal ativa
        if (activeWeeklyId != null) {
            QuestDefinition def = weeklyPool.get(activeWeeklyId);
            if (def != null && def.type() == type && matchesTarget(def, target)) {
                if (!pd.getClaimedWeeklyQuests().contains(activeWeeklyId)) {
                    int current = pd.getWeeklyQuestProgress().getOrDefault(activeWeeklyId, 0);
                    if (current < def.amount()) {
                        pd.getWeeklyQuestProgress().put(activeWeeklyId, Math.min(current + amount, def.amount()));
                        plugin.getPlayerDataManager().saveData(uuid);

                        if (pd.getWeeklyQuestProgress().get(activeWeeklyId) >= def.amount()) {
                            Player player = plugin.getServer().getPlayer(uuid);
                            if (player != null && player.isOnline()) {
                                plugin.getMessageManager().send(player, "quests.quest_completed",
                                        def.name());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Incrementa o contador de quests diárias completadas (para quest semanal
     * DAILY_COMPLETE).
     */
    public void incrementDailyCompleted(UUID uuid) {
        addProgress(uuid, QuestType.DAILY_COMPLETE, "ANY", 1);
    }

    /**
     * Adiciona progresso a lore quests permanentes (multi-step).
     * Verifica o step atual do jogador e se o type/target combinam.
     */
    public void addLoreProgress(UUID uuid, QuestType type, String target, int amount) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);

        for (var entry : loreQuestPool.entrySet()) {
            String questId = entry.getKey();
            LoreQuestDefinition lq = entry.getValue();

            // Já completou esta quest?
            if (pd.getCompletedLoreQuests().contains(questId))
                continue;

            int stepIndex = pd.getLoreQuestStep().getOrDefault(questId, 0);
            if (stepIndex >= lq.steps().size())
                continue;

            LoreQuestStep step = lq.steps().get(stepIndex);
            if (step.type() != type)
                continue;
            if (!step.target().equalsIgnoreCase("ANY") && !step.target().equalsIgnoreCase(target))
                continue;

            int current = pd.getLoreQuestStepProgress().getOrDefault(questId, 0);
            if (current >= step.amount())
                continue;

            int newProgress = Math.min(current + amount, step.amount());
            pd.getLoreQuestStepProgress().put(questId, newProgress);
            plugin.getPlayerDataManager().saveData(uuid);

            // Step concluído?
            if (newProgress >= step.amount()) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    // Exibir diálogo do step
                    if (step.dialogue() != null && !step.dialogue().isEmpty()) {
                        player.sendMessage("");
                        player.sendMessage(step.dialogue());
                        player.sendMessage("");
                    }

                    // Avançar para o próximo step
                    int nextStep = stepIndex + 1;
                    pd.getLoreQuestStep().put(questId, nextStep);
                    pd.getLoreQuestStepProgress().put(questId, 0);

                    if (nextStep >= lq.steps().size()) {
                        // Quest completa! Marcar como finalizada
                        pd.getCompletedLoreQuests().add(questId);
                        plugin.getMessageManager().send(player, "quests.lore_quest_completed", lq.name());
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f,
                                1.0f);

                        // Auto-entregar rewards
                        applyLoreReward(player, lq, pd);
                    } else {
                        plugin.getMessageManager().send(player, "quests.lore_step_completed", lq.name(),
                                String.valueOf(nextStep), String.valueOf(lq.steps().size()));
                    }
                }
                plugin.getPlayerDataManager().saveData(uuid);
            }
        }
    }

    /**
     * Aplica recompensas de uma lore quest finalizada.
     */
    private void applyLoreReward(Player player, LoreQuestDefinition lq, PlayerData pd) {
        if (lq.rewardMoney() > 0 && GorvaxCore.getEconomy() != null) {
            GorvaxCore.getEconomy().depositPlayer(player, lq.rewardMoney());
            pd.addMoneyEarned(lq.rewardMoney());
        }
        if (lq.rewardKarma() > 0) {
            pd.addKarma(lq.rewardKarma());
        }
        if (lq.rewardTitle() != null && !lq.rewardTitle().isEmpty()) {
            pd.addUnlockedTitle(lq.rewardTitle());
        }
        if (lq.rewardBook() != null && !lq.rewardBook().isEmpty()) {
            // Dar o livro de lore como reward
            if (plugin.getLoreManager() != null) {
                org.bukkit.inventory.ItemStack book = plugin.getLoreManager().createBook(lq.rewardBook());
                if (book != null) {
                    var leftover = player.getInventory().addItem(book);
                    for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
        }
        plugin.getPlayerDataManager().saveData(player.getUniqueId());
    }

    private boolean matchesTarget(QuestDefinition def, String target) {
        // Se a quest tem target "ANY", qualquer target combina
        if (def.target().equalsIgnoreCase("ANY"))
            return true;
        // Se o target buscado é "ANY" (ex: para SELL_MARKET que não tem target
        // específico)
        if (target == null || target.equalsIgnoreCase("ANY"))
            return true;
        return def.target().equalsIgnoreCase(target);
    }

    // ===== Claim de Rewards =====

    /**
     * Tenta resgatar a recompensa de uma quest.
     * 
     * @return true se resgatou com sucesso, false caso contrário.
     */
    public boolean claimReward(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        QuestDefinition def = getQuest(questId);

        if (def == null)
            return false;

        boolean isDaily = isDailyQuest(questId);
        boolean isWeekly = isWeeklyQuest(questId);

        // Verificar se está ativa
        if (isDaily && !activeDailyIds.contains(questId))
            return false;
        if (isWeekly && !questId.equals(activeWeeklyId))
            return false;

        // Verificar se já resgatou
        if (isDaily && pd.getClaimedDailyQuests().contains(questId))
            return false;
        if (isWeekly && pd.getClaimedWeeklyQuests().contains(questId))
            return false;

        // Verificar se completou
        int progress;
        if (isDaily) {
            progress = pd.getDailyQuestProgress().getOrDefault(questId, 0);
        } else {
            progress = pd.getWeeklyQuestProgress().getOrDefault(questId, 0);
        }

        if (progress < def.amount())
            return false;

        // Entregar recompensas
        if (def.rewardMoney() > 0) {
            GorvaxCore.getEconomy().depositPlayer(player, def.rewardMoney());
            pd.addMoneyEarned(def.rewardMoney());
        }
        if (def.rewardClaimBlocks() > 0) {
            pd.addClaimBlocks(def.rewardClaimBlocks());
        }
        if (def.rewardCrateKey() != null && !def.rewardCrateKey().isEmpty()) {
            pd.addCrateKey(def.rewardCrateKey(), 1);
        }
        if (def.rewardTitle() != null && !def.rewardTitle().isEmpty()) {
            pd.addUnlockedTitle(def.rewardTitle());
        }

        // Marcar como resgatada
        if (isDaily) {
            pd.getClaimedDailyQuests().add(questId);
            // Incrementar contador de daily_complete para quest semanal
            incrementDailyCompleted(uuid);
        } else {
            pd.getClaimedWeeklyQuests().add(questId);
        }

        plugin.getPlayerDataManager().saveData(uuid);

        // Mensagem de sucesso
        StringBuilder rewardMsg = new StringBuilder();
        if (def.rewardMoney() > 0)
            rewardMsg.append("§a$").append(String.format("%.0f", def.rewardMoney())).append(" ");
        if (def.rewardClaimBlocks() > 0)
            rewardMsg.append("§e").append(def.rewardClaimBlocks()).append(" blocos ");
        if (def.rewardCrateKey() != null)
            rewardMsg.append("§d1x Key ").append(def.rewardCrateKey()).append(" ");
        if (def.rewardTitle() != null)
            rewardMsg.append("§bTítulo: ").append(def.rewardTitle()).append(" ");

        plugin.getMessageManager().send(player, "quests.reward_claimed",
                def.name(), rewardMsg.toString().trim());

        return true;
    }

    // ===== Reset de Quests =====

    /**
     * Verifica se as quests do jogador precisam ser resetadas (mudança de
     * dia/semana).
     */
    public void checkAndResetIfNeeded(PlayerData pd) {
        long now = System.currentTimeMillis();
        long oneDayMs = 24L * 60L * 60L * 1000L;

        // Reset diário
        long lastDaily = pd.getLastDailyQuestReset();
        if (lastDaily == 0 || !isSameDay(lastDaily, now)) {
            pd.getDailyQuestProgress().clear();
            pd.getClaimedDailyQuests().clear();
            pd.setLastDailyQuestReset(now);
            // Re-selecionar quests ativas (podem ter mudado de dia)
            selectActiveQuests();
        }

        // Reset semanal
        long lastWeekly = pd.getLastWeeklyQuestReset();
        if (lastWeekly == 0 || !isSameWeek(lastWeekly, now)) {
            pd.getWeeklyQuestProgress().clear();
            pd.getClaimedWeeklyQuests().clear();
            pd.setLastWeeklyQuestReset(now);
            selectActiveQuests();
        }
    }

    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR);
    }

    // ===== Reload =====

    public void reload() {
        loadConfig();
        selectActiveQuests();
    }

    // ===== Utilitários =====

    /**
     * Retorna o progresso de um jogador em uma quest específica.
     */
    public int getProgress(UUID uuid, String questId) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        if (isDailyQuest(questId)) {
            return pd.getDailyQuestProgress().getOrDefault(questId, 0);
        } else if (isWeeklyQuest(questId)) {
            return pd.getWeeklyQuestProgress().getOrDefault(questId, 0);
        }
        return 0;
    }

    /**
     * Verifica se uma quest foi resgatada por um jogador.
     */
    public boolean isClaimed(UUID uuid, String questId) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        if (isDailyQuest(questId)) {
            return pd.getClaimedDailyQuests().contains(questId);
        } else if (isWeeklyQuest(questId)) {
            return pd.getClaimedWeeklyQuests().contains(questId);
        }
        return false;
    }

    /**
     * Verifica se uma quest foi completada (progresso >= amount).
     */
    public boolean isCompleted(UUID uuid, String questId) {
        QuestDefinition def = getQuest(questId);
        if (def == null)
            return false;
        return getProgress(uuid, questId) >= def.amount();
    }

    // ===== Records e Enums =====

    public enum QuestType {
        KILL_MOB,
        KILL_PLAYER,
        MINE_BLOCK,
        SELL_MARKET,
        BOSS_PARTICIPATE,
        DAILY_COMPLETE
    }

    public record QuestDefinition(
            String id,
            QuestType type,
            String target,
            int amount,
            String name,
            String description,
            Material icon,
            double rewardMoney,
            int rewardClaimBlocks,
            String rewardCrateKey,
            String rewardTitle) {
    }

    /** Definição de uma lore quest multi-step permanente. */
    public record LoreQuestDefinition(
            String id,
            String name,
            String description,
            Material icon,
            List<LoreQuestStep> steps,
            double rewardMoney,
            int rewardKarma,
            String rewardTitle,
            String rewardBook) {
    }

    /** Um step dentro de uma lore quest. */
    public record LoreQuestStep(
            QuestType type,
            String target,
            int amount,
            String dialogue) {
    }
}
