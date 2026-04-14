package br.com.gorvax.core.towns;

/**
 * B7 — Relação diplomática entre dois reinos.
 */
public enum Relation {
    NEUTRAL,  // Padrão — regras normais de PvP/acesso
    ALLY,     // Aliados — PvP off, acesso trust, chat compartilhado
    ENEMY,    // Inimigos — PvP forçado, sem acesso
    TRUCE,    // Trégua temporária — PvP off, sem acesso extra
    WAR       // B15 — Guerra formal — PvP forçado, sistema de pontos, espólios
}
