package br.com.gorvax.core.events;

import br.com.gorvax.core.managers.Claim;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * B19 — Disparado quando um claim está prestes a ser criado.
 * Cancelável: se cancelado, o claim não será registrado.
 */
public class ClaimCreateEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID owner;
    private final Claim claim;

    public ClaimCreateEvent(UUID owner, Claim claim) {
        this.owner = owner;
        this.claim = claim;
    }

    /** UUID do dono do claim. */
    public UUID getOwner() {
        return owner;
    }

    /** O claim sendo criado. */
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
