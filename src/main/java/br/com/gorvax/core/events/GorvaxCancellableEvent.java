package br.com.gorvax.core.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * B19 — Classe base para eventos canceláveis do GorvaxCore.
 * Se cancelado, a ação associada não será executada.
 */
public abstract class GorvaxCancellableEvent extends GorvaxEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    public GorvaxCancellableEvent() {
        super();
    }

    public GorvaxCancellableEvent(boolean isAsync) {
        super(isAsync);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
