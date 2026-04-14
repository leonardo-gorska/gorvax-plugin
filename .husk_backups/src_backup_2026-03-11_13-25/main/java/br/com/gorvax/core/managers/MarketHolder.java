package br.com.gorvax.core.managers;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MarketHolder implements InventoryHolder {

    private Inventory inventory;
    private final MarketType type;
    private final String cityId;
    private final int page;
    private final String filter; // Contexto extra para o MarketManager recuperar
    private final java.util.UUID sellerFilter;

    public MarketHolder(MarketType type, String cityId, int page, String filter, java.util.UUID sellerFilter) {
        this.type = type;
        this.cityId = cityId;
        this.page = page;
        this.filter = filter;
        this.sellerFilter = sellerFilter;
    }

    // B14 — Construtor com String para mapeamento flexível
    public MarketHolder(String typeName, String cityId, int page) {
        this(MarketType.valueOf(typeName.toUpperCase()), cityId, page, null, null);
    }

    public MarketHolder(MarketType type, String cityId, int page, String filter) {
        this(type, cityId, page, filter, null);
    }

    public MarketHolder(MarketType type, String cityId, int page) {
        this(type, cityId, page, null);
    }

    public MarketHolder(MarketType type) {
        this(type, null, 1, null);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public MarketType getType() {
        return type;
    }

    public String getCityId() {
        return cityId;
    }

    public int getPage() {
        return page;
    }

    public String getFilter() {
        return filter;
    }

    public java.util.UUID getSellerFilter() {
        return sellerFilter;
    }

    public enum MarketType {
        GLOBAL,
        CATEGORY,
        SHOPS_MENU,
        LOCAL_LISTINGS,
        MY_LISTINGS,
        SELL_SELECTION,
        AUCTION_LIST,
        AUCTION_MY
    }
}
