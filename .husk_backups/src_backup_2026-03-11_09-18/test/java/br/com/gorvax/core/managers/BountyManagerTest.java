package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.BountyManager.Bounty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para BountyManager.
 * Testa modelo Bounty e lógica de negócio replicada.
 */
class BountyManagerTest {

    private UUID targetUUID;
    private UUID placerUUID;

    @BeforeEach
    void setUp() {
        targetUUID = UUID.randomUUID();
        placerUUID = UUID.randomUUID();
    }

    // --- Lógica replicada de placeBounty ---

    /**
     * Replica a lógica de validação do placeBounty.
     * 
     * @return 0=sucesso, 1=desabilitado, 2=valor inválido, 4=si mesmo
     */
    private int placeBountyValidation(boolean enabled, double value, double minValue, double maxValue,
            UUID placerUUID, UUID targetUUID) {
        if (!enabled)
            return 1;
        if (value < minValue || value > maxValue)
            return 2;
        if (placerUUID.equals(targetUUID))
            return 4;
        return 0;
    }

    @Test
    void placeBountyDesabilitadoRetorna1() {
        assertEquals(1, placeBountyValidation(false, 500, 100, 1000000, placerUUID, targetUUID));
    }

    @Test
    void placeBountyValorAbaixoMinimoRetorna2() {
        assertEquals(2, placeBountyValidation(true, 50, 100, 1000000, placerUUID, targetUUID));
    }

    @Test
    void placeBountyValorAcimaMaximoRetorna2() {
        assertEquals(2, placeBountyValidation(true, 2000000, 100, 1000000, placerUUID, targetUUID));
    }

    @Test
    void placeBountySiMesmoRetorna4() {
        assertEquals(4, placeBountyValidation(true, 500, 100, 1000000, placerUUID, placerUUID));
    }

    @Test
    void placeBountySucessoRetorna0() {
        assertEquals(0, placeBountyValidation(true, 500, 100, 1000000, placerUUID, targetUUID));
    }

    // --- Bounty model ---

    @Test
    void bountyModelDefaultValues() {
        Bounty bounty = new Bounty(targetUUID, "TestPlayer");
        assertEquals(targetUUID, bounty.targetUUID);
        assertEquals("TestPlayer", bounty.targetName);
        assertEquals(0, bounty.totalValue, 0.001);
        assertTrue(bounty.contributors.isEmpty());
        assertTrue(bounty.lastUpdated > 0);
    }

    @Test
    void bountyModelFormattedDate() {
        Bounty bounty = new Bounty(targetUUID, "Test");
        assertNotNull(bounty.getFormattedDate());
        assertFalse(bounty.getFormattedDate().isEmpty());
    }

    @Test
    void bountyAcumulaValor() {
        Bounty bounty = new Bounty(targetUUID, "Target");
        UUID placer1 = UUID.randomUUID();
        UUID placer2 = UUID.randomUUID();

        bounty.totalValue += 300;
        bounty.contributors.merge(placer1, 300.0, Double::sum);
        bounty.totalValue += 200;
        bounty.contributors.merge(placer2, 200.0, Double::sum);

        assertEquals(500.0, bounty.totalValue, 0.001);
        assertEquals(2, bounty.contributors.size());
    }

    @Test
    void bountyMesmoPlacerAcumula() {
        Bounty bounty = new Bounty(targetUUID, "Target");

        bounty.totalValue += 300;
        bounty.contributors.merge(placerUUID, 300.0, Double::sum);
        bounty.totalValue += 200;
        bounty.contributors.merge(placerUUID, 200.0, Double::sum);

        assertEquals(500.0, bounty.totalValue, 0.001);
        assertEquals(1, bounty.contributors.size());
        assertEquals(500.0, bounty.contributors.get(placerUUID), 0.001);
    }

    // --- resolveBounty (lógica replicada) ---

    @Test
    void resolveBountyComBountyAtiva() {
        Map<UUID, Bounty> activeBounties = new ConcurrentHashMap<>();
        Bounty bounty = new Bounty(targetUUID, "Target");
        bounty.totalValue = 500;
        activeBounties.put(targetUUID, bounty);

        Bounty removed = activeBounties.remove(targetUUID);
        double valor = removed != null ? removed.totalValue : 0;

        assertEquals(500.0, valor, 0.001);
        assertNull(activeBounties.get(targetUUID));
    }

    @Test
    void resolveBountySemBountyRetornaZero() {
        Map<UUID, Bounty> activeBounties = new ConcurrentHashMap<>();
        Bounty removed = activeBounties.remove(UUID.randomUUID());
        double valor = removed != null ? removed.totalValue : 0;
        assertEquals(0, valor, 0.001);
    }

    // --- removeContribution (lógica replicada) ---

    @Test
    void removeContributionSucesso() {
        Map<UUID, Bounty> activeBounties = new ConcurrentHashMap<>();
        Bounty bounty = new Bounty(targetUUID, "Target");
        UUID placer2 = UUID.randomUUID();
        bounty.totalValue = 500;
        bounty.contributors.put(placerUUID, 300.0);
        bounty.contributors.put(placer2, 200.0);
        activeBounties.put(targetUUID, bounty);

        // Remove contribuição do placer1
        Double contribution = bounty.contributors.remove(placerUUID);
        assertNotNull(contribution);
        bounty.totalValue -= contribution;

        assertEquals(200.0, bounty.totalValue, 0.001);
        assertEquals(1, bounty.contributors.size());
    }

    @Test
    void removeContributionRemoveBountySeZerada() {
        Map<UUID, Bounty> activeBounties = new ConcurrentHashMap<>();
        Bounty bounty = new Bounty(targetUUID, "Target");
        bounty.totalValue = 300;
        bounty.contributors.put(placerUUID, 300.0);
        activeBounties.put(targetUUID, bounty);

        Double contribution = bounty.contributors.remove(placerUUID);
        bounty.totalValue -= contribution;
        if (bounty.totalValue <= 0 || bounty.contributors.isEmpty()) {
            activeBounties.remove(targetUUID);
        }

        assertNull(activeBounties.get(targetUUID));
    }

    // --- getAllBounties: ordenação ---

    @Test
    void getAllBountiesOrdenaPorValor() {
        Map<UUID, Bounty> activeBounties = new ConcurrentHashMap<>();
        UUID t1 = UUID.randomUUID(), t2 = UUID.randomUUID(), t3 = UUID.randomUUID();

        Bounty b1 = new Bounty(t1, "T1");
        b1.totalValue = 100;
        Bounty b2 = new Bounty(t2, "T2");
        b2.totalValue = 500;
        Bounty b3 = new Bounty(t3, "T3");
        b3.totalValue = 200;
        activeBounties.put(t1, b1);
        activeBounties.put(t2, b2);
        activeBounties.put(t3, b3);

        List<Bounty> sorted = new ArrayList<>(activeBounties.values());
        sorted.sort((a, b) -> Double.compare(b.totalValue, a.totalValue));

        assertEquals(500.0, sorted.get(0).totalValue, 0.001);
        assertEquals(200.0, sorted.get(1).totalValue, 0.001);
        assertEquals(100.0, sorted.get(2).totalValue, 0.001);
    }

    @Test
    void bountyValorNaoNegativo() {
        Bounty bounty = new Bounty(targetUUID, "Test");
        assertEquals(0, bounty.totalValue, 0.001);
    }
}
