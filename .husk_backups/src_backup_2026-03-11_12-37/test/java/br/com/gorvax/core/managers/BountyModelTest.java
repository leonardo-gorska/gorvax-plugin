package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.BountyManager.Bounty;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para BountyManager.Bounty (inner class).
 * Lógica pura — sem mocks necessários.
 */
class BountyModelTest {

    @Test
    void criacaoPadrao() {
        UUID target = UUID.randomUUID();
        Bounty bounty = new Bounty(target, "Gorska");

        assertEquals(target, bounty.targetUUID);
        assertEquals("Gorska", bounty.targetName);
        assertEquals(0, bounty.totalValue);
        assertTrue(bounty.contributors.isEmpty());
        assertTrue(bounty.lastUpdated > 0);
    }

    @Test
    void acumularContribuicoes() {
        UUID target = UUID.randomUUID();
        Bounty bounty = new Bounty(target, "Gorska");

        UUID contrib1 = UUID.randomUUID();
        UUID contrib2 = UUID.randomUUID();

        bounty.contributors.put(contrib1, 500.0);
        bounty.contributors.put(contrib2, 300.0);
        bounty.totalValue = 800.0;

        assertEquals(800.0, bounty.totalValue, 0.001);
        assertEquals(2, bounty.contributors.size());
        assertEquals(500.0, bounty.contributors.get(contrib1), 0.001);
    }

    @Test
    void formattedDateNaoNulo() {
        Bounty bounty = new Bounty(UUID.randomUUID(), "Test");
        String date = bounty.getFormattedDate();
        assertNotNull(date);
        // Formato dd/MM HH:mm → pelo menos 11 chars
        assertTrue(date.length() >= 10, "Data formatada: " + date);
    }
}
