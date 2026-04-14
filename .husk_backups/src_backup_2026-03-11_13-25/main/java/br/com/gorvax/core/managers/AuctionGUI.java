package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * B14.1 — GUI de leilões (inventário paginado).
 * Mostra leilões ativos com informações detalhadas.
 */
public class AuctionGUI {

    private static final int ITEMS_PER_PAGE = 36; // 4 linhas de 9
    private static final NamespacedKey AUCTION_KEY = new NamespacedKey("gorvaxcore", "auction_id");
    private static final NamespacedKey AUCTION_ACTION = new NamespacedKey("gorvaxcore", "auction_action");

    /**
     * Abre a GUI de listagem de leilões ativos.
     */
    public static void openAuctionList(GorvaxCore plugin, Player p, int page) {
        AuctionManager am = plugin.getAuctionManager();
        var msg = plugin.getMessageManager();

        List<AuctionManager.Auction> auctions = am.getActiveAuctions().stream()
                .filter(a -> !a.isExpired())
                .sorted(Comparator.comparingLong(AuctionManager.Auction::getRemainingSeconds))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) auctions.size() / ITEMS_PER_PAGE));
        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        String title = msg.get("auction.gui_title", String.valueOf(page), String.valueOf(totalPages));
        Inventory inv = Bukkit.createInventory(
                new MarketHolder("auction_list", null, page),
                54, LegacyComponentSerializer.legacySection().deserialize(title));

        // Preencher fundo da última linha
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Itens de leilão
        int startIdx = (page - 1) * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, auctions.size());

        for (int i = startIdx; i < endIdx; i++) {
            AuctionManager.Auction auction = auctions.get(i);
            int slot = i - startIdx;

            ItemStack display = auction.item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta == null)
                meta = Bukkit.getItemFactory().getItemMeta(display.getType());

            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m─────────────────"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Vendedor: §f" + auction.sellerName));

            if (auction.currentBid > 0) {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Lance atual: §a$" + String.format("%.2f", auction.currentBid)));
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Licitante: §e" + auction.currentBidderName));
            } else {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Preço mínimo: §a$" + String.format("%.2f", auction.minPrice)));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Lances: §cNenhum"));
            }

            long remaining = auction.getRemainingSeconds();
            String timeColor = remaining <= 30 ? "§c" : remaining <= 120 ? "§e" : "§a";
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§7Tempo: " + timeColor + am.formatTime(remaining)));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Lances: §f" + auction.bidCount));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7ID: §8" + auction.id));
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§e▶ Clique para dar lance"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m─────────────────"));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(AUCTION_KEY, PersistentDataType.STRING, auction.id);
            meta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "bid");
            display.setItemMeta(meta);
            inv.setItem(slot, display);
        }

        // Botões de navegação
        if (page > 1) {
            ItemStack prev = createItem(Material.ARROW, "§a◀ Página Anterior");
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "page");
            prevMeta.getPersistentDataContainer().set(AUCTION_KEY, PersistentDataType.STRING, String.valueOf(page - 1));
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        if (page < totalPages) {
            ItemStack next = createItem(Material.ARROW, "§a▶ Próxima Página");
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "page");
            nextMeta.getPersistentDataContainer().set(AUCTION_KEY, PersistentDataType.STRING, String.valueOf(page + 1));
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        // Botão central: Coletar Pendências
        boolean hasPending = am.hasPendingCollections(p.getUniqueId());
        ItemStack collectBtn = createItem(
                hasPending ? Material.CHEST : Material.ENDER_CHEST,
                hasPending ? "§a§l✉ Coletar Pendências" : "§7Sem pendências");
        ItemMeta collectMeta = collectBtn.getItemMeta();
        if (hasPending) {
            List<String> collectLore = new ArrayList<>();
            collectLore.add("§7Você tem itens/dinheiro para coletar!");
            collectLore.add("§e▶ Clique para coletar");
            collectMeta.lore(
                    collectLore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s)).toList());
        }
        collectMeta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "collect");
        collectBtn.setItemMeta(collectMeta);
        inv.setItem(49, collectBtn);

        // Botão: Info/Ajuda
        ItemStack helpBtn = createItem(Material.BOOK, "§6§lAjuda - Leilão");
        ItemMeta helpMeta = helpBtn.getItemMeta();
        List<String> helpLore = new ArrayList<>();
        helpLore.add("§7/leilao iniciar <preço> [duração]");
        helpLore.add("§7/leilao lance <valor> [ID]");
        helpLore.add("§7/leilao coletar");
        helpLore.add("§7/leilao cancelar <ID>");
        helpMeta.lore(helpLore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s)).toList());
        helpBtn.setItemMeta(helpMeta);
        inv.setItem(47, helpBtn);

        // Botão: Meus Leilões
        long myCount = auctions.stream()
                .filter(a -> a.sellerUUID.equals(p.getUniqueId()))
                .count();
        ItemStack myBtn = createItem(Material.GOLD_INGOT, "§e§lMeus Leilões §7(" + myCount + ")");
        ItemMeta myMeta = myBtn.getItemMeta();
        myMeta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "my_auctions");
        myBtn.setItemMeta(myMeta);
        inv.setItem(51, myBtn);

        p.openInventory(inv);
    }

    /**
     * Abre a GUI mostrando apenas os leilões do jogador.
     */
    public static void openMyAuctions(GorvaxCore plugin, Player p) {
        AuctionManager am = plugin.getAuctionManager();
        var msg = plugin.getMessageManager();

        List<AuctionManager.Auction> myAuctions = am.getActiveAuctions().stream()
                .filter(a -> a.sellerUUID.equals(p.getUniqueId()) && !a.isExpired())
                .sorted(Comparator.comparingLong(AuctionManager.Auction::getRemainingSeconds))
                .collect(Collectors.toList());

        String title = msg.get("auction.gui_my_title");
        Inventory inv = Bukkit.createInventory(
                new MarketHolder("auction_my", null, 1),
                54, LegacyComponentSerializer.legacySection().deserialize(title));

        // Preencher fundo
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        for (int i = 0; i < Math.min(myAuctions.size(), ITEMS_PER_PAGE); i++) {
            AuctionManager.Auction auction = myAuctions.get(i);

            ItemStack display = auction.item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta == null)
                meta = Bukkit.getItemFactory().getItemMeta(display.getType());

            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m─────────────────"));
            if (auction.currentBid > 0) {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Lance atual: §a$" + String.format("%.2f", auction.currentBid)));
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Licitante: §e" + auction.currentBidderName));
            } else {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§7Preço mínimo: §a$" + String.format("%.2f", auction.minPrice)));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Lances: §cNenhum"));
            }
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§7Tempo: §e" + am.formatTime(auction.getRemainingSeconds())));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7ID: §8" + auction.id));
            if (auction.bidCount == 0) {
                lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§c▶ Clique para cancelar"));
            }
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m─────────────────"));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(AUCTION_KEY, PersistentDataType.STRING, auction.id);
            meta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "cancel_own");
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }

        // Botão voltar
        ItemStack backBtn = createItem(Material.BARRIER, "§c§lVoltar");
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.getPersistentDataContainer().set(AUCTION_ACTION, PersistentDataType.STRING, "back");
        backBtn.setItemMeta(backMeta);
        inv.setItem(49, backBtn);

        p.openInventory(inv);
    }

    /**
     * Processa cliques no inventário de leilão.
     * Chamado pelo MarketListener.
     */
    public static void handleClick(GorvaxCore plugin, Player p, ItemStack clicked, String holderType) {
        if (clicked == null || !clicked.hasItemMeta())
            return;

        ItemMeta meta = clicked.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        if (!pdc.has(AUCTION_ACTION, PersistentDataType.STRING))
            return;

        String action = pdc.get(AUCTION_ACTION, PersistentDataType.STRING);
        String value = pdc.getOrDefault(AUCTION_KEY, PersistentDataType.STRING, "");

        switch (action) {
            case "bid" -> {
                p.closeInventory();
                // Solicitar valor do lance via InputManager
                String auctionId = value;
                AuctionManager.Auction auction = plugin.getAuctionManager().getAuction(auctionId);
                if (auction == null || auction.isExpired()) {
                    plugin.getMessageManager().send(p, "auction.expired");
                    return;
                }
                double minBid = auction.currentBid > 0
                        ? auction.currentBid + plugin.getConfig().getDouble("auction.bid_increment", 1.0)
                        : auction.minPrice;
                plugin.getInputManager().openAnvilInput(p,
                        "Seu lance (mín: $" + String.format("%.2f", minBid) + ")",
                        String.format("%.0f", minBid),
                        input -> {
                            try {
                                double bid = Double.parseDouble(input);
                                Bukkit.getScheduler().runTask(plugin,
                                        () -> plugin.getAuctionManager().placeBid(p, auctionId, bid));
                            } catch (NumberFormatException e) {
                                plugin.getMessageManager().send(p, "auction.invalid_price");
                            }
                        });
            }
            case "page" -> {
                int page = 1;
                try {
                    page = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
                openAuctionList(plugin, p, page);
            }
            case "collect" -> {
                p.closeInventory();
                plugin.getAuctionManager().collectPending(p);
            }
            case "my_auctions" -> openMyAuctions(plugin, p);
            case "cancel_own" -> {
                p.closeInventory();
                plugin.getAuctionManager().cancelAuction(p, value);
            }
            case "back" -> openAuctionList(plugin, p, 1);
        }
    }

    // Utilitário para criar itens decorativos
    private static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static NamespacedKey getAuctionActionKey() {
        return AUCTION_ACTION;
    }
}
