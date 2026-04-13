package br.com.gorvax.core.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * B19 — Classe base abstrata para todos os eventos customizados do GorvaxCore.
 * Plugins externos podem escutar estes eventos via @EventHandler.
 */
public abstract class GorvaxEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final long timestamp;

    public GorvaxEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public GorvaxEvent(boolean isAsync) {
        super(isAsync);
        this.timestamp = System.currentTimeMillis();
    }

    /** Timestamp (epoch ms) de quando o evento foi criado. */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
