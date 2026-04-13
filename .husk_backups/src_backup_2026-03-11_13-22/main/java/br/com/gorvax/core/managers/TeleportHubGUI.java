package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.utils.MenuUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * B37 — GUI principal do Hub de Teleportes (54 slots).
 * Acessível via Bússola ou /tp-hub.
 */
public class TeleportHubGUI implements Listener {

    private final GorvaxCore plugin;

    // Slots dos botões
    private static final int SLOT_SPAWN = 10;
    private static final int SLOT_RTP = 12;
    private static final int SLOT_KINGDOMS = 14;
    private static final int SLOT_STRUCTURES = 16;
    private static final int SLOT_MY_KINGDOM = 30;
    private static final int SLOT_CLOSE = 49;

    public TeleportHubGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre o Hub de Teleportes.
     * Detecta Bedrock e usa SimpleForm se disponível.
     */
    public void open(Player player) {
        BedrockFormManager bedrock = plugin.getBedrockFormManager();
        if (bedrock != null && bedrock.isAvailable() && InputManager.isBedrockPlayer(player)) {
            openBedrockForm(player);
            return;
        }
        openJavaGUI(player);
    }

    // ─── GUI Java (Inventário 54 slots) ─────────────────────────────

    private void openJavaGUI(Player player) {
        MessageManager msg = plugin.getMessageManager();
        String title = msg.get("teleport_hub.title");
        Inventory inv = Bukkit.createInventory(new TeleportHubHolder(), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Preencher bordas com vidro preto
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // ---- Linha 2 (slots 10, 12, 14, 16) ----
        inv.setItem(SLOT_SPAWN, createMenuItem(Material.RED_BED,
                msg.get("teleport_hub.spawn_name"),
                msg.get("teleport_hub.spawn_lore")));

        inv.setItem(SLOT_RTP, createMenuItem(Material.GRASS_BLOCK,
                msg.get("teleport_hub.rtp_name"),
                msg.get("teleport_hub.rtp_lore")));

        inv.setItem(SLOT_KINGDOMS, createMenuItem(Material.GOLDEN_HELMET,
                msg.get("teleport_hub.kingdoms_name"),
                msg.get("teleport_hub.kingdoms_lore")));

        inv.setItem(SLOT_STRUCTURES, createMenuItem(Material.FILLED_MAP,
                msg.get("teleport_hub.structures_name"),
                msg.get("teleport_hub.structures_lore")));

        // ---- Linha 4 (slot 30) — Meu Reino ----
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom != null) {
            inv.setItem(SLOT_MY_KINGDOM, createMenuItem(Material.OAK_DOOR,
                    msg.get("teleport_hub.my_kingdom_name"),
                    msg.get("teleport_hub.my_kingdom_lore")));
        } else {
            inv.setItem(SLOT_MY_KINGDOM, createMenuItem(Material.GRAY_DYE,
                    msg.get("teleport_hub.my_kingdom_name"),
                    msg.get("teleport_hub.my_kingdom_none")));
        }

        // ---- Linha 6 (slot 49) — Fechar ----
        inv.setItem(SLOT_CLOSE, createItem(Material.BARRIER,
                msg.get("teleport_hub.close_name")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ─── Listener de Cliques ────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeleportHubHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();
        TeleportHubManager manager = plugin.getTeleportHubManager();

        switch (slot) {
            case SLOT_SPAWN -> {
                player.closeInventory();
                manager.teleportToSpawn(player);
            }
            case SLOT_RTP -> {
                player.closeInventory();
                if (manager.canTeleport(player, "rtp")) {
                    manager.startWarmup(player, "rtp", () -> {
                        player.performCommand("rtp");
                    });
                }
            }
            case SLOT_KINGDOMS -> {
                // Abrir sub-GUI: Reinos Públicos
                new TeleportHubKingdomsGUI(plugin).open(player, 0);
            }
            case SLOT_STRUCTURES -> {
                // Abrir sub-GUI: Cidades/Estruturas
                new TeleportHubStructuresGUI(plugin).open(player, 0);
            }
            case SLOT_MY_KINGDOM -> {
                if (clicked.getType() == Material.GRAY_DYE) return; // Desativado
                player.closeInventory();
                manager.teleportToKingdom(player);
            }
            case SLOT_CLOSE -> {
                player.closeInventory();
            }
        }
    }

    // ─── Bedrock SimpleForm ─────────────────────────────────────────

    private void openBedrockForm(Player player) {
        MessageManager msg = plugin.getMessageManager();
        List<String> buttons = new ArrayList<>();
        buttons.add(msg.get("teleport_hub.spawn_name"));
        buttons.add(msg.get("teleport_hub.rtp_name"));
        buttons.add(msg.get("teleport_hub.kingdoms_name"));
        buttons.add(msg.get("teleport_hub.structures_name"));

        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom != null) {
            buttons.add(msg.get("teleport_hub.my_kingdom_name"));
        }

        plugin.getBedrockFormManager().sendSimpleForm(player,
                msg.get("teleport_hub.title"),
                msg.get("teleport_hub.bedrock_content"),
                buttons,
                index -> {
                    if (index < 0) return;
                    TeleportHubManager manager = plugin.getTeleportHubManager();
                    switch (index) {
                        case 0 -> manager.teleportToSpawn(player);
                        case 1 -> {
                            if (manager.canTeleport(player, "rtp")) {
                                manager.startWarmup(player, "rtp", () -> {
                                    player.performCommand("rtp");
                                });
                            }
                        }
                        case 2 -> new TeleportHubKingdomsGUI(plugin).openBedrock(player);
                        case 3 -> new TeleportHubStructuresGUI(plugin).openBedrock(player);
                        case 4 -> {
                            if (kingdom != null) {
                                manager.teleportToKingdom(player);
                            }
                        }
                    }
                });
    }

    // ─── Utilitários (delegados para MenuUtils) ──────────────────────

    /** Delegação para MenuUtils. */
    private ItemStack createItem(Material material, String name) {
        return MenuUtils.createItem(material, name);
    }

    /** Delegação para MenuUtils. */
    private ItemStack createMenuItem(Material material, String name, String loreLine) {
        return MenuUtils.createMenuItem(material, name, loreLine);
    }

    /**
     * InventoryHolder customizado para identificação.
     */
    public static class TeleportHubHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
