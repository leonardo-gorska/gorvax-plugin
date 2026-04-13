package br.com.gorvax.core.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * B19 — Disparado quando um World Boss morre e recompensas estão sendo
 * distribuídas.
 */
public class BossDeathEvent extends GorvaxEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String bossId;
    private final Location deathLocation;
    private final List<Map.Entry<UUID, Double>> topDamagers;

    public BossDeathEvent(String bossId, Location deathLocation, List<Map.Entry<UUID, Double>> topDamagers) {
        this.bossId = bossId;
        this.deathLocation = deathLocation;
        this.topDamagers = topDamagers;
    }

    /** ID do boss que morreu. */
    public String getBossId() {
        return bossId;
    }

    /** Localização da morte do boss. */
    public Location getDeathLocation() {
        return deathLocation;
    }

    /** Lista ordenada dos jogadores que mais causaram dano (UUID → dano). */
    public List<Map.Entry<UUID, Double>> getTopDamagers() {
        return topDamagers;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
