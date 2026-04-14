package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.AchievementManager;
import br.com.gorvax.core.managers.AchievementManager.AchievementType;
import br.com.gorvax.core.managers.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

/**
 * B12 — Listener de conquistas.
 * Escuta eventos para verificar conquistas em tempo real e
 * trata interações nos menus de conquistas e títulos.
 */
public class AchievementListener implements Listener {

    private final GorvaxCore plugin;

    public AchievementListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // Eventos de progresso — verificam conquistas após cada ação
    // ========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        AchievementManager am = plugin.getAchievementManager();
        if (am != null) {
            am.checkAndUnlock(event.getPlayer(), AchievementType.BLOCKS_BROKEN);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        AchievementManager am = plugin.getAchievementManager();
        if (am != null) {
            am.checkAndUnlock(event.getPlayer(), AchievementType.BLOCKS_PLACED);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            AchievementManager am = plugin.getAchievementManager();
            if (am != null) {
                am.checkAndUnlock(killer, AchievementType.KILLS);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Verificação completa no login (PLAYTIME_HOURS, etc.)
        AchievementManager am = plugin.getAchievementManager();
        if (am != null) {
            // Schedular 1 tick para garantir que o PlayerData está carregado
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    am.checkAll(event.getPlayer());
                }
            }, 20L);
        }
    }

    // ========================================================================
    // Interações nos menus GUI
    // ========================================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        String achievementMenuTitle = plugin.getMessageManager().get("achievements.menu_title");
        String titleMenuTitle = plugin.getMessageManager().get("titles.menu_title");

        // Menu de Conquistas — bloquear interação (apenas visualização)
        if (title.equals(achievementMenuTitle)) {
            event.setCancelled(true);
            return;
        }

        // Menu de Títulos — seleção de título
        if (title.equals(titleMenuTitle)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta())
                return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName())
                return;

            PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

            // Barrier = remover título
            if (clicked.getType() == Material.BARRIER) {
                pd.setActiveTitle("");
                plugin.getPlayerDataManager().saveData(player.getUniqueId());
                plugin.getMessageManager().send(player, "titles.removed");
                // Atualizar display name
                plugin.refreshPlayerName(player);
                player.closeInventory();
                return;
            }

            // Name Tag = selecionar título
            if (clicked.getType() == Material.NAME_TAG) {
                String selectedTitle = LegacyComponentSerializer.legacySection().serialize(meta.displayName());
                if (pd.getUnlockedTitles().contains(selectedTitle)) {
                    pd.setActiveTitle(selectedTitle);
                    plugin.getPlayerDataManager().saveData(player.getUniqueId());
                    plugin.getMessageManager().send(player, "titles.selected", selectedTitle);
                    // Atualizar display name
                    plugin.refreshPlayerName(player);
                    player.closeInventory();
                }
            }
        }
    }
}
