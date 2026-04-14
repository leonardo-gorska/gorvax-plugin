package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fachada retrocompatível do sistema de mercado.
 * Delega para: GlobalMarketManager, LocalMarketManager, MarketGUI, MarketData.
 *
 * B11 — Refatoração de 1047 linhas em fachada + 4 classes especializadas.
 */
public class MarketManager {

    private final GorvaxCore plugin;
    private final GlobalMarketManager globalMarket;
    private final LocalMarketManager localMarket;
    private final MarketGUI gui;
    private final MarketData data;

    // Dados compartilhados (referências passadas para os submódulos)
    private final Map<String, List<MarketListing>> localListings = new ConcurrentHashMap<>();

    public MarketManager(GorvaxCore plugin) {
        this.plugin = plugin;

        // Inicializar submódulos com dados compartilhados
        this.globalMarket = new GlobalMarketManager(plugin);
        this.data = new MarketData(plugin, globalMarket.getItemDemand(), localListings);
        this.gui = new MarketGUI(plugin, globalMarket, localListings);
        this.localMarket = new LocalMarketManager(plugin, localListings, data, gui);

        // Carregar dados
        globalMarket.loadConfig(data);
        data.loadLocalMarket();
        data.loadMarketData();
        data.startNormalizationTask();
    }

    public void reload() {
        globalMarket.loadConfig(data);
        data.loadLocalMarket();
        data.loadMarketData();
        plugin.getLogger().info("[Mercado] Dados recarregados com sucesso!");
    }

    // =============================
    // DELEGAÇÃO — MERCADO GLOBAL
    // =============================

    public void openGlobalMarket(Player p) {
        String title = data.getMarketConfig().getString("market.title", "§8Mercado Global");
        gui.openGlobalMarket(p, title);
    }

    public void openCategory(Player p, String categoryId) {
        gui.openCategory(p, categoryId);
    }

    public double getPrice(MarketItem item, boolean isBuy) {
        return globalMarket.getPrice(item, isBuy);
    }

    public void updateDemand(String itemId, int amount) {
        globalMarket.updateDemand(itemId, amount);
    }

    public Map<String, MarketCategory> getCategories() {
        return globalMarket.getCategories();
    }

    // =============================
    // DELEGAÇÃO — MERCADO LOCAL
    // =============================

    public void openLocalMarket(Player p, String cityId, int page) {
        gui.openLocalMarket(p, cityId, page, null, null);
    }

    public void openLocalMarket(Player p, String cityId, int page, String filter) {
        gui.openLocalMarket(p, cityId, page, filter, null);
    }

    public void openLocalMarket(Player p, String cityId, int page, String filter, UUID sellerFilter) {
        gui.openLocalMarket(p, cityId, page, filter, sellerFilter);
    }

    public void openPlayerShopsMenu(Player p, String cityId) {
        gui.openPlayerShopsMenu(p, cityId);
    }

    public void openMyListings(Player p, String cityId) {
        gui.openMyListings(p, cityId);
    }

    public void openSellSelection(Player p, String cityId) {
        gui.openSellSelection(p, cityId);
    }

    public void startSearch(Player p, String cityId) {
        localMarket.startSearch(p, cityId);
    }

    public void startSelling(Player p, ItemStack item) {
        localMarket.startSelling(p, item);
    }

    public void prepareSelling(Player p, ItemStack item, String cityId) {
        localMarket.prepareSelling(p, item, cityId);
    }

    public void completeSelling(Player p, ItemStack item, String cityId, double price) {
        localMarket.completeSelling(p, item, cityId, price);
    }

    public void buyListing(Player buyer, String cityId, String listingId) {
        localMarket.buyListing(buyer, cityId, listingId);
    }

    public void cancelListing(Player p, String cityId, String listingId) {
        localMarket.cancelListing(p, cityId, listingId);
    }

    // =============================
    // DELEGAÇÃO — PERSISTÊNCIA
    // =============================

    public void saveMarketData() {
        data.saveMarketData();
    }

    public boolean saveLocalMarketSync() {
        return data.saveLocalMarketSync();
    }

    public void saveLocalMarket() {
        data.markLocalDirty();
    }

    // =============================
    // CLASSES DE DADOS (mantidas para compatibilidade)
    // =============================

    public static class MarketCategory {
        public String id;
        public String name;
        public Material icon;
        public int slot;
        public List<MarketItem> items = new ArrayList<>();

        public MarketCategory(String id, String name, Material icon, int slot) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.slot = slot;
        }
    }

    public static class MarketItem {
        public String id;
        public String name;
        public Material material;
        public double buyPrice;
        public double sellPrice;

        public MarketItem(String id, String name, Material material, double buyPrice, double sellPrice) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public double getSellPrice() {
            return sellPrice;
        }

        public Material getMaterial() {
            return material;
        }
    }

    public static class MarketListing {
        public String id;
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public double price;
        public long timestamp;

        public MarketListing(String id, UUID seller, String sellerName, ItemStack item, double price, long timestamp) {
            this.id = id;
            this.seller = seller;
            this.sellerName = sellerName;
            this.item = item;
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
