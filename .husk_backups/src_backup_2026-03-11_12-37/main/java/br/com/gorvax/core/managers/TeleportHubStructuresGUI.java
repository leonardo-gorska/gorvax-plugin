package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
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
 * B37.4 — Sub-GUI paginada de Cidades/Estruturas (POIs).
 * Lista todas as estruturas de structures.yml.
 */
public class TeleportHubStructuresGUI implements Listener {

    private final GorvaxCore plugin;

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length;

    public TeleportHubStructuresGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre a GUI de Estruturas na página especificada.
     */
    public void open(Player player, int page) {
        MessageManager msg = plugin.getMessageManager();
        StructureManager structureManager = plugin.getStructureManager();

        List<StructureManager.StructureData> structures = new ArrayList<>(structureManager.getAll());

        int totalPages = Math.max(1, (int) Math.ceil((double) structures.size() / ITEMS_PER_PAGE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        String title = msg.get("teleport_hub.structures_title") + " §8(" + (currentPage + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(new StructuresHolder(currentPage), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Bordas
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // Itens da página
        int start = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < structures.size(); i++) {
            StructureManager.StructureData structure = structures.get(start + i);
            inv.setItem(CONTENT_SLOTS[i], createStructureItem(structure, player));
        }

        // Navegação
        if (currentPage > 0) {
            inv.setItem(SLOT_PREV, createItem(Material.ARROW, "§e◀ Página Anterior"));
        }
        inv.setItem(SLOT_BACK, createItem(Material.BARRIER, "§c◀ Voltar ao Hub"));
        if (currentPage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, createItem(Material.ARROW, "§ePróxima Página ▶"));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * Abre versão Bedrock (SimpleForm).
     */
    public void openBedrock(Player player) {
        MessageManager msg = plugin.getMessageManager();
        StructureManager structureManager = plugin.getStructureManager();

        List<StructureManager.StructureData> structures = new ArrayList<>(structureManager.getAll());

        List<String> buttons = new ArrayList<>();
        for (StructureManager.StructureData s : structures) {
            double dist = s.distanceTo(player.getLocation());
            String distStr = dist < Double.MAX_VALUE ? String.format("%.0f", dist) + "m" : "?";
            buttons.add(s.nome() + " §7(" + distStr + ")");
        }
        buttons.add("§c◀ Voltar");

        plugin.getBedrockFormManager().sendSimpleForm(player,
                msg.get("teleport_hub.structures_title"),
                "§7Selecione um local para visitar:",
                buttons,
                index -> {
                    if (index < 0) return;
                    if (index < structures.size()) {
                        StructureManager.StructureData s = structures.get(index);
                        player.performCommand("estrutura tp " + s.id());
                    } else {
                        plugin.getTeleportHubGUI().open(player);
                    }
                });
    }

    /**
     * Cria item para representar uma estrutura.
     */
    private ItemStack createStructureItem(StructureManager.StructureData structure, Player viewer) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize("§e§l" + structure.nome()));

            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§7Tema: §f" + structure.tema()));

            // Distância do jogador
            double dist = structure.distanceTo(viewer.getLocation());
            String distStr = dist < Double.MAX_VALUE ? String.format("%.0f", dist) + " blocos" : "Mundo diferente";
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§7Distância: §f" + distStr));

            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§aClique para teleportar!"));

            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── Listener de Cliques ────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StructuresHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        if (slot == SLOT_BACK) {
            plugin.getTeleportHubGUI().open(player);
            return;
        }

        if (slot == SLOT_PREV) {
            open(player, holder.getPage() - 1);
            return;
        }

        if (slot == SLOT_NEXT) {
            open(player, holder.getPage() + 1);
            return;
        }

        // Clique em uma estrutura — extrair ID do nome
        if (clicked.getType() == Material.FILLED_MAP && clicked.hasItemMeta()) {
            ItemMeta meta = clicked.getItemMeta();
            Component displayName = meta.displayName();
            if (displayName != null) {
                String name = LegacyComponentSerializer.legacySection().serialize(displayName);
                name = name.replaceAll("§[0-9a-fk-or]", "").trim();

                // Encontrar estrutura pelo nome
                StructureManager sm = plugin.getStructureManager();
                for (StructureManager.StructureData s : sm.getAll()) {
                    String cleanNome = s.nome().replaceAll("§[0-9a-fk-or]", "").trim();
                    if (cleanNome.equals(name)) {
                        player.closeInventory();
                        player.performCommand("estrutura tp " + s.id());
                        return;
                    }
                }
            }
        }
    }

    // ─── Utilitários ────────────────────────────────────────────────

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * InventoryHolder customizado com paginação.
     */
    public static class StructuresHolder implements InventoryHolder {
        private final int page;

        public StructuresHolder(int page) {
            this.page = page;
        }

        public int getPage() {
            return page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
