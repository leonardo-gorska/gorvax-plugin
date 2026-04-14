package br.com.gorvax.core.events;

import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * B19 — Disparado quando um leilão é finalizado (com ou sem comprador).
 */
public class AuctionEndEvent extends GorvaxEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID seller;
    private final UUID winner;
    private final double finalPrice;
    private final ItemStack item;

    /**
     * @param seller     UUID do vendedor
     * @param winner     UUID do vencedor (null se ninguém comprou)
     * @param finalPrice Preço final do leilão
     * @param item       Item leiloado
     */
    public AuctionEndEvent(UUID seller, UUID winner, double finalPrice, ItemStack item) {
        this.seller = seller;
        this.winner = winner;
        this.finalPrice = finalPrice;
        this.item = item;
    }

    /** UUID do vendedor que criou o leilão. */
    public UUID getSeller() {
        return seller;
    }

    /** UUID do vencedor do leilão, ou null se expirou sem lances. */
    public UUID getWinner() {
        return winner;
    }

    /** Preço final do leilão. */
    public double getFinalPrice() {
        return finalPrice;
    }

    /** Item que foi leiloado. */
    public ItemStack getItem() {
        return item;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
