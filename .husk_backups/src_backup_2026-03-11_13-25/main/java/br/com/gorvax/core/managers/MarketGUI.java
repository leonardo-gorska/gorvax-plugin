package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.MarketManager.MarketCategory;
import br.com.gorvax.core.managers.MarketManager.MarketItem;
import br.com.gorvax.core.managers.MarketManager.MarketListing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Criação e montagem de inventários/GUIs do sistema de mercado.
 * Responsável por todas as 6 telas: Global, Categoria, Lojas, Listings, Meus
 * Itens, Seleção de Venda.
 */
public class MarketGUI {

    private final GorvaxCore plugin;
    private final GlobalMarketManager globalMarket;
    private final Map<String, List<MarketListing>> localListings;

    public MarketGUI(GorvaxCore plugin, GlobalMarketManager globalMarket,
            Map<String, List<MarketListing>> localListings) {
        this.plugin = plugin;
        this.globalMarket = globalMarket;
        this.localListings = localListings;
    }

    // =============================
    // MERCADO GLOBAL
    // =============================

    public void openGlobalMarket(Player p, String title) {
        MarketHolder holder = new MarketHolder(MarketHolder.MarketType.GLOBAL);
        Inventory inv = Bukkit.createInventory(holder, 36,
                LegacyComponentSerializer.legacySection().deserialize(title));
        holder.setInventory(inv);

        for (MarketCategory cat : globalMarket.getCategories().values()) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Clique para ver itens");
            lore.add("§7Total de itens: §f" + cat.items.size());

            ItemStack icon = createItem(cat.icon, cat.name, lore);
            if (cat.slot >= 0 && cat.slot < 36) {
                inv.setItem(cat.slot, icon);
            } else {
                inv.addItem(icon);
            }
        }

        p.openInventory(inv);
    }

    public void openCategory(Player p, String categoryId) {
        MarketCategory cat = globalMarket.getCategories().get(categoryId);
        if (cat == null)
            return;

        MarketHolder holder = new MarketHolder(MarketHolder.MarketType.CATEGORY, null, 1, categoryId);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize("§8Mercado: " + cat.name));
        holder.setInventory(inv);

        for (MarketItem item : cat.items) {
            List<String> lore = new ArrayList<>();

            double currentBuy = globalMarket.getPrice(item, true);
            double currentSell = globalMarket.getPrice(item, false);

            // Indicador de demanda
            int demand = globalMarket.getDemand(item.id);
            String trend = "§7Estável";
            if (demand > 50)
                trend = "§cAlta Demanda (↑)";
            else if (demand < -50)
                trend = "§aBaixa Demanda (↓)";

            lore.add("");
            lore.add("§7Preço Base: $" + item.buyPrice);
            lore.add(trend + " §8[" + demand + "]");

            // B14 — Tendência de preço (histórico)
            PriceHistoryManager phm = plugin.getPriceHistoryManager();
            if (phm != null) {
                String trendStr = phm.getTrendString(item.id);
                lore.add("§7Tendência: " + trendStr);
            }

            lore.add("");
            lore.add("§aCompra: §f$" + String.format("%.2f", currentBuy));
            lore.add("§cVenda: §f$" + String.format("%.2f", currentSell));
            lore.add("");
            lore.add("§eClique Esquerdo para COMPRAR");
            lore.add("§eClique Direito para VENDER");

            ItemStack is = createItem(item.material, item.name, lore);
            inv.addItem(is);
        }

        // Botão Voltar
        ItemStack back = createItem(Material.ARROW, "§cVoltar");
        inv.setItem(49, back);

        p.openInventory(inv);
    }

    // =============================
    // MERCADO LOCAL — LOJAS
    // =============================

    public void openPlayerShopsMenu(Player p, String cityId) {
        String cityName = plugin.getKingdomManager().getNome(cityId);
        MarketHolder holder = new MarketHolder(MarketHolder.MarketType.SHOPS_MENU, cityId, 1);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize("§8Lojas do Reino: " + cityName));
        holder.setInventory(inv);

        List<MarketListing> listings = localListings.getOrDefault(cityId, new ArrayList<>());

        Map<UUID, Integer> sellerCounts = new HashMap<>();
        Map<UUID, String> sellerNames = new HashMap<>();

        for (MarketListing l : listings) {
            sellerCounts.put(l.seller, sellerCounts.getOrDefault(l.seller, 0) + 1);
            sellerNames.putIfAbsent(l.seller, l.sellerName);
        }

        ItemStack generalMarket = createItem(Material.NETHER_STAR, "§b§lMercado Geral", Arrays.asList(
                "§7Visualize todos os itens do reino",
                "§7em uma única lista.",
                "",
                "§fTotal de Ofertas: §a" + listings.size(),
                "",
                "§e[Clique para Acessar]"));
        inv.setItem(4, generalMarket);

        int slot = 9;
        for (Map.Entry<UUID, Integer> entry : sellerCounts.entrySet()) {
            if (slot >= 54)
                break;

            UUID sellerId = entry.getKey();
            String name = sellerNames.get(sellerId);
            int count = entry.getValue();

            ItemStack head = createSkull(sellerId, "§eLoja de " + name, Arrays.asList(
                    "§7" + count + " itens à venda.",
                    "",
                    "§e[Clique para visitar]"));

            ItemMeta meta = head.getItemMeta();
            NamespacedKey key = new NamespacedKey(plugin, "market_seller_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sellerId.toString());
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
        }

        // Botão para itens do próprio jogador
        ItemStack myListingsBtn = createItem(Material.ENDER_CHEST, "§b§lMEUS ITENS A VENDA", Arrays.asList(
                "§7Clique para gerenciar",
                "§7seus itens listados."));
        inv.setItem(53, myListingsBtn);

        // Botão Vender Item
        ItemStack sellBtn = createItem(Material.EMERALD, "§a§lVENDER ITEM DA MÃO", Arrays.asList(
                "§7Clique para vender o item",
                "§7que você está segurando.",
                " ",
                "§7Você define o preço no chat."));
        inv.setItem(49, sellBtn);

        p.openInventory(inv);
    }

    // =============================
    // MERCADO LOCAL — LISTINGS
    // =============================

    public void openLocalMarket(Player p, String cityId, int page, String filter, UUID sellerFilter) {
        Claim cityClaim = plugin.getClaimManager().getClaimById(cityId);
        double tax = cityClaim != null ? cityClaim.getTax() : 5.0;

        String title = "§8Mercado Local: " + cityId + " #" + page;
        if (sellerFilter != null) {
            String sellerName = Bukkit.getOfflinePlayer(sellerFilter).getName();
            title = "§8Loja: " + sellerName + " #" + page;
        } else if (filter != null && !filter.isEmpty()) {
            title = "§8Busca: " + filter + " #" + page;
        }

        MarketHolder holder = new MarketHolder(MarketHolder.MarketType.LOCAL_LISTINGS, cityId, page, filter,
                sellerFilter);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize(title));
        holder.setInventory(inv);

        // Info
        ItemStack info = createItem(Material.BOOK, "§e§lInformações", Arrays.asList(
                "§7Bem-vindo ao Mercado Local!",
                " ",
                "§7Taxa da Cidade: §c" + tax + "%",
                "§7(A taxa vai para o Prefeito)",
                " ",
                "§fFiltro: §a"
                        + (sellerFilter != null ? "Vendedor" : (filter != null ? filter : "Todos"))));
        inv.setItem(4, info);

        // Botão Pesquisar
        ItemStack searchBtn = createItem(Material.OAK_SIGN, "§e§lPESQUISAR ITEM", Arrays.asList(
                "§7Clique para buscar um item",
                "§7pelo nome."));
        inv.setItem(48, searchBtn);

        // Botão Vender
        ItemStack sellBtn = createItem(Material.EMERALD, "§a§lVENDER ITEM DA MÃO", Arrays.asList(
                "§7Clique para vender o item",
                "§7que você está segurando.",
                " ",
                "§7Você define o preço no chat."));
        inv.setItem(49, sellBtn);

        // Botão Meus Itens
        ItemStack myListingsBtn = createItem(Material.ENDER_CHEST, "§b§lMEUS ITENS A VENDA", Arrays.asList(
                "§7Clique para gerenciar",
                "§7seus itens listados."));
        inv.setItem(50, myListingsBtn);

        // Botão Voltar
        if (sellerFilter != null) {
            ItemStack back = createItem(Material.ARROW, "§cVoltar para Lojas",
                    Arrays.asList("§7Ver todos os vendedores"));
            inv.setItem(45, back);
        } else if (filter != null) {
            ItemStack back = createItem(Material.ARROW, "§cLimpar Busca", Arrays.asList("§7Ver todos os itens"));
            inv.setItem(45, back);
        } else if (page > 1) {
            ItemStack prev = createItem(Material.ARROW, "§a< Página Anterior",
                    Arrays.asList("§7Ir para página " + (page - 1)));

            if (sellerFilter != null) {
                ItemMeta meta = prev.getItemMeta();
                NamespacedKey key = new NamespacedKey(plugin, "market_seller_filter");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sellerFilter.toString());
                prev.setItemMeta(meta);
            }

            inv.setItem(45, prev);
        } else {
            // Caso base: Voltar para Lojas
            ItemStack backToShops = createItem(Material.ARROW, "§cVoltar para Lojas",
                    Arrays.asList("§7Ver menu de vendedores"));
            inv.setItem(45, backToShops);
        }

        // Paginação e Filtro
        List<MarketListing> allListings = localListings.getOrDefault(cityId, new ArrayList<>());
        List<MarketListing> displayedListings = new ArrayList<>();

        for (MarketListing l : allListings) {
            boolean matches = true;

            if (sellerFilter != null && !l.seller.equals(sellerFilter)) {
                matches = false;
            }

            if (matches && filter != null && !filter.isEmpty()) {
                String f = filter.toLowerCase();
                String name = l.item.hasItemMeta() && l.item.getItemMeta().hasDisplayName()
                        ? PlainTextComponentSerializer.plainText().serialize(l.item.getItemMeta().displayName())
                        : l.item.getType().toString();

                if (!name.toLowerCase().contains(f) && !l.item.getType().toString().toLowerCase().contains(f)) {
                    matches = false;
                }
            }

            if (matches) {
                displayedListings.add(l);
            }
        }

        int itemsPerPage = 36;
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, displayedListings.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot >= 45)
                break;
            MarketListing listing = displayedListings.get(i);

            ItemStack display = listing.item.clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(" "));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m----------------"));
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§fPreço: §a" + String.format("%.2f", listing.price)));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§fVendedor: §7" + listing.sellerName));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m----------------"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§eClique para comprar!"));

            // SAFETY: Tipo real do item para evitar Scam
            lore.add(LegacyComponentSerializer.legacySection().deserialize(" "));
            lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize("§8Tipo Original: " + listing.item.getType().name()));

            meta.lore(lore);

            // PDC ID
            NamespacedKey key = new NamespacedKey(plugin, "market_listing_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, listing.id);
            display.setItemMeta(meta);

            inv.setItem(slot++, display);
        }

        if (endIndex < displayedListings.size()) {
            ItemStack next = createItem(Material.ARROW, "§aPróxima Página >",
                    Arrays.asList("§7Ir para página " + (page + 1)));

            if (sellerFilter != null) {
                ItemMeta meta = next.getItemMeta();
                NamespacedKey key = new NamespacedKey(plugin, "market_seller_filter");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sellerFilter.toString());
                next.setItemMeta(meta);
            }

            inv.setItem(53, next);
        }

        p.openInventory(inv);
    }

    // =============================
    // MEUS ITENS
    // =============================

    public void openMyListings(Player p, String cityId) {
        MarketHolder holder = new MarketHolder(MarketHolder.MarketType.MY_LISTINGS, cityId, 1);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize("§8Meus Itens: " + cityId));
        holder.setInventory(inv);

        List<MarketListing> allListings = localListings.getOrDefault(cityId, new ArrayList<>());
        int slot = 9;

        for (MarketListing listing : allListings) {
            if (listing.seller.equals(p.getUniqueId())) {
                ItemStack display = listing.item.clone();
                ItemMeta meta = display.getItemMeta();
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize(" "));
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§fPreço: §a" + String.format("%.2f", listing.price)));
                lore.add(LegacyComponentSerializer.legacySection().deserialize(" "));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§cclique para CANCELAR venda"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§cclique para CANCELAR venda"));

                // SAFETY: Tipo real
                lore.add(LegacyComponentSerializer.legacySection().deserialize(" "));
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§8Tipo Original: " + listing.item.getType().name()));

                meta.lore(lore);

                // PDC ID
                NamespacedKey key = new NamespacedKey(plugin, "market_listing_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, listing.id);
                display.setItemMeta(meta);

                inv.setItem(slot++, display);
                if (slot >= 45)
                    break;
            }
        }

        ItemStack back = createItem(Material.BARRIER, "§cVoltar", Arrays.asList("§7Voltar ao mercado."));
        inv.setItem(49, back);

        p.openInventory(inv);
    }

    // =============================
    // SELEÇÃO DE VENDA
    // =============================

    public void openSellSelection(Player p, String cityId) {
        MarketHolder holder = new MarketHolder(MarketHolder.MarketType.SELL_SELECTION, cityId, 1);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize("§8Selecionar Item: " + cityId));
        holder.setInventory(inv);

        int slot = 0;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (ItemStack is : contents) {
            if (is != null && is.getType() != Material.AIR) {
                inv.setItem(slot++, is.clone());
                if (slot >= 45)
                    break;
            }
        }

        ItemStack back = createItem(Material.ARROW, "§cVoltar", Arrays.asList("§7Cancelar seleção"));
        inv.setItem(49, back);

        p.openInventory(inv);
    }

    // =============================
    // HELPERS
    // =============================

    public ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            if (lore != null)
                meta.lore(lore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createItem(Material mat, String name) {
        return createItem(mat, name, null);
    }

    private ItemStack createSkull(UUID ownerId, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerId));
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            if (lore != null)
                meta.lore(lore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s)).toList());
            skull.setItemMeta(meta);
        }
        return skull;
    }
}
