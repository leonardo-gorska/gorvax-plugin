package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para CosmeticManager.
 * Testa lógica pura do CosmeticType enum e CosmeticEntry record.
 */
class CosmeticManagerTest {

    // --- CosmeticType enum ---

    @Test
    void cosmeticTypeValues() {
        assertEquals(5, CosmeticManager.CosmeticType.values().length);
    }

    @Test
    void cosmeticTypeContainsExpected() {
        assertDoesNotThrow(() -> CosmeticManager.CosmeticType.valueOf("WALK_PARTICLE"));
        assertDoesNotThrow(() -> CosmeticManager.CosmeticType.valueOf("KILL_PARTICLE"));
        assertDoesNotThrow(() -> CosmeticManager.CosmeticType.valueOf("ARROW_TRAIL"));
        assertDoesNotThrow(() -> CosmeticManager.CosmeticType.valueOf("CHAT_TAG"));
        assertDoesNotThrow(() -> CosmeticManager.CosmeticType.valueOf("KILL_EFFECT"));
    }

    @Test
    void cosmeticTypeInvalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> CosmeticManager.CosmeticType.valueOf("INVALID_TYPE"));
    }

    // --- CosmeticEntry record ---

    @Test
    void cosmeticEntryRecord() {
        var entry = new CosmeticManager.CosmeticEntry(
                "flames_walk",
                CosmeticManager.CosmeticType.WALK_PARTICLE,
                "§cChamas ao Caminhar",
                "Partículas de fogo nos seus passos",
                "shop",
                "gorvax.cosmetic.flames",
                500.0,
                null, // Particle (Bukkit, ignorado)
                3, 0.02, 0.1,
                null, null);
        assertEquals("flames_walk", entry.id());
        assertEquals(CosmeticManager.CosmeticType.WALK_PARTICLE, entry.type());
        assertEquals("§cChamas ao Caminhar", entry.name());
        assertEquals("Partículas de fogo nos seus passos", entry.description());
        assertEquals("shop", entry.source());
        assertEquals("gorvax.cosmetic.flames", entry.permission());
        assertEquals(500.0, entry.price(), 0.001);
        assertEquals(3, entry.count());
        assertEquals(0.02, entry.speed(), 0.001);
        assertEquals(0.1, entry.offsetY(), 0.001);
        assertNull(entry.display());
        assertNull(entry.effect());
    }

    @Test
    void cosmeticEntryEquality() {
        var a = new CosmeticManager.CosmeticEntry(
                "tag_1", CosmeticManager.CosmeticType.CHAT_TAG,
                "§eTag VIP", "", "vip", null, 0,
                null, 0, 0, 0,
                "§eVIP", null);
        var b = new CosmeticManager.CosmeticEntry(
                "tag_1", CosmeticManager.CosmeticType.CHAT_TAG,
                "§eTag VIP", "", "vip", null, 0,
                null, 0, 0, 0,
                "§eVIP", null);
        assertEquals(a, b);
    }

    @Test
    void cosmeticEntryInequality() {
        var a = new CosmeticManager.CosmeticEntry(
                "tag_1", CosmeticManager.CosmeticType.CHAT_TAG,
                "§eTag VIP", "", "vip", null, 0,
                null, 0, 0, 0, "§eVIP", null);
        var b = new CosmeticManager.CosmeticEntry(
                "tag_2", CosmeticManager.CosmeticType.CHAT_TAG,
                "§bTag MVP", "", "vip", null, 0,
                null, 0, 0, 0, "§bMVP", null);
        assertNotEquals(a, b);
    }

    @Test
    void cosmeticEntryChatTag() {
        var tag = new CosmeticManager.CosmeticEntry(
                "crown_tag", CosmeticManager.CosmeticType.CHAT_TAG,
                "§6Coroa", "Coroa dourada no chat", "achievement",
                null, 0, null, 0, 0, 0, "§6👑", null);
        assertEquals("§6👑", tag.display());
        assertEquals(CosmeticManager.CosmeticType.CHAT_TAG, tag.type());
    }

    @Test
    void cosmeticEntryKillEffect() {
        var effect = new CosmeticManager.CosmeticEntry(
                "lightning_kill", CosmeticManager.CosmeticType.KILL_EFFECT,
                "§bRelâmpago", "Raio no kill", "crate",
                null, 0, null, 0, 0, 0, null, "LIGHTNING");
        assertEquals("LIGHTNING", effect.effect());
        assertEquals(CosmeticManager.CosmeticType.KILL_EFFECT, effect.type());
    }

    @Test
    void cosmeticEntryAdminSource() {
        var entry = new CosmeticManager.CosmeticEntry(
                "admin_aura", CosmeticManager.CosmeticType.WALK_PARTICLE,
                "§4Admin Aura", "Exclusivo", "admin",
                null, 0, null, 5, 0.05, 0.2, null, null);
        assertEquals("admin", entry.source());
        assertNull(entry.permission());
        assertEquals(0.0, entry.price(), 0.001);
    }
}
