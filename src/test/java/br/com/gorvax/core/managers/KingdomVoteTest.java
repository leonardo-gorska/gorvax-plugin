package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.VoteManager.KingdomVote;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para VoteManager.KingdomVote (inner class).
 * Lógica pura — sem mocks necessários.
 */
class KingdomVoteTest {

    private KingdomVote criarVote(long expiresAt) {
        return new KingdomVote("kingdom-1", "Devemos expandir?",
                UUID.randomUUID(), System.currentTimeMillis(), expiresAt);
    }

    // --- Contagem de votos ---

    @Test
    void contagemDeVotos() {
        KingdomVote vote = criarVote(Long.MAX_VALUE);

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        vote.votes.put(p1, true);
        vote.votes.put(p2, false);
        vote.votes.put(p3, true);

        assertEquals(2, vote.getYesCount());
        assertEquals(1, vote.getNoCount());
        assertEquals(3, vote.getTotalVotes());
    }

    @Test
    void semVotosZerado() {
        KingdomVote vote = criarVote(Long.MAX_VALUE);

        assertEquals(0, vote.getYesCount());
        assertEquals(0, vote.getNoCount());
        assertEquals(0, vote.getTotalVotes());
    }

    // --- hasVoted ---

    @Test
    void hasVotedAnteEDepois() {
        KingdomVote vote = criarVote(Long.MAX_VALUE);
        UUID player = UUID.randomUUID();

        assertFalse(vote.hasVoted(player));

        vote.votes.put(player, true);
        assertTrue(vote.hasVoted(player));
    }

    // --- isExpired ---

    @Test
    void isExpiredPassado() {
        KingdomVote vote = criarVote(System.currentTimeMillis() - 1000);
        assertTrue(vote.isExpired());
    }

    @Test
    void isNotExpiredFuturo() {
        KingdomVote vote = criarVote(System.currentTimeMillis() + 3600_000L);
        assertFalse(vote.isExpired());
    }

    // --- getRemainingTime ---

    @Test
    void remainingTimeExpirada() {
        KingdomVote vote = criarVote(System.currentTimeMillis() - 1000);
        assertEquals("Expirada", vote.getRemainingTime());
    }

    @Test
    void remainingTimeFormatoMinutos() {
        // 30 minutos no futuro
        KingdomVote vote = criarVote(System.currentTimeMillis() + 30 * 60 * 1000L);
        String remaining = vote.getRemainingTime();
        // Deve ter formato "Xm" (sem horas)
        assertTrue(remaining.endsWith("m"), "Esperava formato de minutos, got: " + remaining);
        assertFalse(remaining.contains("h"), "Não deveria ter horas, got: " + remaining);
    }

    @Test
    void remainingTimeFormatoHoras() {
        // 2h30 no futuro
        KingdomVote vote = criarVote(System.currentTimeMillis() + (2 * 3600_000L + 30 * 60_000L));
        String remaining = vote.getRemainingTime();
        assertTrue(remaining.contains("h"), "Esperava horas, got: " + remaining);
        assertTrue(remaining.contains("m"), "Esperava minutos, got: " + remaining);
    }
}
