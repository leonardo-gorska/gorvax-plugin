package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.TeleportHubManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * B37 — Listener do Hub de Teleportes.
 * Gerencia interação com bússola, proteção do item e cancelamento de warmup.
 */
public class TeleportHubListener implements Listener {

    private final GorvaxCore plugin;

    public TeleportHubListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Clique direito com bússola abre o Hub de Teleportes.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!manager.isCompass(item)) return;

        event.setCancelled(true);
        plugin.getTeleportHubGUI().open(player);
    }

    /**
     * Dá a bússola ao jogador ao entrar no servidor.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        // Delay de 1 segundo para garantir que o inventário está carregado
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline()) {
                manager.giveCompass(player);
            }
        }, 20L);
    }

    /**
     * Impede drop da bússola.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        if (manager.isCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impede mover a bússola para baú, craft, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        // Se o jogador está clicando em um inventário que NÃO é o próprio
        // e o item clicado é a bússola, cancelar
        if (event.getInventory().getType() != InventoryType.PLAYER
                && event.getInventory().getType() != InventoryType.CRAFTING) {
            // Verificar item clicado e item no cursor
            if (manager.isCompass(event.getCurrentItem()) || manager.isCompass(event.getCursor())) {
                event.setCancelled(true);
            }
        }

        // Também prevenir shift-click da bússola quando GUIs estão abertas
        if (event.isShiftClick() && manager.isCompass(event.getCurrentItem())) {
            if (event.getInventory().getType() != InventoryType.PLAYER
                    && event.getInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Impede arrastar a bússola em inventários.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        if (manager.isCompass(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impede perda da bússola ao morrer (recria após respawn).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        // Remover bússola dos drops
        event.getDrops().removeIf(manager::isCompass);
    }

    /**
     * Devolve a bússola após respawn.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null || !manager.isEnabled()) return;

        // Delay para garantir inventário limpo após respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline()) {
                manager.giveCompass(player);
            }
        }, 5L);
    }

    /**
     * Cancela warmup ao mover.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        TeleportHubManager manager = plugin.getTeleportHubManager();
        if (manager == null) return;

        // Otimização: só verificar se há warmup ativo
        if (!manager.isInWarmup(event.getPlayer().getUniqueId())) return;

        // Verificar se realmente se moveu (não apenas rotação da câmera)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            manager.cancelWarmup(event.getPlayer().getUniqueId());
        }
    }
}
