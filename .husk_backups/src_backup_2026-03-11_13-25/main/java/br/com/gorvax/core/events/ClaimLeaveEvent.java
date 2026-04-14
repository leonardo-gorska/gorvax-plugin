package br.com.gorvax.core.events;

import br.com.gorvax.core.managers.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando um jogador sai de um claim.
 * Não cancelável.
 */
public class ClaimLeaveEvent extends GorvaxEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Claim fromClaim;

    public ClaimLeaveEvent(Player player, Claim fromClaim) {
        this.player = player;
        this.fromClaim = fromClaim;
    }

    /** O jogador que está saindo do claim. */
    public Player getPlayer() {
        return player;
    }

    /** O claim que o jogador está deixando. */
    public Claim getFromClaim() {
        return fromClaim;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
