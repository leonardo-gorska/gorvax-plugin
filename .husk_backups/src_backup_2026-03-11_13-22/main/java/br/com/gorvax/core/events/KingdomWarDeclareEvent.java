package br.com.gorvax.core.events;

import org.bukkit.event.HandlerList;

/**
 * B19 — Disparado quando um reino declara guerra contra outro.
 * Cancelável: se cancelado, a declaração de guerra é impedida.
 */
public class KingdomWarDeclareEvent extends GorvaxCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String attackerKingdomId;
    private final String defenderKingdomId;

    public KingdomWarDeclareEvent(String attackerKingdomId, String defenderKingdomId) {
        this.attackerKingdomId = attackerKingdomId;
        this.defenderKingdomId = defenderKingdomId;
    }

    /** ID do reino atacante (que declarou guerra). */
    public String getAttackerKingdomId() {
        return attackerKingdomId;
    }

    /** ID do reino defensor (alvo da guerra). */
    public String getDefenderKingdomId() {
        return defenderKingdomId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
