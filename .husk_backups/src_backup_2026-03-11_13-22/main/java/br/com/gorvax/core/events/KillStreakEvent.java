package br.com.gorvax.core.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando o kill streak de um jogador é incrementado.
 */
public class KillStreakEvent extends GorvaxEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int streak;

    public KillStreakEvent(Player player, int streak) {
        this.player = player;
        this.streak = streak;
    }

    /** O jogador que teve o streak incrementado. */
    public Player getPlayer() {
        return player;
    }

    /** Valor atual do kill streak. */
    public int getStreak() {
        return streak;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
