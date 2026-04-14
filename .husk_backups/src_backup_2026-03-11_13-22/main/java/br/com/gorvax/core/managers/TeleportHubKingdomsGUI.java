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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * B37.3 — Sub-GUI paginada de Reinos Públicos.
 * Lista todos os reinos com isPublic == true.
 */
public class TeleportHubKingdomsGUI implements Listener {

    private final GorvaxCore plugin;

    // Slots de conteúdo (21 slots: linhas 2-4, colunas 1-7)
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length;

    public TeleportHubKingdomsGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre a GUI de Reinos Públicos na página especificada.
     */
    public void open(Player player, int page) {
        MessageManager msg = plugin.getMessageManager();

        // Obter lista de reinos públicos
        List<Claim> kingdoms = plugin.getClaimManager().getClaims().stream()
                .filter(c -> c.isKingdom() && c.isPublic())
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) kingdoms.size() / ITEMS_PER_PAGE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        String title = msg.get("teleport_hub.kingdoms_title") + " §8(" + (currentPage + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(new KingdomsHolder(currentPage), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Bordas
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // Itens da página
        int start = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < kingdoms.size(); i++) {
            Claim kingdom = kingdoms.get(start + i);
            inv.setItem(CONTENT_SLOTS[i], createKingdomItem(kingdom, player));
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
     * Abre versão Bedrock (SimpleForm com lista de botões).
     */
    public void openBedrock(Player player) {
        MessageManager msg = plugin.getMessageManager();

        List<Claim> kingdoms = plugin.getClaimManager().getClaims().stream()
                .filter(c -> c.isKingdom() && c.isPublic())
                .collect(Collectors.toList());

        List<String> buttons = new ArrayList<>();
        for (Claim k : kingdoms) {
            String ownerName = plugin.getPlayerName(k.getOwner());
            buttons.add(k.getName() + " §7(Rei: " + ownerName + ")");
        }
        buttons.add("§c◀ Voltar");

        plugin.getBedrockFormManager().sendSimpleForm(player,
                msg.get("teleport_hub.kingdoms_title"),
                "§7Selecione um reino para visitar:",
                buttons,
                index -> {
                    if (index < 0) return;
                    if (index < kingdoms.size()) {
                        Claim k = kingdoms.get(index);
                        player.performCommand("reino visitar " + k.getName());
                    } else {
                        // Voltar ao hub
                        plugin.getTeleportHubGUI().open(player);
                    }
                });
    }

    /**
     * Cria item para representar um reino na GUI.
     */
    private ItemStack createKingdomItem(Claim kingdom, Player viewer) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            // Cabeça do rei
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(kingdom.getOwner()));

            // Nome do reino
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize("§6§l" + kingdom.getName()));

            // Lore
            List<Component> lore = new ArrayList<>();
            String ownerName = plugin.getPlayerName(kingdom.getOwner());
            int level = plugin.getKingdomManager().getKingdomLevel(kingdom.getId());
            int members = kingdom.getTrustedPlayers().size() + 1; // +1 para o dono
            double visitCost = plugin.getConfig().getDouble("kingdoms.visit_cost", 500.0);

            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Rei: §f" + ownerName));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Membros: §f" + members));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Nível: §f" + level));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§e💰 Custo: §f$" + String.format("%.0f", visitCost)));
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§aClique para visitar!"));

            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── Listener de Cliques ────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KingdomsHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        if (slot == SLOT_BACK) {
            // Voltar ao hub principal
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

        // Clique em um reino — obter nome do displayName
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            ItemMeta meta = clicked.getItemMeta();
            Component displayName = meta.displayName();
            if (displayName != null) {
                String name = LegacyComponentSerializer.legacySection().serialize(displayName);
                // Remover formatação (§6§l)
                name = name.replaceAll("§[0-9a-fk-or]", "").trim();
                player.closeInventory();
                player.performCommand("reino visitar " + name);
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
    public static class KingdomsHolder implements InventoryHolder {
        private final int page;

        public KingdomsHolder(int page) {
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
