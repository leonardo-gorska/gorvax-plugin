package br.com.gorvax.core.events;

import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * B19 — Disparado quando um duelo termina (vitória ou empate).
 */
public class DuelEndEvent extends GorvaxEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID winner;
    private final UUID loser;
    private final double betAmount;
    private final boolean isDraw;

    /**
     * @param winner    UUID do vencedor (null se empate)
     * @param loser     UUID do perdedor (null se empate)
     * @param betAmount Valor da aposta (0 se sem aposta)
     * @param isDraw    true se o duelo terminou em empate
     */
    public DuelEndEvent(UUID winner, UUID loser, double betAmount, boolean isDraw) {
        this.winner = winner;
        this.loser = loser;
        this.betAmount = betAmount;
        this.isDraw = isDraw;
    }

    /** UUID do vencedor, ou null se empate. */
    public UUID getWinner() {
        return winner;
    }

    /** UUID do perdedor, ou null se empate. */
    public UUID getLoser() {
        return loser;
    }

    /** Valor da aposta em dinheiro. */
    public double getBetAmount() {
        return betAmount;
    }

    /** true se o duelo terminou em empate (timeout). */
    public boolean isDraw() {
        return isDraw;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
