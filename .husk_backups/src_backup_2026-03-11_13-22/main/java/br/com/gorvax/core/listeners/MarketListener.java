package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.MarketManager;
import br.com.gorvax.core.managers.MarketManager.MarketCategory;
import br.com.gorvax.core.managers.MarketManager.MarketItem;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import br.com.gorvax.core.managers.MarketHolder;
import br.com.gorvax.core.managers.MarketHolder.MarketType;
import br.com.gorvax.core.managers.AuctionGUI;
import org.bukkit.inventory.Inventory;

public class MarketListener implements Listener {

    private final GorvaxCore plugin;

    public MarketListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // Old handler removed - merged logic is at the bottom of the file in the new
    // handler.
    // Or simpler: Leave this spot empty and ensure the one at the bottom handles
    // BOTH cases?
    // Actually, let's keep the one at the bottom and delete this one.
    // BUT, I need to make sure the one at the bottom handes BOTH "Selling Price
    // Input" and "Search Input".
    // Checking file content...
    // Line 31-38 handles "isSelling" (setting price).
    // The new one handles "searchingPlayers".
    // A player can't be doing both. So I can merge them.

    // Deleting this block.

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;

        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof MarketHolder holder) {
            e.setCancelled(true);

            if (e.getClickedInventory() != inv) {
                if (holder.getType() == MarketType.SELL_SELECTION) {
                    ItemStack clicked = e.getCurrentItem();
                    if (clicked != null && clicked.getType() != Material.AIR) {
                        plugin.getMarketManager().prepareSelling(p, clicked, holder.getCityId());
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                }
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            handleMarketClick(p, holder, clicked, e.getSlot(), e.getClick(), e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MarketHolder) {
            e.setCancelled(true);
        }
    }

    private void handleMarketClick(Player p, MarketHolder holder, ItemStack clicked, int slot, ClickType click,
            InventoryClickEvent e) {
        MarketManager manager = plugin.getMarketManager();
        String cityId = holder.getCityId();

        switch (holder.getType()) {
            case AUCTION_LIST, AUCTION_MY:
                AuctionGUI.handleClick(plugin, p, clicked, holder.getType().name());
                return;

            case GLOBAL:
                for (MarketCategory cat : manager.getCategories().values()) {
                    if (clicked.getType() == cat.icon && clicked.hasItemMeta()
                            && clicked.getItemMeta().hasDisplayName()
                            && LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName())
                                    .equals(cat.name)) {
                        manager.openCategory(p, cat.id);
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        return;
                    }
                }
                break;

            case CATEGORY:
                if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()
                        && clicked.getItemMeta().hasDisplayName()
                        && LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName())
                                .equals("§cVoltar")) {
                    manager.openGlobalMarket(p);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    return;
                }
                String catId = holder.getFilter();
                MarketCategory cat = manager.getCategories().get(catId);
                if (cat != null) {
                    for (MarketItem item : cat.items) {
                        if (clicked.getType() == item.getMaterial()) {
                            handleTransaction(p, item, click);
                            return;
                        }
                    }
                }
                break;

            case SHOPS_MENU:
                if (slot == 4 && clicked.getType() == Material.NETHER_STAR) {
                    manager.openLocalMarket(p, cityId, 1);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                } else if (slot == 49 && clicked.getType() == Material.EMERALD) {
                    manager.startSelling(p, p.getInventory().getItemInMainHand());
                } else if (slot == 53 && clicked.getType() == Material.ENDER_CHEST) {
                    manager.openMyListings(p, cityId);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                } else {
                    NamespacedKey key = new NamespacedKey(plugin, "market_seller_id");
                    if (clicked.hasItemMeta()
                            && clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String sellerUuidStr = clicked.getItemMeta().getPersistentDataContainer().get(key,
                                PersistentDataType.STRING);
                        manager.openLocalMarket(p, cityId, 1, null, UUID.fromString(sellerUuidStr));
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                }
                break;

            case LOCAL_LISTINGS:
                if (slot == 49 && clicked.getType() == Material.EMERALD) {
                    manager.startSelling(p, p.getInventory().getItemInMainHand());
                } else if (slot == 50 && clicked.getType() == Material.ENDER_CHEST) {
                    manager.openMyListings(p, cityId);
                } else if (slot == 48 && clicked.getType() == Material.OAK_SIGN) {
                    manager.startSearch(p, cityId);
                } else if (slot == 45 && clicked.getType() == Material.ARROW) {
                    String displayName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                            ? LegacyComponentSerializer.legacySection().serialize(clicked.getItemMeta().displayName())
                            : "";
                    if (displayName.equals("§cVoltar para Lojas")) {
                        manager.openPlayerShopsMenu(p, cityId);
                    } else if (displayName.equals("§cLimpar Busca")) {
                        manager.openLocalMarket(p, cityId, 1);
                    } else {
                        int page = holder.getPage();
                        if (page > 1) {
                            manager.openLocalMarket(p, cityId, page - 1, holder.getFilter(), holder.getSellerFilter());
                        }
                    }
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                } else if (slot == 53 && clicked.getType() == Material.ARROW) {
                    int page = holder.getPage();
                    manager.openLocalMarket(p, cityId, page + 1, holder.getFilter(), holder.getSellerFilter());
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                } else {
                    NamespacedKey key = new NamespacedKey(plugin, "market_listing_id");
                    if (clicked.hasItemMeta()
                            && clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String listingId = clicked.getItemMeta().getPersistentDataContainer().get(key,
                                PersistentDataType.STRING);
                        manager.buyListing(p, cityId, listingId);
                    }
                }
                break;

            case MY_LISTINGS:
                if (slot == 49) {
                    manager.openLocalMarket(p, cityId, 1);
                } else {
                    NamespacedKey key = new NamespacedKey(plugin, "market_listing_id");
                    if (clicked.hasItemMeta()
                            && clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String listingId = clicked.getItemMeta().getPersistentDataContainer().get(key,
                                PersistentDataType.STRING);
                        manager.cancelListing(p, cityId, listingId);
                    }
                }
                break;

            case SELL_SELECTION:
                if (slot == 49) {
                    manager.openPlayerShopsMenu(p, cityId);
                }
                break;
        }
    }

    private void handleTransaction(Player p, MarketItem item, ClickType click) {
        if (GorvaxCore.getEconomy() == null) {
            p.sendMessage(plugin.getMessageManager().get("market.error_no_economy"));
            return;
        }

        MarketManager manager = plugin.getMarketManager();

        if (click.isLeftClick()) {
            // BUY
            double price = manager.getPrice(item, true);
            if (price <= 0) {
                p.sendMessage(plugin.getMessageManager().get("market.error_not_for_sale"));
                return;
            }

            if (!GorvaxCore.getEconomy().has(p, price)) {
                p.sendMessage(plugin.getMessageManager().get("market.error_insufficient_funds",
                        String.format("%.2f", price)));
                return;
            }

            if (p.getInventory().firstEmpty() == -1) {
                p.sendMessage(plugin.getMessageManager().get("market.error_inventory_full"));
                return;
            }

            EconomyResponse r = GorvaxCore.getEconomy().withdrawPlayer(p, price);
            if (r.transactionSuccess()) {
                p.getInventory().addItem(new ItemStack(item.getMaterial()));
                p.sendMessage(
                        plugin.getMessageManager().get("market.buy_success", item.name, String.format("%.2f", price)));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                // Increase Demand (Buy = Demand Up)
                manager.updateDemand(item.id, 1);

                // Re-open GUI to update prices visually
                // Note: Re-opening inv immediately can be annoying if spam clicking,
                // but needed to show price change. Maybe only update item lore?
                // For MVP, reopening checks category.
                // manager.openCategory(p, findCategoryId(item)); -> Hard to find category
                // without map.
                // Let's just update the lore if possible or leave it.
                // Ideally we refresh the inventory.
                refreshGui(p, item);

            } else {
                p.sendMessage(plugin.getMessageManager().get("market.error_transaction", r.errorMessage));
            }
        } else if (click.isRightClick()) {
            // SELL
            double price = manager.getPrice(item, false);
            if (price <= 0) {
                p.sendMessage(plugin.getMessageManager().get("market.error_cannot_sell"));
                return;
            }

            if (!hasSufficientVanillaItems(p, item.getMaterial(), 1)) {
                p.sendMessage(plugin.getMessageManager().get("market.error_no_vanilla_item"));
                return;
            }

            // BUG-01 FIX: Remover item ANTES de depositar para prevenir duplicação
            removeVanillaItems(p, item.getMaterial(), 1);
            EconomyResponse r = GorvaxCore.getEconomy().depositPlayer(p, price);
            if (r.transactionSuccess()) {
                p.sendMessage(
                        plugin.getMessageManager().get("market.sell_success", item.name, String.format("%.2f", price)));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                // Decrease Demand (Sell = Demand Down)
                manager.updateDemand(item.id, -1);

                refreshGui(p, item);

            } else {
                // Rollback: devolver o item ao jogador
                p.getInventory().addItem(new ItemStack(item.getMaterial()));
                p.sendMessage(plugin.getMessageManager().get("market.error_transaction", r.errorMessage));
            }
        }
    }

    private void refreshGui(Player p, MarketItem item) {
        // Simple refresh: close and open same category
        for (MarketCategory cat : plugin.getMarketManager().getCategories().values()) {
            if (cat.items.contains(item)) {
                plugin.getMarketManager().openCategory(p, cat.id);
                break;
            }
        }
    }

    // BUG-10 FIX: Adicionado check de lore para evitar "lavagem" de itens de boss
    private boolean isVanillaItem(ItemStack is) {
        if (!is.hasItemMeta())
            return true;
        var meta = is.getItemMeta();
        return !meta.hasDisplayName()
                && meta.getEnchants().isEmpty()
                && !meta.hasLore();
    }

    private boolean hasSufficientVanillaItems(Player p, Material mat, int amount) {
        int count = 0;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == mat && isVanillaItem(is)) {
                count += is.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeVanillaItems(Player p, Material mat, int amount) {
        ItemStack[] contents = p.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == mat && isVanillaItem(is)) {
                int take = Math.min(remaining, is.getAmount());
                is.setAmount(is.getAmount() - take);
                remaining -= take;
                p.getInventory().setItem(i, is.getAmount() > 0 ? is : null);
            }
        }
    }

}
