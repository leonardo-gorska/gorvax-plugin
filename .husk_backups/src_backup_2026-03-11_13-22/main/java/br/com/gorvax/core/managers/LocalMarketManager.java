package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.MarketManager.MarketListing;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lógica do mercado local P2P: listagem, compra, venda e cancelamento.
 */
public class LocalMarketManager {

    private final GorvaxCore plugin;
    private final Map<String, List<MarketListing>> localListings;
    private final MarketData data;
    private final MarketGUI gui;

    public LocalMarketManager(GorvaxCore plugin, Map<String, List<MarketListing>> localListings,
            MarketData data, MarketGUI gui) {
        this.plugin = plugin;
        this.localListings = localListings;
        this.data = data;
        this.gui = gui;
    }

    // =============================
    // BUSCA
    // =============================

    public void startSearch(Player p, String cityId) {
        p.closeInventory();
        plugin.getInputManager().openAnvilInput(p, "Buscar Item", "", (text) -> {
            if (text == null || text.trim().isEmpty())
                return;
            Bukkit.getScheduler().runTask(plugin, () -> gui.openLocalMarket(p, cityId, 1, text, null));
        });
    }

    // =============================
    // VENDA
    // =============================

    public void startSelling(Player p, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage(plugin.getMessageManager().get("local_market.hold_item"));
            return;
        }

        p.closeInventory();
        plugin.getInputManager().openNumericInput(p, "Definir Preço", 0, (price) -> {
            completeSelling(p, item, null, price);
        });
    }

    public void prepareSelling(Player p, ItemStack item, String cityId) {
        if (item == null || item.getType() == Material.AIR)
            return;

        plugin.getInputManager().openNumericInput(p, "Definir Preço", 0, (price) -> {
            completeSelling(p, item, cityId, price);
        });
    }

    public void completeSelling(Player p, ItemStack item, String cityId, double price) {
        // Validação de preço com limites realistas
        if (price <= 0) {
            p.sendMessage(plugin.getMessageManager().get("local_market.invalid_price"));
            return;
        }

        if (price > 1_000_000_000) {
            p.sendMessage(plugin.getMessageManager().get("local_market.price_too_high"));
            return;
        }

        if (Double.isNaN(price) || Double.isInfinite(price)) {
            p.sendMessage(plugin.getMessageManager().get("local_market.price_invalid"));
            return;
        }

        // Recuperar CityID se null (da localização)
        if (cityId == null) {
            Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
            if (claim != null && claim.isKingdom()) {
                cityId = claim.getId();
            } else {
                p.sendMessage(plugin.getMessageManager().get("local_market.must_be_in_city"));
                return;
            }
        } else {
            Claim claim = plugin.getClaimManager().getClaimAt(p.getLocation());
            if (claim == null || !claim.getId().equals(cityId)) {
                p.sendMessage(plugin.getMessageManager().get("local_market.wrong_city"));
                return;
            }
        }

        // Verificar se jogador ainda tem o item
        if (!p.getInventory().containsAtLeast(item, item.getAmount())) {
            p.sendMessage(plugin.getMessageManager().get("local_market.item_not_in_inventory"));
            if (cityId != null)
                gui.openLocalMarket(p, cityId, 1, null, null);
            return;
        }

        // Remover item com segurança
        HashMap<Integer, ItemStack> leftover = p.getInventory().removeItem(item);
        if (!leftover.isEmpty()) {
            p.sendMessage(plugin.getMessageManager().get("local_market.remove_error"));
            int removedAmount = item.getAmount();
            for (ItemStack left : leftover.values())
                removedAmount -= left.getAmount();

            if (removedAmount > 0) {
                ItemStack refund = item.clone();
                refund.setAmount(removedAmount);
                p.getInventory().addItem(refund);
            }
            return;
        }

        MarketListing listing = new MarketListing(
                UUID.randomUUID().toString(),
                p.getUniqueId(),
                p.getName(),
                item.clone(),
                price,
                System.currentTimeMillis());

        synchronized (localListings) {
            localListings.computeIfAbsent(cityId, k -> new CopyOnWriteArrayList<>()).add(listing);
        }

        // SAVE SYNC para prevenir duplicação de item
        data.saveLocalMarketSync();

        p.sendMessage(plugin.getMessageManager().get("local_market.sell_success", String.format("%.2f", price)));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // B10 — Log de auditoria
        if (plugin.getAuditManager() != null) {
            String auditItemName = (item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName())
                    : item.getType().name());
            plugin.getAuditManager().log(
                    AuditManager.AuditAction.MARKET_SELL,
                    p.getUniqueId(), p.getName(),
                    auditItemName + " x" + item.getAmount() + " por $" + String.format("%.2f", price),
                    price);
        }

        gui.openLocalMarket(p, cityId, 1, null, null);
    }

    // =============================
    // COMPRA
    // =============================

    public void buyListing(Player buyer, String cityId, String listingId) {
        synchronized (localListings) {
            List<MarketListing> listings = localListings.get(cityId);
            if (listings == null) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.no_items"));
                return;
            }

            MarketListing target = null;
            for (MarketListing l : listings) {
                if (l.id.equals(listingId)) {
                    target = l;
                    break;
                }
            }

            if (target == null) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.item_unavailable"));
                gui.openLocalMarket(buyer, cityId, 1, null, null);
                return;
            }

            // Proibir comprar próprio item
            if (target.seller.equals(buyer.getUniqueId())) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.cannot_buy_own"));
                return;
            }

            // Validar item
            if (target.item == null || target.item.getType() == Material.AIR) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.invalid_item"));
                listings.remove(target);
                data.saveLocalMarketSync();
                return;
            }

            // Verificar economia
            if (GorvaxCore.getEconomy().getBalance(buyer) < target.price) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.insufficient_funds",
                        String.format("%.2f", target.price - GorvaxCore.getEconomy().getBalance(buyer))));
                return;
            }

            // Verificar inventário
            if (buyer.getInventory().firstEmpty() == -1) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.inventory_full"));
                return;
            }

            // TRANSAÇÃO ATÔMICA
            net.milkbowl.vault.economy.EconomyResponse response = GorvaxCore.getEconomy().withdrawPlayer(buyer,
                    target.price);
            if (!response.transactionSuccess()) {
                buyer.sendMessage(
                        plugin.getMessageManager().get("local_market.transaction_error", response.errorMessage));
                return;
            }

            // Remover da listagem ANTES de pagar o vendedor
            listings.remove(target);
            // CRITICAL: Force Sync Save com Rollback
            if (!data.saveLocalMarketSync()) {
                listings.add(target);
                GorvaxCore.getEconomy().depositPlayer(buyer, target.price);
                buyer.sendMessage(plugin.getMessageManager().get("local_market.critical_error"));
                return;
            }

            // Lógica de taxa
            Claim cityClaim = plugin.getClaimManager().getClaimById(cityId);
            double taxRate = cityClaim != null ? cityClaim.getTax() : 5.0;
            double taxAmount = (target.price * taxRate) / 100.0;
            double sellerAmount = target.price - taxAmount;

            // Pagar vendedor e cidade
            GorvaxCore.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(target.seller), sellerAmount);
            if (cityClaim != null && cityClaim.getOwner() != null) {
                GorvaxCore.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(cityClaim.getOwner()), taxAmount);
            }

            // Dar item ao comprador
            HashMap<Integer, ItemStack> leftBuy = buyer.getInventory().addItem(target.item);
            if (!leftBuy.isEmpty()) {
                buyer.sendMessage(plugin.getMessageManager().get("local_market.item_dropped"));
                for (ItemStack drop : leftBuy.values()) {
                    buyer.getWorld().dropItem(buyer.getLocation(), drop);
                }
            }

            // Notificar
            String itemName = (target.item.hasItemMeta() && target.item.getItemMeta().hasDisplayName()
                    ? LegacyComponentSerializer.legacySection().serialize(target.item.getItemMeta().displayName())
                    : target.item.getType().name());
            buyer.sendMessage(plugin.getMessageManager().get("local_market.buy_success", itemName,
                    String.format("%.2f", target.price)));
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

            // B10 — Log de auditoria
            if (plugin.getAuditManager() != null) {
                plugin.getAuditManager().log(
                        AuditManager.AuditAction.MARKET_BUY,
                        buyer.getUniqueId(), buyer.getName(),
                        itemName + " x" + target.item.getAmount() + " de " + target.sellerName,
                        target.price);
            }

            Player seller = Bukkit.getPlayer(target.seller);
            if (seller != null) {
                seller.sendMessage(
                        plugin.getMessageManager().get("local_market.item_sold", String.format("%.2f", target.price)));
                seller.sendMessage(plugin.getMessageManager().get("local_market.tax_info", taxRate,
                        String.format("%.2f", sellerAmount)));
            }

            gui.openLocalMarket(buyer, cityId, 1, null, null);
        }
    }

    // =============================
    // CANCELAMENTO
    // =============================

    public void cancelListing(Player p, String cityId, String listingId) {
        synchronized (localListings) {
            List<MarketListing> listings = localListings.get(cityId);
            if (listings == null) {
                p.sendMessage(plugin.getMessageManager().get("local_market.city_not_found"));
                return;
            }

            MarketListing target = null;
            for (MarketListing l : listings) {
                if (l.id.equals(listingId)) {
                    target = l;
                    break;
                }
            }

            if (target == null) {
                p.sendMessage(plugin.getMessageManager().get("local_market.listing_not_found"));
                gui.openMyListings(p, cityId);
                return;
            }

            if (!target.seller.equals(p.getUniqueId()) && !p.hasPermission("gorvax.admin")) {
                p.sendMessage(plugin.getMessageManager().get("local_market.cancel_own_only"));
                return;
            }

            // Devolver item
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(target.item);
            if (!left.isEmpty()) {
                p.sendMessage(plugin.getMessageManager().get("local_market.cancel_inv_full"));
                for (ItemStack drop : left.values()) {
                    p.getWorld().dropItem(p.getLocation(), drop);
                }
            } else {
                p.sendMessage(plugin.getMessageManager().get("local_market.cancel_success"));
            }

            listings.remove(target);
            data.saveLocalMarketSync();

            // B10 — Log de auditoria
            if (plugin.getAuditManager() != null) {
                String cancelItemName = (target.item.hasItemMeta() && target.item.getItemMeta().hasDisplayName()
                        ? LegacyComponentSerializer.legacySection().serialize(target.item.getItemMeta().displayName())
                        : target.item.getType().name());
                plugin.getAuditManager().log(
                        AuditManager.AuditAction.MARKET_CANCEL,
                        p.getUniqueId(), p.getName(),
                        cancelItemName + " x" + target.item.getAmount(),
                        target.price);
            }

            gui.openMyListings(p, cityId);
        }
    }

    public Map<String, List<MarketListing>> getLocalListings() {
        return localListings;
    }
}
