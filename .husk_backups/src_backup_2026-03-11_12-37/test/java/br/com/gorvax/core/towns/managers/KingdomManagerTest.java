package br.com.gorvax.core.towns.managers;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para KingdomManager.
 * Testa lógica pura de rank por súditos, bônus de XP, e records.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class KingdomManagerTest {

    // --- getKingdomRank (lógica replicada) ---

    private String getKingdomRank(int suditos) {
        if (suditos >= 50)
            return "§6§lImpério";
        if (suditos >= 20)
            return "§e§lReino";
        if (suditos >= 10)
            return "§b§lVila";
        return "§7Acampamento";
    }

    @Test
    void rankAcampamentoComZeroSuditos() {
        assertEquals("§7Acampamento", getKingdomRank(0));
    }

    @Test
    void rankAcampamentoComUmSudito() {
        assertEquals("§7Acampamento", getKingdomRank(1));
    }

    @Test
    void rankAcampamentoComNoveSuditos() {
        assertEquals("§7Acampamento", getKingdomRank(9));
    }

    @Test
    void rankVilaComDezSuditos() {
        assertEquals("§b§lVila", getKingdomRank(10));
    }

    @Test
    void rankVilaComDezenoveSuditos() {
        assertEquals("§b§lVila", getKingdomRank(19));
    }

    @Test
    void rankReinoComVinteSuditos() {
        assertEquals("§e§lReino", getKingdomRank(20));
    }

    @Test
    void rankReinoComQuarentaENoveSuditos() {
        assertEquals("§e§lReino", getKingdomRank(49));
    }

    @Test
    void rankImperioComCinquentaSuditos() {
        assertEquals("§6§lImpério", getKingdomRank(50));
    }

    @Test
    void rankImperioComCemSuditos() {
        assertEquals("§6§lImpério", getKingdomRank(100));
    }

    // --- getPassiveXpBonus (lógica replicada) ---

    private double getPassiveXpBonus(int suditos) {
        return (suditos / 5) * 0.02;
    }

    @Test
    void xpBonusZeroSuditos() {
        assertEquals(0.0, getPassiveXpBonus(0), 0.001);
    }

    @Test
    void xpBonusQuatroSuditos() {
        assertEquals(0.0, getPassiveXpBonus(4), 0.001);
    }

    @Test
    void xpBonusCincoSuditos() {
        assertEquals(0.02, getPassiveXpBonus(5), 0.001);
    }

    @Test
    void xpBonusDezSuditos() {
        assertEquals(0.04, getPassiveXpBonus(10), 0.001);
    }

    @Test
    void xpBonusCinquentaSuditos() {
        assertEquals(0.20, getPassiveXpBonus(50), 0.001);
    }

    // --- Invite record ---

    @Test
    void inviteRecordCampos() {
        UUID inviter = UUID.randomUUID();
        long now = System.currentTimeMillis();
        var invite = new KingdomManager.Invite("kingdom1", inviter, now);
        assertEquals("kingdom1", invite.kingdomId());
        assertEquals(inviter, invite.inviter());
        assertEquals(now, invite.timestamp());
    }

    @Test
    void inviteRecordEquality() {
        UUID inviter = UUID.randomUUID();
        long now = 1000L;
        var a = new KingdomManager.Invite("k1", inviter, now);
        var b = new KingdomManager.Invite("k1", inviter, now);
        assertEquals(a, b);
    }

    @Test
    void inviteRecordInequalityDiferenteReino() {
        UUID inviter = UUID.randomUUID();
        var a = new KingdomManager.Invite("k1", inviter, 1000L);
        var b = new KingdomManager.Invite("k2", inviter, 1000L);
        assertNotEquals(a, b);
    }

    // --- AllianceProposal record ---

    @Test
    void allianceProposalCampos() {
        long now = System.currentTimeMillis();
        var proposal = new KingdomManager.AllianceProposal("from", "to", now);
        assertEquals("from", proposal.fromKingdomId());
        assertEquals("to", proposal.toKingdomId());
        assertEquals(now, proposal.timestamp());
    }

    @Test
    void allianceProposalEquality() {
        var a = new KingdomManager.AllianceProposal("f", "t", 500L);
        var b = new KingdomManager.AllianceProposal("f", "t", 500L);
        assertEquals(a, b);
    }
}
