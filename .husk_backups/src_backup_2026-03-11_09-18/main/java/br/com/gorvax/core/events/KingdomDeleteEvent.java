package br.com.gorvax.core.events;

import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * B19 — Disparado quando um reino é deletado/dissolvido.
 * Não cancelável (a deleção já foi confirmada pelo jogador).
 */
public class KingdomDeleteEvent extends GorvaxEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String kingdomId;
    private final String kingdomName;
    private final UUID kingUUID;

    public KingdomDeleteEvent(String kingdomId, String kingdomName, UUID kingUUID) {
        this.kingdomId = kingdomId;
        this.kingdomName = kingdomName;
        this.kingUUID = kingUUID;
    }

    /** ID único do reino sendo deletado. */
    public String getKingdomId() {
        return kingdomId;
    }

    /** Nome do reino sendo deletado. */
    public String getKingdomName() {
        return kingdomName;
    }

    /** UUID do Rei (líder) do reino. Pode ser null se o rei era desconhecido. */
    public UUID getKingUUID() {
        return kingUUID;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
