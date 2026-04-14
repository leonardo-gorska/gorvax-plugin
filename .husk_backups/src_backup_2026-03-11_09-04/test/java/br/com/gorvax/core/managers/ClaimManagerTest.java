package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.Claim.TrustType;
import br.com.gorvax.core.managers.Claim.Type;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o modelo Claim e lógica de overlap.
 * Testa overlap detection, chunk key, propriedades, trust e types.
 */
class ClaimManagerTest {

    // --- Helper: cria Claim usando o Load Constructor ---
    private Claim createClaim(String id, UUID owner, String world, int minX, int minZ, int maxX, int maxZ) {
        return new Claim(id, owner, world, minX, minZ, maxX, maxZ, false, "Terreno de Teste");
    }

    // --- Overlap detection (lógica pura replicada) ---

    private boolean isOverlapping(String world1, int minX1, int minZ1, int maxX1, int maxZ1,
            String world2, int minX2, int minZ2, int maxX2, int maxZ2) {
        if (!world1.equals(world2))
            return false;
        return minX1 <= maxX2 && maxX1 >= minX2 && minZ1 <= maxZ2 && maxZ1 >= minZ2;
    }

    @Test
    void overlapTotal() {
        assertTrue(isOverlapping("world", 0, 0, 10, 10, "world", 0, 0, 10, 10));
    }

    @Test
    void overlapParcial() {
        assertTrue(isOverlapping("world", 0, 0, 10, 10, "world", 5, 5, 15, 15));
    }

    @Test
    void semOverlapDireita() {
        assertFalse(isOverlapping("world", 0, 0, 10, 10, "world", 11, 0, 20, 10));
    }

    @Test
    void semOverlapAbaixo() {
        assertFalse(isOverlapping("world", 0, 0, 10, 10, "world", 0, 11, 10, 20));
    }

    @Test
    void overlapNaBorda() {
        assertTrue(isOverlapping("world", 0, 0, 10, 10, "world", 10, 10, 20, 20));
    }

    @Test
    void semOverlapMundosDiferentes() {
        assertFalse(isOverlapping("world", 0, 0, 10, 10, "world_nether", 0, 0, 10, 10));
    }

    @Test
    void overlapContido() {
        assertTrue(isOverlapping("world", 0, 0, 20, 20, "world", 5, 5, 15, 15));
    }

    @Test
    void overlapNegativo() {
        assertTrue(isOverlapping("world", -10, -10, 10, 10, "world", -5, -5, 5, 5));
    }

    // --- Chunk key calculation ---

    private long getChunkKey(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    @Test
    void chunkKeyOrigin() {
        assertEquals(getChunkKey(0, 0), getChunkKey(15, 15));
    }

    @Test
    void chunkKeysDiferentes() {
        assertNotEquals(getChunkKey(0, 0), getChunkKey(16, 0));
    }

    @Test
    void chunkKeyNegativo() {
        assertEquals(getChunkKey(-16, -16), getChunkKey(-1, -1));
    }

    @Test
    void chunkKeyDeterministico() {
        assertEquals(getChunkKey(100, 200), getChunkKey(100, 200));
    }

    // --- Claim model: Load Constructor ---

    @Test
    void claimPropBasicas() {
        UUID owner = UUID.randomUUID();
        Claim claim = createClaim("claim-1", owner, "world", 0, 0, 100, 100);

        assertEquals("claim-1", claim.getId());
        assertEquals(owner, claim.getOwner());
        assertEquals("world", claim.getWorldName());
        assertEquals(0, claim.getMinX());
        assertEquals(0, claim.getMinZ());
        assertEquals(100, claim.getMaxX());
        assertEquals(100, claim.getMaxZ());
    }

    @Test
    void claimArea() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 10, 10);
        assertEquals(121, claim.getArea()); // (10-0+1)*(10-0+1) = 11*11
    }

    @Test
    void claimAreaGrande() {
        Claim claim = createClaim("c2", UUID.randomUUID(), "world", -50, -50, 50, 50);
        assertEquals(101 * 101, claim.getArea());
    }

    // --- Trust System ---

    @Test
    void trustAddRemove() {
        UUID owner = UUID.randomUUID();
        UUID trusted = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        Claim claim = createClaim("c1", owner, "world", 0, 0, 100, 100);

        // Owner sempre tem permissão
        assertTrue(claim.hasPermission(owner, TrustType.GERAL));

        // Stranger sem trust
        assertFalse(claim.hasPermission(stranger, TrustType.ACESSO));

        // Adicionar trust
        claim.addTrust(trusted, TrustType.CONSTRUCAO);
        assertTrue(claim.hasPermission(trusted, TrustType.CONSTRUCAO));
        assertFalse(claim.hasPermission(trusted, TrustType.CONTEINER)); // Sem herança

        // Remover trust
        claim.removeTrust(trusted);
        assertFalse(claim.hasPermission(trusted, TrustType.CONSTRUCAO));
    }

    @Test
    void trustGeralDaTudo() {
        UUID owner = UUID.randomUUID();
        UUID trusted = UUID.randomUUID();
        Claim claim = createClaim("c1", owner, "world", 0, 0, 100, 100);

        claim.addTrust(trusted, TrustType.GERAL);
        assertTrue(claim.hasPermission(trusted, TrustType.ACESSO));
        assertTrue(claim.hasPermission(trusted, TrustType.CONTEINER));
        assertTrue(claim.hasPermission(trusted, TrustType.CONSTRUCAO));
        assertFalse(claim.hasPermission(trusted, TrustType.VICE)); // GERAL não dá VICE
    }

    @Test
    void trustViceDaTudo() {
        UUID owner = UUID.randomUUID();
        UUID trusted = UUID.randomUUID();
        Claim claim = createClaim("c1", owner, "world", 0, 0, 100, 100);

        claim.addTrust(trusted, TrustType.VICE);
        assertTrue(claim.hasPermission(trusted, TrustType.ACESSO));
        assertTrue(claim.hasPermission(trusted, TrustType.VICE));
        assertTrue(claim.hasPermission(trusted, TrustType.CONSTRUCAO));
    }

    // --- Kingdom e Outpost ---

    @Test
    void claimKingdom() {
        Claim claim = new Claim("k1", UUID.randomUUID(), "world", 0, 0, 500, 500, true, "Reino Gorvax");
        assertTrue(claim.isKingdom());
        assertFalse(claim.isOutpost());
    }

    @Test
    void claimOutpost() {
        Claim claim = createClaim("o1", UUID.randomUUID(), "world", 0, 0, 50, 50);
        assertFalse(claim.isOutpost());
        claim.setType(Type.OUTPOST);
        assertTrue(claim.isOutpost());
    }

    @Test
    void claimKingdomName() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 100, 100);
        assertNull(claim.getKingdomName());
        claim.setKingdomName("Gorvax Empire");
        assertEquals("Gorvax Empire", claim.getKingdomName());
    }

    // --- Tag com sanitização ---

    @Test
    void tagMaximoDe3Chars() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 100, 100);
        claim.setTag("ABCDEFG");
        assertEquals("ABC", claim.getTag());
    }

    @Test
    void tagRemoveCodigosCor() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 100, 100);
        claim.setTag("§aGX");
        // §a é removido → "aGX" → truncado em 3
        assertNotNull(claim.getTag());
        assertFalse(claim.getTag().contains("§"));
    }

    // --- Setters/Getters simples ---

    @Test
    void pvpFlags() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 100, 100);
        assertFalse(claim.isPvp());
        claim.setPvp(true);
        assertTrue(claim.isPvp());
    }

    @Test
    void publicFlag() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 100, 100);
        assertTrue(claim.isPublic()); // Default true
        claim.setPublic(false);
        assertFalse(claim.isPublic());
    }

    @Test
    void taxPadrao() {
        Claim claim = createClaim("c1", UUID.randomUUID(), "world", 0, 0, 100, 100);
        assertEquals(5.0, claim.getTax(), 0.001);
        claim.setTax(10.0);
        assertEquals(10.0, claim.getTax(), 0.001);
    }
}
