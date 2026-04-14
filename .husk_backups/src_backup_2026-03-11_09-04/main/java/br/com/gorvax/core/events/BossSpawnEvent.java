package br.com.gorvax.core.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando um World Boss está prestes a spawnar.
 * Cancelável: se cancelado, o boss não será spawnado.
 */
public class BossSpawnEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String bossId;
    private final Location location;

    public BossSpawnEvent(String bossId, Location location) {
        this.bossId = bossId;
        this.location = location;
    }

    /** ID do boss (ex: "rei_gorvax", "indrax_abissal"). */
    public String getBossId() {
        return bossId;
    }

    /** Localização onde o boss será spawnado. */
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
