package br.com.gorvax.core.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando um novo reino é criado.
 * Cancelável: se cancelado, a criação do reino é impedida.
 */
public class KingdomCreateEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player founder;
    private final String kingdomId;
    private final String kingdomName;

    public KingdomCreateEvent(Player founder, String kingdomId, String kingdomName) {
        this.founder = founder;
        this.kingdomId = kingdomId;
        this.kingdomName = kingdomName;
    }

    /** O jogador que está fundando o reino. */
    public Player getFounder() {
        return founder;
    }

    /** ID único do reino (geralmente o ID do claim). */
    public String getKingdomId() {
        return kingdomId;
    }

    /** Nome escolhido para o reino. */
    public String getKingdomName() {
        return kingdomName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
