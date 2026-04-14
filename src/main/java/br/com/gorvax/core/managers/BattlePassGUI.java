package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * B15 — GUI do Battle Pass (54 slots, 6 linhas).
 * Exibe progresso, rewards Free/Premium e permite resgate.
 */
public class BattlePassGUI implements Listener {

    private final GorvaxCore plugin;
    private static final int LEVELS_PER_PAGE = 5;

    public BattlePassGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre a GUI do Battle Pass para o jogador (página 1).
     */
    public void open(Player player) {
        open(player, 0);
    }

    /**
     * Abre a GUI do Battle Pass em uma página específica.
     */
    public void open(Player player, int page) {
        BattlePassManager bpm = plugin.getBattlePassManager();
        if (bpm == null || !bpm.isEnabled()) {
            plugin.getMessageManager().send(player, "battlepass.disabled");
            return;
        }

        // Se Bedrock, usar forms
        if (InputManager.isBedrockPlayer(player)) {
            openBedrockForm(player, page);
            return;
        }

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        int playerLevel = pd.getBattlePassLevel();
        boolean hasPremium = pd.isBattlePassPremium();
        Set<Integer> claimedFree = pd.getBattlePassClaimedFree();
        Set<Integer> claimedPremium = pd.getBattlePassClaimedPremium();

        int maxPages = (int) Math.ceil(bpm.getMaxLevel() / (double) LEVELS_PER_PAGE);
        if (page < 0)
            page = 0;
        if (page >= maxPages)
            page = maxPages - 1;

        var msg = plugin.getMessageManager();
        String title = msg.get("battlepass.gui_title", page + 1, maxPages);
        Inventory inv = Bukkit.createInventory(new BattlePassHolder(page), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // === Linha 0 (slots 0-8): Info da temporada ===
        // Slot 4: Info central
        ItemStack infoItem = createItem(Material.NETHER_STAR,
                msg.get("battlepass.gui_season_title", bpm.getSeasonName()),
                List.of(
                        msg.get("battlepass.gui_season_number", bpm.getSeasonNumber()),
                        msg.get("battlepass.gui_days_remaining", bpm.getDaysRemaining()),
                        "§r",
                        msg.get("battlepass.gui_your_level", playerLevel, bpm.getMaxLevel()),
                        msg.get("battlepass.gui_your_xp", pd.getBattlePassXp(),
                                playerLevel < bpm.getMaxLevel() ? bpm.getXpForLevel(playerLevel + 1) : 0),
                        "§r",
                        hasPremium ? "§d✦ Premium Ativo" : "§7Premium: §cInativo",
                        "§r",
                        "§e💡 Mineração, kills e login dão XP!"));
        inv.setItem(4, infoItem);

        // Barra de XP (slots 0-3 e 5-8 da linha 0)
        if (playerLevel < bpm.getMaxLevel()) {
            int xpNeeded = bpm.getXpForLevel(playerLevel + 1);
            int currentXp = pd.getBattlePassXp();
            double progress = xpNeeded > 0 ? (double) currentXp / xpNeeded : 1.0;
            int filled = (int) Math.round(progress * 8);

            for (int i = 0; i < 4; i++) {
                Material mat = (i < filled) ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                inv.setItem(i, createItem(mat, "§7Progresso XP", List.of()));
            }
            for (int i = 5; i < 9; i++) {
                Material mat = ((i - 1) < filled) ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                inv.setItem(i, createItem(mat, "§7Progresso XP", List.of()));
            }
        } else {
            for (int i = 0; i < 9; i++) {
                if (i == 4)
                    continue;
                inv.setItem(i, createItem(Material.LIME_STAINED_GLASS_PANE, "§aNível Máximo!", List.of()));
            }
        }

        // === Linhas 1-4 (5 níveis): Free na coluna 2, Premium na coluna 6 ===
        int startLevel = page * LEVELS_PER_PAGE + 1;
        for (int i = 0; i < LEVELS_PER_PAGE; i++) {
            int level = startLevel + i;
            if (level > bpm.getMaxLevel())
                break;

            int row = i + 1;
            int rowStart = row * 9;

            // Slot central: indicador de nível
            Material levelMat;
            String levelPrefix;
            if (level <= playerLevel) {
                levelMat = Material.GOLD_INGOT;
                levelPrefix = "§a✔ ";
            } else if (level == playerLevel + 1) {
                levelMat = Material.EXPERIENCE_BOTTLE;
                levelPrefix = "§e▶ ";
            } else {
                levelMat = Material.COAL;
                levelPrefix = "§8";
            }
            inv.setItem(rowStart + 4, createItem(levelMat,
                    levelPrefix + "Nível " + level,
                    List.of(level <= playerLevel ? "§aDesbloqueado!" : "§7Requer nível " + level)));

            // Free reward (coluna 2 = slot rowStart+2)
            List<BattlePassManager.RewardEntry> freeList = bpm.getFreeRewards(level);
            boolean freeClaimable = level <= playerLevel && !claimedFree.contains(level);
            boolean freeClaimed = claimedFree.contains(level);
            Material freeMat = freeClaimed ? Material.LIME_STAINED_GLASS_PANE
                    : freeClaimable ? Material.CHEST
                            : Material.GRAY_STAINED_GLASS_PANE;
            String freeName = freeClaimed ? "§a✔ Free Nível " + level
                    : freeClaimable ? "§e🎁 Resgatar Free Nível " + level
                            : "§7Free Nível " + level;

            List<String> freeLore = new ArrayList<>();
            for (BattlePassManager.RewardEntry r : freeList) {
                freeLore.add("  " + bpm.getRewardDescription(r));
            }
            if (freeClaimable)
                freeLore.add("§r§eClique para resgatar!");
            inv.setItem(rowStart + 2, createItem(freeMat, freeName, freeLore));

            // Premium reward (coluna 6 = slot rowStart+6)
            List<BattlePassManager.RewardEntry> premList = bpm.getPremiumRewards(level);
            boolean premClaimable = level <= playerLevel && hasPremium && !claimedPremium.contains(level);
            boolean premClaimed = claimedPremium.contains(level);
            Material premMat = premClaimed ? Material.MAGENTA_STAINED_GLASS_PANE
                    : premClaimable ? Material.ENDER_CHEST
                            : !hasPremium ? Material.BARRIER
                                    : Material.PURPLE_STAINED_GLASS_PANE;
            String premName = premClaimed ? "§d✔ Premium Nível " + level
                    : premClaimable ? "§d🎁 Resgatar Premium Nível " + level
                            : !hasPremium ? "§8🔒 Premium Nível " + level
                                    : "§5Premium Nível " + level;

            List<String> premLore = new ArrayList<>();
            for (BattlePassManager.RewardEntry r : premList) {
                premLore.add("  " + bpm.getRewardDescription(r));
            }
            if (!hasPremium)
                premLore.add("§r§cRequer Battle Pass Premium");
            else if (premClaimable)
                premLore.add("§r§dClique para resgatar!");
            inv.setItem(rowStart + 6, createItem(premMat, premName, premLore));
        }

        // === Linha 5 (slots 45-53): Navegação ===
        // Separador
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
        }

        // Página anterior
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "§e◀ Página Anterior", List.of("§7Página " + page)));
        }
        // Fechar
        inv.setItem(49, createItem(Material.BARRIER, "§c✖ Fechar", List.of()));
        // Próxima página
        if (page < maxPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, "§e▶ Próxima Página", List.of("§7Página " + (page + 2))));
        }

        player.openInventory(inv);
    }

    /**
     * Abre form nativo para jogadores Bedrock.
     */
    private void openBedrockForm(Player player, int page) {
        var formManager = plugin.getBedrockFormManager();
        if (formManager == null || !formManager.isAvailable()) {
            // Fallback para GUI normal
            return;
        }

        BattlePassManager bpm = plugin.getBattlePassManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        var msg = plugin.getMessageManager();

        String title = bpm.getSeasonName() + " — Nível " + pd.getBattlePassLevel() + "/" + bpm.getMaxLevel();
        String content = "XP: " + pd.getBattlePassXp() + " | Dias restantes: " + bpm.getDaysRemaining()
                + "\nPremium: " + (pd.isBattlePassPremium() ? "Ativo" : "Inativo");

        int startLevel = page * LEVELS_PER_PAGE + 1;
        List<Integer> levels = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        for (int i = 0; i < LEVELS_PER_PAGE; i++) {
            int level = startLevel + i;
            if (level > bpm.getMaxLevel())
                break;

            boolean unlocked = level <= pd.getBattlePassLevel();
            String status = unlocked ? "✔" : "🔒";
            buttons.add(status + " Nível " + level);
            levels.add(level);
        }

        formManager.sendSimpleForm(player, title, content, buttons, idx -> {
            if (idx >= 0 && idx < levels.size()) {
                int level = levels.get(idx);
                if (level <= pd.getBattlePassLevel()) {
                    // Tentar resgatar free primeiro, depois premium
                    if (!pd.getBattlePassClaimedFree().contains(level)) {
                        bpm.claimReward(player, level, false);
                    } else if (pd.isBattlePassPremium() && !pd.getBattlePassClaimedPremium().contains(level)) {
                        bpm.claimReward(player, level, true);
                    } else {
                        player.sendMessage(msg.get("battlepass.already_claimed"));
                    }
                }
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BattlePassHolder holder))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54)
            return;

        int page = holder.page;
        BattlePassManager bpm = plugin.getBattlePassManager();
        if (bpm == null)
            return;

        // Navegação
        if (slot == 45 && page > 0) {
            open(player, page - 1);
            return;
        }
        if (slot == 53) {
            int maxPages = (int) Math.ceil(bpm.getMaxLevel() / (double) LEVELS_PER_PAGE);
            if (page < maxPages - 1) {
                open(player, page + 1);
            }
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Click em reward: coluna 2 = free, coluna 6 = premium
        int column = slot % 9;
        int row = slot / 9;
        if (row < 1 || row > 5)
            return;

        int level = page * LEVELS_PER_PAGE + row;
        if (level > bpm.getMaxLevel())
            return;

        if (column == 2) {
            // Free reward
            bpm.claimReward(player, level, false);
            // Reabrir para atualizar
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, page), 2L);
        } else if (column == 6) {
            // Premium reward
            bpm.claimReward(player, level, true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, page), 2L);
        }
    }

    // ===== UTILITÁRIOS =====

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(name));
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(LegacyComponentSerializer.legacySection().deserialize(line));
            }
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * InventoryHolder customizado para identificar GUIs do Battle Pass.
     */
    public record BattlePassHolder(int page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
