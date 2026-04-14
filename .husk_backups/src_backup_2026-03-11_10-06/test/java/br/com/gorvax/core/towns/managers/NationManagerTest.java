package br.com.gorvax.core.towns.managers;

import br.com.gorvax.core.towns.managers.NationManager.Nation;
import br.com.gorvax.core.towns.managers.NationManager.NationInvite;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para NationManager.
 * Testa lógica pura do modelo Nation, NationInvite, e operações de
 * membros/banco.
 */
class NationManagerTest {

    // --- Nation model ---

    private Nation createNation(String id, String name, String founder, double bank) {
        return new Nation(id, name, founder, Set.of(founder), bank, System.currentTimeMillis());
    }

    @Test
    void nationGetters() {
        long now = 1000L;
        Nation n = new Nation("n1", "Império Solar", "k1", Set.of("k1"), 5000.0, now);
        assertEquals("n1", n.getId());
        assertEquals("Império Solar", n.getName());
        assertEquals("k1", n.getFounderKingdomId());
        assertEquals(5000.0, n.getBankBalance(), 0.001);
        assertEquals(now, n.getCreatedAt());
    }

    @Test
    void nationSetName() {
        Nation n = createNation("n1", "Antigo", "k1", 0);
        n.setName("Novo Nome");
        assertEquals("Novo Nome", n.getName());
    }

    @Test
    void nationSetFounderKingdomId() {
        Nation n = createNation("n1", "N", "k1", 0);
        n.setFounderKingdomId("k2");
        assertEquals("k2", n.getFounderKingdomId());
    }

    // --- Membros ---

    @Test
    void nationAddKingdom() {
        Nation n = createNation("n1", "N", "k1", 0);
        assertEquals(1, n.getKingdomCount());
        n.addKingdom("k2");
        assertEquals(2, n.getKingdomCount());
        assertTrue(n.hasKingdom("k2"));
    }

    @Test
    void nationAddKingdomDuplicado() {
        Nation n = createNation("n1", "N", "k1", 0);
        n.addKingdom("k1"); // já existe
        assertEquals(1, n.getKingdomCount());
    }

    @Test
    void nationRemoveKingdom() {
        Nation n = createNation("n1", "N", "k1", 0);
        n.addKingdom("k2");
        assertEquals(2, n.getKingdomCount());
        n.removeKingdom("k2");
        assertEquals(1, n.getKingdomCount());
        assertFalse(n.hasKingdom("k2"));
    }

    @Test
    void nationRemoveKingdomInexistente() {
        Nation n = createNation("n1", "N", "k1", 0);
        n.removeKingdom("k99"); // não existe
        assertEquals(1, n.getKingdomCount());
    }

    @Test
    void nationHasKingdom() {
        Nation n = createNation("n1", "N", "k1", 0);
        assertTrue(n.hasKingdom("k1"));
        assertFalse(n.hasKingdom("k2"));
    }

    @Test
    void nationGetMemberKingdomIds() {
        Nation n = createNation("n1", "N", "k1", 0);
        n.addKingdom("k2");
        Set<String> members = n.getMemberKingdomIds();
        assertTrue(members.contains("k1"));
        assertTrue(members.contains("k2"));
        assertEquals(2, members.size());
    }

    // --- Banco ---

    @Test
    void nationDepositBank() {
        Nation n = createNation("n1", "N", "k1", 1000);
        n.depositBank(500);
        assertEquals(1500.0, n.getBankBalance(), 0.001);
    }

    @Test
    void nationWithdrawBankSucesso() {
        Nation n = createNation("n1", "N", "k1", 1000);
        assertTrue(n.withdrawBank(500));
        assertEquals(500.0, n.getBankBalance(), 0.001);
    }

    @Test
    void nationWithdrawBankSaldoExato() {
        Nation n = createNation("n1", "N", "k1", 1000);
        assertTrue(n.withdrawBank(1000));
        assertEquals(0.0, n.getBankBalance(), 0.001);
    }

    @Test
    void nationWithdrawBankSaldoInsuficiente() {
        Nation n = createNation("n1", "N", "k1", 500);
        assertFalse(n.withdrawBank(600));
        assertEquals(500.0, n.getBankBalance(), 0.001);
    }

    // --- NationInvite ---

    @Test
    void nationInviteGetters() {
        long expire = System.currentTimeMillis() + 60000;
        NationInvite invite = new NationInvite("n1", "k2", expire);
        assertEquals("n1", invite.getNationId());
        assertEquals("k2", invite.getKingdomId());
    }

    @Test
    void nationInviteNaoExpirado() {
        long expire = System.currentTimeMillis() + 60000;
        NationInvite invite = new NationInvite("n1", "k2", expire);
        assertFalse(invite.isExpired());
        assertTrue(invite.getRemainingSeconds() > 0);
    }

    @Test
    void nationInviteExpirado() {
        long expire = System.currentTimeMillis() - 1000;
        NationInvite invite = new NationInvite("n1", "k2", expire);
        assertTrue(invite.isExpired());
    }

    @Test
    void nationInviteRemainingSecondsPositivo() {
        long expire = System.currentTimeMillis() + 30000;
        NationInvite invite = new NationInvite("n1", "k2", expire);
        long remaining = invite.getRemainingSeconds();
        assertTrue(remaining > 0 && remaining <= 30);
    }
}
