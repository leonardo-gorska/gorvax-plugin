package br.com.gorvax.core.managers;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CodexManager.CodexCategory;
import br.com.gorvax.core.managers.CodexManager.CodexEntry;

import java.util.*;

/**
 * B28 — GUI do Códex de Gorvax.
 * Menu principal (27 slots) + sub-GUIs paginadas (54 slots).
 */
public class CodexGUI implements Listener {

    private static final String MAIN_TITLE = "§8§l📖 Códex de Gorvax";
    private static final String CAT_TITLE_PREFIX = "§8§l📖 ";
    private static final int MAIN_SIZE = 27;
    private static final int CAT_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 45; // Slots 0-44
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;

    private final GorvaxCore plugin;

    // Rastreio de páginas abertas por jogador
    private final Map<UUID, String> openCategory = new HashMap<>();
    private final Map<UUID, Integer> openPage = new HashMap<>();

    public CodexGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // ==================== Menu Principal ====================

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE,
                LegacyComponentSerializer.legacySection().deserialize(MAIN_TITLE));

        CodexManager mgr = plugin.getCodexManager();
        int[] totalProgress = mgr.getProgress(player.getUniqueId());

        // Preencher vidro decorativo
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < MAIN_SIZE; i++) {
            inv.setItem(i, glass);
        }

        // Categorias nos slots centrais (10-16)
        int slot = 10;
        for (Map.Entry<String, CodexCategory> entry : mgr.getCategories().entrySet()) {
            if (slot > 16)
                break;
            CodexCategory cat = entry.getValue();
            int[] catProgress = mgr.getCategoryProgress(player.getUniqueId(), entry.getKey());
            String progressStr = "§7" + catProgress[0] + "/" + catProgress[1];
            boolean complete = catProgress[0] == catProgress[1] && catProgress[1] > 0;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7" + cat.descricao());
            lore.add("");
            lore.add("§7Progresso: " + (complete ? "§a✔ " : "§e") + progressStr);
            lore.add("");
            lore.add("§eClique para explorar!");

            inv.setItem(slot, createItem(cat.icone(), cat.nome(), lore));
            slot++;
        }

        // Slot de progresso geral (bússola no slot 22)
        int percent = totalProgress[1] > 0 ? (totalProgress[0] * 100 / totalProgress[1]) : 0;
        List<String> progressLore = new ArrayList<>();
        progressLore.add("");
        progressLore.add("§7Desbloqueados: §e" + totalProgress[0] + "§7/§e" + totalProgress[1]);
        progressLore.add("§7Progresso: §b" + percent + "%");
        progressLore.add("");
        progressLore.add(buildProgressBar(percent));

        inv.setItem(22, createItem(Material.COMPASS, "§b§lProgresso Geral", progressLore));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

        // Limpar rastreio
        openCategory.remove(player.getUniqueId());
        openPage.remove(player.getUniqueId());
    }

    // ==================== Sub-GUI Paginada ====================

    public void openCategory(Player player, String categoryId) {
        openCategory(player, categoryId, 0);
    }

    public void openCategory(Player player, String categoryId, int page) {
        CodexManager mgr = plugin.getCodexManager();
        CodexCategory cat = mgr.getCategories().get(categoryId);
        if (cat == null)
            return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (pd == null)
            return;

        List<Map.Entry<String, CodexEntry>> entryList = new ArrayList<>(cat.entries().entrySet());
        int totalPages = Math.max(1, (int) Math.ceil(entryList.size() / (double) ENTRIES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, CAT_SIZE,
                LegacyComponentSerializer.legacySection().deserialize(CAT_TITLE_PREFIX + cat.nome()));

        // Entradas da página atual
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entryList.size());

        for (int i = start; i < end; i++) {
            Map.Entry<String, CodexEntry> entry = entryList.get(i);
            String fullKey = categoryId + "." + entry.getKey();
            CodexEntry ce = entry.getValue();
            boolean unlocked = pd.hasCodexEntry(fullKey);

            if (unlocked) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.addAll(ce.loreDesbloqueado());
                lore.add("");
                lore.add("§a✔ Desbloqueado");
                inv.setItem(i - start, createItem(ce.icone(), "§a" + ce.nome(), lore));
            } else {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.addAll(ce.loreBloqueado());
                lore.add("");
                lore.add("§c✖ Bloqueado");
                inv.setItem(i - start, createItem(Material.GRAY_STAINED_GLASS_PANE, "§7§l???", lore));
            }
        }

        // Barra de navegação (linha inferior)
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Botão Voltar
        inv.setItem(SLOT_BACK, createItem(Material.ARROW, "§c← Voltar ao Menu"));

        // Páginas
        if (page > 0) {
            inv.setItem(SLOT_PREV, createItem(Material.SPECTRAL_ARROW, "§e← Página Anterior"));
        }

        // Info central
        int[] catProgress = mgr.getCategoryProgress(player.getUniqueId(), categoryId);
        int pct = catProgress[1] > 0 ? (catProgress[0] * 100 / catProgress[1]) : 0;
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("§7" + catProgress[0] + "/" + catProgress[1] + " §8(§b" + pct + "%§8)");
        infoLore.add(buildProgressBar(pct));
        infoLore.add("");
        infoLore.add("§7Página " + (page + 1) + "/" + totalPages);
        inv.setItem(SLOT_INFO, createItem(Material.MAP, "§b" + cat.nome(), infoLore));

        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, createItem(Material.SPECTRAL_ARROW, "§ePróxima Página →"));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);

        openCategory.put(player.getUniqueId(), categoryId);
        openPage.put(player.getUniqueId(), page);
    }

    // ==================== Click Handler ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Component title = event.getView().title();
        String titleStr = LegacyComponentSerializer.legacySection().serialize(title);

        // Menu principal
        if (titleStr.equals(MAIN_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= MAIN_SIZE)
                return;

            // Slots 10-16 = categorias
            if (slot >= 10 && slot <= 16) {
                int catIndex = slot - 10;
                List<String> catIds = new ArrayList<>(plugin.getCodexManager().getCategories().keySet());
                if (catIndex < catIds.size()) {
                    openCategory(player, catIds.get(catIndex));
                }
            }
            return;
        }

        // Sub-GUI de categoria
        if (titleStr.startsWith(CAT_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= CAT_SIZE)
                return;

            String catId = openCategory.get(player.getUniqueId());
            int page = openPage.getOrDefault(player.getUniqueId(), 0);

            if (slot == SLOT_BACK) {
                openMain(player);
            } else if (slot == SLOT_PREV && page > 0) {
                openCategory(player, catId, page - 1);
            } else if (slot == SLOT_NEXT) {
                openCategory(player, catId, page + 1);
            }
        }
    }

    // ==================== Helpers ====================

    private ItemStack createItem(Material mat, String name) {
        return createItem(mat, name, Collections.emptyList());
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            if (!lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(l -> LegacyComponentSerializer.legacySection().deserialize(l))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String buildProgressBar(int percent) {
        int filled = percent / 5;
        int empty = 20 - filled;
        return "§a" + "█".repeat(Math.max(0, filled)) + "§7" + "█".repeat(Math.max(0, empty)) + " §b" + percent + "%";
    }
}
