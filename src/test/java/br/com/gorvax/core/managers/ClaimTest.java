package br.com.gorvax.core.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para Claim.
 * Usa Mockito para mockar Location e World do Bukkit.
 */
class ClaimTest {

    private Claim claim;
    private UUID ownerUUID;

    @BeforeEach
    void setUp() {
        ownerUUID = UUID.randomUUID();
        // Claim de (10,10) a (20,20)
        claim = new Claim("claim-1", ownerUUID, "world", 10, 10, 20, 20, false, "Terreno de Teste");
    }

    // --- Helpers ---
    private Location mockLocation(String worldName, int x, int z) {
        World w = mock(World.class);
        when(w.getName()).thenReturn(worldName);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(w);
        when(loc.getBlockX()).thenReturn(x);
        when(loc.getBlockZ()).thenReturn(z);
        return loc;
    }

    // --- contains ---

    @Test
    void containsDentro() {
        assertTrue(claim.contains(mockLocation("world", 15, 15)));
    }

    @Test
    void containsBorda() {
        assertTrue(claim.contains(mockLocation("world", 10, 10)));
        assertTrue(claim.contains(mockLocation("world", 20, 20)));
    }

    @Test
    void containsFora() {
        assertFalse(claim.contains(mockLocation("world", 9, 15)));
        assertFalse(claim.contains(mockLocation("world", 21, 15)));
    }

    @Test
    void containsMundoErrado() {
        assertFalse(claim.contains(mockLocation("nether", 15, 15)));
    }

    @Test
    void containsWorldNull() {
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(null);
        assertFalse(claim.contains(loc));
    }

    // --- getArea ---

    @Test
    void areaCorreta() {
        // (20-10+1) * (20-10+1) = 11 * 11 = 121
        assertEquals(121, claim.getArea());
    }

    // --- Trust system ---

    @Test
    void ownerTemPermissaoTotal() {
        assertTrue(claim.hasPermission(ownerUUID, Claim.TrustType.VICE));
        assertTrue(claim.hasPermission(ownerUUID, Claim.TrustType.GERAL));
        assertTrue(claim.hasPermission(ownerUUID, Claim.TrustType.CONSTRUCAO));
    }

    @Test
    void semTrustNegado() {
        UUID stranger = UUID.randomUUID();
        assertFalse(claim.hasPermission(stranger, Claim.TrustType.ACESSO));
    }

    @Test
    void addTrustERemoveTrust() {
        UUID player = UUID.randomUUID();
        claim.addTrust(player, Claim.TrustType.ACESSO);
        assertTrue(claim.hasPermission(player, Claim.TrustType.ACESSO));

        claim.removeTrust(player);
        assertFalse(claim.hasPermission(player, Claim.TrustType.ACESSO));
    }

    @Test
    void removeTrustPorTipo() {
        UUID player = UUID.randomUUID();
        claim.addTrust(player, Claim.TrustType.ACESSO);
        claim.addTrust(player, Claim.TrustType.CONTEINER);

        claim.removeTrust(player, Claim.TrustType.ACESSO);
        assertFalse(claim.hasPermission(player, Claim.TrustType.ACESSO));
        assertTrue(claim.hasPermission(player, Claim.TrustType.CONTEINER));
    }

    @Test
    void viceTemPermissaoTotal() {
        UUID vicePlayer = UUID.randomUUID();
        claim.addTrust(vicePlayer, Claim.TrustType.VICE);

        assertTrue(claim.hasPermission(vicePlayer, Claim.TrustType.VICE));
        assertTrue(claim.hasPermission(vicePlayer, Claim.TrustType.GERAL));
        assertTrue(claim.hasPermission(vicePlayer, Claim.TrustType.ACESSO));
        assertTrue(claim.hasPermission(vicePlayer, Claim.TrustType.CONSTRUCAO));
        assertTrue(claim.hasPermission(vicePlayer, Claim.TrustType.CONTEINER));
    }

    @Test
    void geralNaoTemVice() {
        UUID geralPlayer = UUID.randomUUID();
        claim.addTrust(geralPlayer, Claim.TrustType.GERAL);

        assertTrue(claim.hasPermission(geralPlayer, Claim.TrustType.ACESSO));
        assertTrue(claim.hasPermission(geralPlayer, Claim.TrustType.CONSTRUCAO));
        assertTrue(claim.hasPermission(geralPlayer, Claim.TrustType.CONTEINER));
        assertFalse(claim.hasPermission(geralPlayer, Claim.TrustType.VICE));
    }

    @Test
    void permissaoEspecificaNaoHerda() {
        UUID player = UUID.randomUUID();
        claim.addTrust(player, Claim.TrustType.CONSTRUCAO);

        assertTrue(claim.hasPermission(player, Claim.TrustType.CONSTRUCAO));
        assertFalse(claim.hasPermission(player, Claim.TrustType.CONTEINER)); // Não herda
        assertFalse(claim.hasPermission(player, Claim.TrustType.ACESSO));    // Não herda
    }

    // --- Kingdom/Outpost/Type ---

    @Test
    void isKingdomESetKingdom() {
        assertFalse(claim.isKingdom());
        claim.setKingdom(true);
        assertTrue(claim.isKingdom());
        assertEquals(Claim.Type.REINO, claim.getType());
    }

    @Test
    void isOutpostESetType() {
        assertFalse(claim.isOutpost());
        claim.setType(Claim.Type.OUTPOST);
        assertTrue(claim.isOutpost());
        assertFalse(claim.isKingdom());
    }

    @Test
    void parentKingdomId() {
        assertNull(claim.getParentKingdomId());
        claim.setParentKingdomId("kingdom-abc");
        assertEquals("kingdom-abc", claim.getParentKingdomId());
    }

    // --- setTag ---

    @Test
    void setTagTruncaEm3Chars() {
        claim.setTag("ABCDEF");
        assertEquals("ABC", claim.getTag());
    }

    @Test
    void setTagRemoveCodigosCor() {
        claim.setTag("§aHi");
        assertEquals("aHi", claim.getTag());

        claim.setTag("&bXY");
        assertEquals("bXY", claim.getTag());
    }

    @Test
    void setTagNull() {
        claim.setTag(null);
        assertNull(claim.getTag());
    }

    // --- isPublic ---

    @Test
    void isPublicDefaultTrue() {
        assertTrue(claim.isPublic());
    }

    // --- PvP flags ---

    @Test
    void pvpFlagsDefaultFalse() {
        assertFalse(claim.isPvp());
        assertFalse(claim.isResidentsPvp());
        assertFalse(claim.isResidentsPvpOutside());
    }

    @Test
    void pvpFlagsToggle() {
        claim.setPvp(true);
        claim.setResidentsPvp(true);
        assertTrue(claim.isPvp());
        assertTrue(claim.isResidentsPvp());
    }
}
