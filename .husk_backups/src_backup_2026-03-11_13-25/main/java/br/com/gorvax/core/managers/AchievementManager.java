package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * B12 — Gerencia conquistas e títulos desbloqueáveis.
 * Carrega conquistas de achievements.yml, verifica progresso via PlayerData,
 * e concede recompensas (blocos, dinheiro, títulos).
 */
public class AchievementManager {

    private final GorvaxCore plugin;
    private final Map<String, Achievement> achievements = new HashMap<>();

    public AchievementManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadAchievements();
        startPeriodicCheck();
    }

    // ========================================================================
    // Carregamento
    // ========================================================================

    private void loadAchievements() {
        achievements.clear();

        File achFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achFile.exists()) {
            plugin.saveResource("achievements.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(achFile);
        ConfigurationSection section = config.getConfigurationSection("achievements");
        if (section == null) {
            plugin.getLogger().warning("[AchievementManager] Seção 'achievements' não encontrada!");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection achSection = section.getConfigurationSection(id);
            if (achSection == null)
                continue;

            String name = achSection.getString("name", id);
            String description = achSection.getString("description", "");
            String categoryStr = achSection.getString("category", "EXPLORACAO");
            String typeStr = achSection.getString("type", "BLOCKS_BROKEN");
            int goal = achSection.getInt("goal", 1);
            String rewardTypeStr = achSection.getString("reward_type", "BLOCKS");
            String rewardValue = achSection.getString("reward_value", "0");
            String iconStr = achSection.getString("icon", "STONE");

            AchievementType type;
            try {
                type = AchievementType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger()
                        .warning("[AchievementManager] Tipo inválido '" + typeStr + "' na conquista '" + id + "'");
                continue;
            }

            RewardType rewardType;
            try {
                rewardType = RewardType.valueOf(rewardTypeStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(
                        "[AchievementManager] RewardType inválido '" + rewardTypeStr + "' na conquista '" + id + "'");
                continue;
            }

            Material icon;
            try {
                icon = Material.valueOf(iconStr);
            } catch (IllegalArgumentException e) {
                icon = Material.STONE;
            }

            achievements.put(id,
                    new Achievement(id, name, description, categoryStr, type, goal, rewardType, rewardValue, icon));
        }

        plugin.getLogger().info("[AchievementManager] " + achievements.size() + " conquistas carregadas.");
    }

    /**
     * Verificação periódica de conquistas baseadas em tempo (PLAYTIME_HOURS).
     * Roda a cada 5 minutos (6000 ticks) por padrão.
     */
    private void startPeriodicCheck() {
        long interval = plugin.getConfig().getLong("achievements.check_interval", 6000L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkAll(player);
            }
        }, interval, interval);
    }

    // ========================================================================
    // Verificação de conquistas
    // ========================================================================

    /**
     * Verifica todas as conquistas de um tipo específico para o jogador.
     */
    public void checkAndUnlock(Player player, AchievementType type) {
        if (player == null)
            return;
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        for (Achievement ach : achievements.values()) {
            if (ach.type != type)
                continue;
            if (pd.hasAchievement(ach.id))
                continue;

            int progress = getProgress(player, ach);
            if (progress >= ach.goal) {
                unlockAchievement(player, ach);
            }
        }
    }

    /**
     * Verifica todas as conquistas do jogador (usado em verificações periódicas).
     */
    public void checkAll(Player player) {
        if (player == null)
            return;
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        for (Achievement ach : achievements.values()) {
            if (pd.hasAchievement(ach.id))
                continue;

            int progress = getProgress(player, ach);
            if (progress >= ach.goal) {
                unlockAchievement(player, ach);
            }
        }
    }

    /**
     * Retorna o progresso atual do jogador para uma conquista.
     */
    public int getProgress(Player player, Achievement ach) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        UUID uuid = player.getUniqueId();

        return switch (ach.type) {
            case BLOCKS_BROKEN -> pd.getTotalBlocksBroken();
            case BLOCKS_PLACED -> pd.getTotalBlocksPlaced();
            case CLAIM_CREATED -> {
                int count = (int) plugin.getClaimManager().getClaims().stream()
                        .filter(c -> c.getOwner().equals(uuid))
                        .count();
                yield count;
            }
            case KINGDOM_CREATED -> {
                Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
                yield (kingdom != null && kingdom.isKingdom()) ? 1 : 0;
            }
            case BOSSES_KILLED -> pd.getBossesKilled();
            case BOSS_TOP_DAMAGE -> pd.getBossTopDamage();
            case MONEY_EARNED -> (int) pd.getTotalMoneyEarned();
            case KILLS -> pd.getTotalKills();
            case MEMBERS_COUNT -> {
                Claim kingdom = plugin.getKingdomManager().getKingdom(uuid);
                if (kingdom == null)
                    yield 0;
                yield plugin.getKingdomManager().getSuditosCount(kingdom.getId());
            }
            case PLAYTIME_HOURS -> {
                long ms = pd.getTotalPlayTime();
                yield (int) (ms / 3_600_000L); // milissegundos → horas
            }
            case CODEX_PERCENT -> {
                int[] progress = plugin.getCodexManager().getProgress(uuid);
                yield progress[1] > 0 ? (progress[0] * 100) / progress[1] : 0;
            }
        };
    }

    // ========================================================================
    // Desbloqueio e recompensas
    // ========================================================================

    /**
     * Desbloqueia uma conquista e concede a recompensa ao jogador.
     */
    private void unlockAchievement(Player player, Achievement ach) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        pd.unlockAchievement(ach.id);

        MessageManager msg = plugin.getMessageManager();

        // Conceder recompensa
        switch (ach.rewardType) {
            case BLOCKS -> {
                int blocks = parseIntSafe(ach.rewardValue, 0);
                pd.addClaimBlocks(blocks);
                msg.send(player, "achievements.reward_blocks", blocks);
            }
            case MONEY -> {
                double money = parseDoubleSafe(ach.rewardValue, 0);
                GorvaxCore.getEconomy().depositPlayer(player, money);
                msg.send(player, "achievements.reward_money", String.format("%.2f", money));
            }
            case TITLE -> {
                pd.addUnlockedTitle(ach.rewardValue);
                msg.send(player, "achievements.reward_title", ach.rewardValue);
            }
        }

        // Notificação visual
        msg.sendTitle(player, "achievements.unlock_title", "achievements.unlock_subtitle",
                10, 60, 20, ach.name);

        // Som
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Fogos de artifício
        if (plugin.getConfig().getBoolean("achievements.fireworks_on_unlock", true)) {
            Bukkit.getScheduler().runTask(plugin, () -> spawnFirework(player));
        }

        // Broadcast opcional
        if (plugin.getConfig().getBoolean("achievements.broadcast_on_unlock", false)) {
            msg.broadcast("achievements.broadcast", player.getName(), ach.name);
        }

        // B8 — Discord: alerta de conquista desbloqueada
        plugin.getDiscordManager().sendAchievementAlert(player.getName(), ach.name);

        // Salvar dados
        plugin.getPlayerDataManager().saveData(player.getUniqueId());

        plugin.getLogger().info("[Conquista] " + player.getName() + " desbloqueou: " + ach.id);
    }

    private void spawnFirework(Player player) {
        Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.ORANGE)
                .withFade(Color.RED)
                .with(FireworkEffect.Type.STAR)
                .withTrail()
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
    }

    // ========================================================================
    // GUIs
    // ========================================================================

    /**
     * Abre o menu de conquistas para o jogador.
     * GUI de 54 slots com conquistas organizadas por categoria.
     */
    public void openAchievementMenu(Player player) {
        MessageManager msg = plugin.getMessageManager();
        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacySection().deserialize(msg.get("achievements.menu_title")));

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        int slot = 0;
        for (Achievement ach : achievements.values()) {
            if (slot >= 53)
                break;

            boolean unlocked = pd.hasAchievement(ach.id);
            int progress = getProgress(player, ach);

            ItemStack item;
            if (unlocked) {
                item = new ItemStack(ach.icon);
            } else {
                item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(ach.name));

                List<String> lore = new ArrayList<>();
                lore.add("§7" + ach.description);
                lore.add("");

                if (unlocked) {
                    lore.add("§a✔ Conquista desbloqueada!");
                } else {
                    String progressBar = buildProgressBar(progress, ach.goal);
                    lore.add("§fProgresso: §e" + progress + "§7/§e" + ach.goal + " " + progressBar);
                }

                lore.add("");
                // Recompensa
                switch (ach.rewardType) {
                    case BLOCKS -> lore.add("§fRecompensa: §a+" + ach.rewardValue + " blocos");
                    case MONEY -> lore.add("§fRecompensa: §6$" + ach.rewardValue);
                    case TITLE -> lore.add("§fRecompensa: §bTítulo " + ach.rewardValue);
                }

                lore.add("");
                lore.add("§8Categoria: " + ach.category);

                meta.lore(lore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                        .collect(Collectors.toList()));
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slot++;
        }

        // Ícone de info no slot 53
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§6§lInformações"));
            List<String> infoLore = new ArrayList<>();
            long unlockedCount = achievements.values().stream()
                    .filter(a -> pd.hasAchievement(a.id))
                    .count();
            infoLore.add("§fConquistas: §e" + unlockedCount + "§7/§e" + achievements.size());
            infoLore.add("");
            infoLore.add("§7Complete conquistas para ganhar");
            infoLore.add("§7blocos, dinheiro e títulos!");
            infoMeta.lore(infoLore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                    .collect(Collectors.toList()));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(53, info);

        player.openInventory(inv);
    }

    /**
     * Abre o menu de seleção de títulos para o jogador.
     */
    public void openTitleMenu(Player player) {
        MessageManager msg = plugin.getMessageManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        Set<String> titles = pd.getUnlockedTitles();

        int size = Math.min(54, ((titles.size() + 1) / 9 + 1) * 9);
        if (size < 9)
            size = 9;

        Inventory inv = Bukkit.createInventory(null, size,
                LegacyComponentSerializer.legacySection().deserialize(msg.get("titles.menu_title")));

        int slot = 0;

        // Opção para remover título
        ItemStack removeItem = new ItemStack(Material.BARRIER);
        ItemMeta removeMeta = removeItem.getItemMeta();
        if (removeMeta != null) {
            removeMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§c§lRemover Título"));
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para remover seu título ativo.");
            if (pd.getActiveTitle().isEmpty()) {
                lore.add("");
                lore.add("§a▸ Selecionado atualmente");
            }
            removeMeta.lore(lore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                    .collect(Collectors.toList()));
            removeItem.setItemMeta(removeMeta);
        }
        inv.setItem(slot++, removeItem);

        // Títulos desbloqueados
        for (String title : titles) {
            if (slot >= size)
                break;

            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(title));
                List<String> lore = new ArrayList<>();
                lore.add("§7Clique para selecionar este título.");
                if (title.equals(pd.getActiveTitle())) {
                    lore.add("");
                    lore.add("§a▸ Selecionado atualmente");
                }
                lore.add("");
                lore.add("§7Preview: " + title + " §f" + player.getName());
                meta.lore(lore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                        .collect(Collectors.toList()));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    // ========================================================================
    // Utilidades
    // ========================================================================

    private String buildProgressBar(int current, int max) {
        int bars = 10;
        int filled = max > 0 ? Math.min(bars, (current * bars) / max) : 0;
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a■" : "§7■");
        }
        sb.append("§8]");
        return sb.toString();
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDoubleSafe(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public Map<String, Achievement> getAchievements() {
        return achievements;
    }

    public void reload() {
        loadAchievements();
    }

    public int getUnlockedCount(UUID uuid) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        return (int) achievements.values().stream()
                .filter(a -> pd.hasAchievement(a.id))
                .count();
    }

    public int getTotalCount() {
        return achievements.size();
    }

    // ========================================================================
    // Tipos e modelo de dados
    // ========================================================================

    public enum AchievementType {
        BLOCKS_BROKEN,
        BLOCKS_PLACED,
        CLAIM_CREATED,
        KINGDOM_CREATED,
        BOSSES_KILLED,
        BOSS_TOP_DAMAGE,
        MONEY_EARNED,
        KILLS,
        MEMBERS_COUNT,
        PLAYTIME_HOURS,
        CODEX_PERCENT // B28 — % do Códex desbloqueado
    }

    public enum RewardType {
        BLOCKS,
        MONEY,
        TITLE
    }

    public static class Achievement {
        public final String id;
        public final String name;
        public final String description;
        public final String category;
        public final AchievementType type;
        public final int goal;
        public final RewardType rewardType;
        public final String rewardValue;
        public final Material icon;

        public Achievement(String id, String name, String description, String category,
                AchievementType type, int goal, RewardType rewardType,
                String rewardValue, Material icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.type = type;
            this.goal = goal;
            this.rewardType = rewardType;
            this.rewardValue = rewardValue;
            this.icon = icon;
        }
    }
}
