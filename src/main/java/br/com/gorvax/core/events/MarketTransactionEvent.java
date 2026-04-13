package br.com.gorvax.core.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * B19 — Disparado quando uma transação de compra/venda ocorre no mercado.
 * Cancelável: se cancelado, a transação é impedida.
 */
public class MarketTransactionEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /** Tipo de transação no mercado. */
    public enum TransactionType {
        BUY, SELL
    }

    private final Player player;
    private final ItemStack item;
    private final double price;
    private final TransactionType type;

    public MarketTransactionEvent(Player player, ItemStack item, double price, TransactionType type) {
        this.player = player;
        this.item = item;
        this.price = price;
        this.type = type;
    }

    /** O jogador realizando a transação. */
    public Player getPlayer() {
        return player;
    }

    /** Item sendo comprado/vendido. */
    public ItemStack getItem() {
        return item;
    }

    /** Preço da transação. */
    public double getPrice() {
        return price;
    }

    /** Tipo: BUY ou SELL. */
    public TransactionType getTransactionType() {
        return type;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
