package br.com.gorvax.core.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando uma Raid de bosses está prestes a iniciar.
 * Cancelável: se cancelado, a raid não será iniciada.
 */
public class RaidStartEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;

    public RaidStartEvent(Location location) {
        this.location = location;
    }

    /** Localização onde a raid será iniciada. */
    public Location getLocation() {
        return location;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
