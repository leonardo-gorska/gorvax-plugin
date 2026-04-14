package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.DuelManager.ActiveDuel;
import br.com.gorvax.core.managers.DuelManager.DuelChallenge;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para DuelManager.
 * Testa inner classes (DuelChallenge, ActiveDuel) e lógica pura de estado.
 */
class DuelManagerTest {

    // --- DuelChallenge model ---

    @Test
    void duelChallengeCriacao() {
        UUID challenger = UUID.randomUUID();
        UUID challenged = UUID.randomUUID();
        DuelChallenge challenge = new DuelChallenge(challenger, challenged, 500.0, 99999L);

        assertEquals(challenger, challenge.challenger);
        assertEquals(challenged, challenge.challenged);
        assertEquals(500.0, challenge.betAmount, 0.001);
        assertEquals(99999L, challenge.expiresAt);
    }

    @Test
    void duelChallengeExpirado() {
        DuelChallenge challenge = new DuelChallenge(
                UUID.randomUUID(), UUID.randomUUID(), 100.0,
                System.currentTimeMillis() - 1000L);

        assertTrue(System.currentTimeMillis() >= challenge.expiresAt);
    }

    @Test
    void duelChallengeNaoExpirado() {
        DuelChallenge challenge = new DuelChallenge(
                UUID.randomUUID(), UUID.randomUUID(), 100.0,
                System.currentTimeMillis() + 30000L);

        assertFalse(System.currentTimeMillis() >= challenge.expiresAt);
    }

    // --- ActiveDuel model ---

    @Test
    void activeDuelCriacao() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        ActiveDuel duel = new ActiveDuel(p1, p2, 1000.0);

        assertEquals(p1, duel.player1);
        assertEquals(p2, duel.player2);
        assertEquals(1000.0, duel.betAmount, 0.001);
        assertEquals(-1, duel.taskId);
        assertEquals(-1, duel.timeoutTaskId);
        assertFalse(duel.started);
        assertEquals(0, duel.startedAt);
    }

    @Test
    void activeDuelIsParticipant() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        ActiveDuel duel = new ActiveDuel(p1, p2, 0);

        assertTrue(duel.isParticipant(p1));
        assertTrue(duel.isParticipant(p2));
        assertFalse(duel.isParticipant(UUID.randomUUID()));
    }

    @Test
    void activeDuelGetOpponent() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        ActiveDuel duel = new ActiveDuel(p1, p2, 0);

        assertEquals(p2, duel.getOpponent(p1));
        assertEquals(p1, duel.getOpponent(p2));
    }

    @Test
    void activeDuelState() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        ActiveDuel duel = new ActiveDuel(p1, p2, 500.0);

        assertFalse(duel.started);
        duel.started = true;
        duel.startedAt = System.currentTimeMillis();
        assertTrue(duel.started);
        assertTrue(duel.startedAt > 0);
    }

    @Test
    void activeDuelSemAposta() {
        ActiveDuel duel = new ActiveDuel(UUID.randomUUID(), UUID.randomUUID(), 0);
        assertEquals(0, duel.betAmount, 0.001);
    }

    // --- Lógica de prêmio (replicada) ---

    @Test
    void duelPremioComTaxa() {
        double betAmount = 1000.0;
        double taxPercent = 5.0;
        double totalPot = betAmount * 2; // 2000
        double tax = totalPot * (taxPercent / 100.0); // 100
        double prize = totalPot - tax; // 1900

        assertEquals(2000.0, totalPot, 0.001);
        assertEquals(100.0, tax, 0.001);
        assertEquals(1900.0, prize, 0.001);
    }

    @Test
    void duelPremioSemAposta() {
        double betAmount = 0;
        double totalPot = betAmount * 2;
        assertEquals(0, totalPot, 0.001);
    }

    @Test
    void duelPremioComTaxaAlta() {
        double betAmount = 500.0;
        double taxPercent = 10.0;
        double totalPot = betAmount * 2;
        double tax = totalPot * (taxPercent / 100.0);
        double prize = totalPot - tax;

        assertEquals(1000.0, totalPot, 0.001);
        assertEquals(100.0, tax, 0.001);
        assertEquals(900.0, prize, 0.001);
    }
}
