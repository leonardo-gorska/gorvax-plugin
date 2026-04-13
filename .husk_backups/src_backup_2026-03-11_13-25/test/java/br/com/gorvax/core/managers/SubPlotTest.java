package br.com.gorvax.core.managers;

import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para SubPlot.
 * Usa mocks leves de Location para contains().
 */
@ExtendWith(MockitoExtension.class)
class SubPlotTest {

    private SubPlot plot;
    private UUID ownerUUID;

    @BeforeEach
    void setUp() {
        ownerUUID = UUID.randomUUID();
        // SubPlot de (5,5) a (15,15) via Load Constructor
        plot = new SubPlot("lote-1", "Lote Central", 5, 5, 15, 15,
                1000.0, 50.0, false, false, ownerUUID, null, 0L);
    }

    private Location mockLoc(int x, int z) {
        Location loc = mock(Location.class);
        when(loc.getBlockX()).thenReturn(x);
        when(loc.getBlockZ()).thenReturn(z);
        return loc;
    }

    // --- contains ---

    @Test
    void containsDentro() {
        assertTrue(plot.contains(mockLoc(10, 10)));
    }

    @Test
    void containsBorda() {
        assertTrue(plot.contains(mockLoc(5, 5)));
        assertTrue(plot.contains(mockLoc(15, 15)));
    }

    @Test
    void containsFora() {
        assertFalse(plot.contains(mockLoc(4, 10)));
        assertFalse(plot.contains(mockLoc(16, 10)));
        assertFalse(plot.contains(mockLoc(10, 4)));
        assertFalse(plot.contains(mockLoc(10, 16)));
    }

    // --- Trust system ---

    @Test
    void ownerTemPermissaoTotal() {
        assertTrue(plot.hasPermission(ownerUUID, Claim.TrustType.VICE));
        assertTrue(plot.hasPermission(ownerUUID, Claim.TrustType.GERAL));
        assertTrue(plot.hasPermission(ownerUUID, Claim.TrustType.ACESSO));
    }

    @Test
    void renterTemPermissaoTotal() {
        UUID renter = UUID.randomUUID();
        plot.setRenter(renter);
        assertTrue(plot.hasPermission(renter, Claim.TrustType.CONSTRUCAO));
        assertTrue(plot.hasPermission(renter, Claim.TrustType.VICE));
    }

    @Test
    void semPermissaoNegado() {
        UUID stranger = UUID.randomUUID();
        assertFalse(plot.hasPermission(stranger, Claim.TrustType.ACESSO));
    }

    @Test
    void addTrustERemoveTrustTotal() {
        UUID player = UUID.randomUUID();
        plot.addTrust(player, Claim.TrustType.ACESSO);
        assertTrue(plot.hasPermission(player, Claim.TrustType.ACESSO));

        plot.removeTrust(player);
        assertFalse(plot.hasPermission(player, Claim.TrustType.ACESSO));
    }

    @Test
    void removeTrustPorTipo() {
        UUID player = UUID.randomUUID();
        plot.addTrust(player, Claim.TrustType.ACESSO);
        plot.addTrust(player, Claim.TrustType.CONTEINER);

        plot.removeTrust(player, Claim.TrustType.ACESSO);
        assertFalse(plot.hasPermission(player, Claim.TrustType.ACESSO));
        assertTrue(plot.hasPermission(player, Claim.TrustType.CONTEINER));
    }

    @Test
    void removeTrustPorTipoUltimoRemoveDoMapa() {
        UUID player = UUID.randomUUID();
        plot.addTrust(player, Claim.TrustType.ACESSO);

        plot.removeTrust(player, Claim.TrustType.ACESSO);
        assertFalse(plot.getTrustedPlayers().containsKey(player));
    }

    @Test
    void viceTemTudo() {
        UUID vicePlayer = UUID.randomUUID();
        plot.addTrust(vicePlayer, Claim.TrustType.VICE);

        assertTrue(plot.hasPermission(vicePlayer, Claim.TrustType.VICE));
        assertTrue(plot.hasPermission(vicePlayer, Claim.TrustType.GERAL));
        assertTrue(plot.hasPermission(vicePlayer, Claim.TrustType.ACESSO));
    }

    @Test
    void geralNaoDaVice() {
        UUID geralPlayer = UUID.randomUUID();
        plot.addTrust(geralPlayer, Claim.TrustType.GERAL);

        assertTrue(plot.hasPermission(geralPlayer, Claim.TrustType.ACESSO));
        assertTrue(plot.hasPermission(geralPlayer, Claim.TrustType.CONSTRUCAO));
        assertFalse(plot.hasPermission(geralPlayer, Claim.TrustType.VICE));
    }

    // --- hasEffectiveOwner ---

    @Test
    void hasEffectiveOwnerComOwner() {
        assertTrue(plot.hasEffectiveOwner());
    }

    @Test
    void hasEffectiveOwnerComRenter() {
        SubPlot emptyPlot = new SubPlot("lote-2", "Vazio", 0, 0, 10, 10,
                0, 0, false, false, null, UUID.randomUUID(), 0L);
        assertTrue(emptyPlot.hasEffectiveOwner());
    }

    @Test
    void hasEffectiveOwnerSemNenhum() {
        SubPlot emptyPlot = new SubPlot("lote-3", "Vazio", 0, 0, 10, 10,
                0, 0, false, false, null, null, 0L);
        assertFalse(emptyPlot.hasEffectiveOwner());
    }

    // --- Flags de venda/aluguel ---

    @Test
    void flagsVendaAluguel() {
        assertFalse(plot.isForSale());
        assertFalse(plot.isForRent());

        plot.setForSale(true);
        plot.setForRent(true);
        assertTrue(plot.isForSale());
        assertTrue(plot.isForRent());
    }
}
