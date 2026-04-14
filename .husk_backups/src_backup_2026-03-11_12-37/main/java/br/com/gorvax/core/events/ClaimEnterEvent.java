package br.com.gorvax.core.events;

import br.com.gorvax.core.managers.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando um jogador entra em um claim.
 * Cancelável: se cancelado, o movimento é impedido (o jogador não entra).
 */
public class ClaimEnterEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Claim claim;

    public ClaimEnterEvent(Player player, Claim claim) {
        this.player = player;
        this.claim = claim;
    }

    /** O jogador que está entrando no claim. */
    public Player getPlayer() {
        return player;
    }

    /** O claim sendo acessado. */
    public Claim getClaim() {
        return claim;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
