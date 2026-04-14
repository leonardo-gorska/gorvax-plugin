package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para VoteManager.
 * Testa KingdomVote inner class e lógica de votação replicada.
 */
class VoteManagerTest {

    // --- KingdomVote model ---

    @Test
    void kingdomVoteFields() {
        UUID creator = UUID.randomUUID();
        long now = System.currentTimeMillis();
        long expires = now + 86400000L;
        var vote = new VoteManager.KingdomVote("reino1", "Mudar capital?", creator, now, expires);

        assertEquals("reino1", vote.kingdomId);
        assertEquals("Mudar capital?", vote.question);
        assertEquals(creator, vote.creatorUUID);
        assertEquals(now, vote.createdAt);
        assertEquals(expires, vote.expiresAt);
        assertTrue(vote.votes.isEmpty());
    }

    @Test
    void kingdomVoteCounters() {
        long now = System.currentTimeMillis();
        var vote = new VoteManager.KingdomVote("r1", "Q?", UUID.randomUUID(), now, now + 86400000L);

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
    void kingdomVoteHasVoted() {
        long now = System.currentTimeMillis();
        var vote = new VoteManager.KingdomVote("r1", "Q?", UUID.randomUUID(), now, now + 86400000L);

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        vote.votes.put(p1, true);

        assertTrue(vote.hasVoted(p1));
        assertFalse(vote.hasVoted(p2));
    }

    @Test
    void kingdomVoteIsExpired() {
        long now = System.currentTimeMillis();
        var active = new VoteManager.KingdomVote("r1", "Q?", UUID.randomUUID(), now, now + 86400000L);
        assertFalse(active.isExpired());

        var expired = new VoteManager.KingdomVote("r2", "Q?", UUID.randomUUID(), now - 100000L, now - 1);
        assertTrue(expired.isExpired());
    }

    @Test
    void kingdomVoteGetRemainingTime() {
        long now = System.currentTimeMillis();

        // Expirada
        var expired = new VoteManager.KingdomVote("r1", "Q?", UUID.randomUUID(), now - 100000L, now - 1);
        assertEquals("Expirada", expired.getRemainingTime());

        // 2h30 restantes
        var active = new VoteManager.KingdomVote("r2", "Q?", UUID.randomUUID(), now,
                now + (2 * 3600000L) + (30 * 60000L));
        String remaining = active.getRemainingTime();
        assertTrue(remaining.contains("h"), "Deve conter 'h': " + remaining);
        assertTrue(remaining.contains("m"), "Deve conter 'm': " + remaining);

        // 15 minutos
        var shortVote = new VoteManager.KingdomVote("r3", "Q?", UUID.randomUUID(), now,
                now + (15 * 60000L));
        String shortRemaining = shortVote.getRemainingTime();
        assertFalse(shortRemaining.contains("h"), "Não deve conter 'h': " + shortRemaining);
        assertTrue(shortRemaining.contains("m"), "Deve conter 'm': " + shortRemaining);
    }

    // --- Lógica de votação replicada ---

    private Map<String, VoteManager.KingdomVote> activeVotes;

    @BeforeEach
    void setUp() {
        activeVotes = new ConcurrentHashMap<>();
    }

    private boolean createVote(String kingdomId, String question, UUID creatorUUID) {
        if (activeVotes.containsKey(kingdomId))
            return false;
        long now = System.currentTimeMillis();
        long expiresAt = now + (24 * 3600L * 1000L);
        var vote = new VoteManager.KingdomVote(kingdomId, question, creatorUUID, now, expiresAt);
        activeVotes.put(kingdomId, vote);
        return true;
    }

    private int castVote(String kingdomId, UUID playerUUID, boolean voteYes) {
        VoteManager.KingdomVote vote = activeVotes.get(kingdomId);
        if (vote == null)
            return 1;
        if (vote.isExpired()) {
            activeVotes.remove(kingdomId);
            return 3;
        }
        if (vote.hasVoted(playerUUID))
            return 2;
        vote.votes.put(playerUUID, voteYes);
        return 0;
    }

    private VoteManager.KingdomVote getActiveVote(String kingdomId) {
        VoteManager.KingdomVote vote = activeVotes.get(kingdomId);
        if (vote != null && vote.isExpired()) {
            activeVotes.remove(kingdomId);
            return null;
        }
        return vote;
    }

    private boolean cancelVote(String kingdomId) {
        return activeVotes.remove(kingdomId) != null;
    }

    @Test
    void createVoteSucesso() {
        assertTrue(createVote("reino1", "Expandir?", UUID.randomUUID()));
        assertNotNull(activeVotes.get("reino1"));
    }

    @Test
    void createVoteDuplicata() {
        createVote("reino1", "Q1", UUID.randomUUID());
        assertFalse(createVote("reino1", "Q2", UUID.randomUUID()));
    }

    @Test
    void castVoteSucesso() {
        createVote("reino1", "Q?", UUID.randomUUID());
        UUID voter = UUID.randomUUID();
        assertEquals(0, castVote("reino1", voter, true));
        assertTrue(activeVotes.get("reino1").hasVoted(voter));
    }

    @Test
    void castVoteSemVotacaoAtiva() {
        assertEquals(1, castVote("inexistente", UUID.randomUUID(), true));
    }

    @Test
    void castVoteJaVotou() {
        createVote("reino1", "Q?", UUID.randomUUID());
        UUID voter = UUID.randomUUID();
        castVote("reino1", voter, true);
        assertEquals(2, castVote("reino1", voter, false));
    }

    @Test
    void castVoteExpirada() {
        long now = System.currentTimeMillis();
        var expired = new VoteManager.KingdomVote("reino1", "Q?", UUID.randomUUID(), now - 100000L, now - 1);
        activeVotes.put("reino1", expired);

        assertEquals(3, castVote("reino1", UUID.randomUUID(), true));
        assertNull(activeVotes.get("reino1")); // Deve ter removido
    }

    @Test
    void getActiveVoteExistente() {
        createVote("reino1", "Q?", UUID.randomUUID());
        assertNotNull(getActiveVote("reino1"));
    }

    @Test
    void getActiveVoteExpiradaAutoRemove() {
        long now = System.currentTimeMillis();
        var expired = new VoteManager.KingdomVote("reino1", "Q?", UUID.randomUUID(), now - 100000L, now - 1);
        activeVotes.put("reino1", expired);

        assertNull(getActiveVote("reino1"));
        assertFalse(activeVotes.containsKey("reino1"));
    }

    @Test
    void cancelVoteExistente() {
        createVote("reino1", "Q?", UUID.randomUUID());
        assertTrue(cancelVote("reino1"));
        assertFalse(activeVotes.containsKey("reino1"));
    }

    @Test
    void cancelVoteInexistente() {
        assertFalse(cancelVote("inexistente"));
    }

    @Test
    void multipleKingdomsIndependent() {
        createVote("reino1", "Q1?", UUID.randomUUID());
        createVote("reino2", "Q2?", UUID.randomUUID());

        UUID voter = UUID.randomUUID();
        assertEquals(0, castVote("reino1", voter, true));
        assertEquals(0, castVote("reino2", voter, false));

        assertEquals(1, activeVotes.get("reino1").getYesCount());
        assertEquals(1, activeVotes.get("reino2").getNoCount());
    }
}
