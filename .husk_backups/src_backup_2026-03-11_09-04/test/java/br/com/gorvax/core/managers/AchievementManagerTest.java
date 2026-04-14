package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para AchievementManager.
 * Testa lógica pura de buildProgressBar, parseIntSafe, parseDoubleSafe e
 * AchievementType.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class AchievementManagerTest {

    // --- buildProgressBar (lógica replicada) ---

    private String buildProgressBar(int current, int max) {
        int bars = 10;
        int filled = max > 0 ? Math.min(bars, (current * bars) / max) : 0;
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a■" : "§7■");
        }
        sb.append("§8]");
        return sb.toString();
    }

    @Test
    void progressBarZero() {
        String bar = buildProgressBar(0, 100);
        assertEquals("§8[§7■§7■§7■§7■§7■§7■§7■§7■§7■§7■§8]", bar);
    }

    @Test
    void progressBarCompleto() {
        String bar = buildProgressBar(100, 100);
        assertEquals("§8[§a■§a■§a■§a■§a■§a■§a■§a■§a■§a■§8]", bar);
    }

    @Test
    void progressBarMetade() {
        String bar = buildProgressBar(50, 100);
        assertEquals("§8[§a■§a■§a■§a■§a■§7■§7■§7■§7■§7■§8]", bar);
    }

    @Test
    void progressBarVintePercento() {
        String bar = buildProgressBar(20, 100);
        assertEquals("§8[§a■§a■§7■§7■§7■§7■§7■§7■§7■§7■§8]", bar);
    }

    @Test
    void progressBarMaxZero() {
        String bar = buildProgressBar(50, 0);
        // max == 0, filled = 0
        assertEquals("§8[§7■§7■§7■§7■§7■§7■§7■§7■§7■§7■§8]", bar);
    }

    @Test
    void progressBarExcedeMax() {
        String bar = buildProgressBar(200, 100);
        // filled clampeia em 10
        assertEquals("§8[§a■§a■§a■§a■§a■§a■§a■§a■§a■§a■§8]", bar);
    }

    @Test
    void progressBarUmTotal() {
        String bar = buildProgressBar(0, 1);
        assertEquals("§8[§7■§7■§7■§7■§7■§7■§7■§7■§7■§7■§8]", bar);
    }

    @Test
    void progressBarUmDeUm() {
        String bar = buildProgressBar(1, 1);
        assertEquals("§8[§a■§a■§a■§a■§a■§a■§a■§a■§a■§a■§8]", bar);
    }

    // --- parseIntSafe (lógica replicada) ---

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    @Test
    void parseIntSafeNumeroValido() {
        assertEquals(42, parseIntSafe("42", 0));
    }

    @Test
    void parseIntSafeNegativo() {
        assertEquals(-5, parseIntSafe("-5", 0));
    }

    @Test
    void parseIntSafeInvalido() {
        assertEquals(99, parseIntSafe("abc", 99));
    }

    @Test
    void parseIntSafeVazio() {
        assertEquals(0, parseIntSafe("", 0));
    }

    @Test
    void parseIntSafeNull() {
        assertEquals(10, parseIntSafe(null, 10));
    }

    @Test
    void parseIntSafeDecimal() {
        assertEquals(0, parseIntSafe("3.14", 0));
    }

    // --- parseDoubleSafe (lógica replicada) ---

    private double parseDoubleSafe(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    @Test
    void parseDoubleSafeNumeroValido() {
        assertEquals(3.14, parseDoubleSafe("3.14", 0), 0.001);
    }

    @Test
    void parseDoubleSafeInteiro() {
        assertEquals(42.0, parseDoubleSafe("42", 0), 0.001);
    }

    @Test
    void parseDoubleSafeNegativo() {
        assertEquals(-1.5, parseDoubleSafe("-1.5", 0), 0.001);
    }

    @Test
    void parseDoubleSafeInvalido() {
        assertEquals(5.0, parseDoubleSafe("abc", 5.0), 0.001);
    }

    @Test
    void parseDoubleSafeVazio() {
        assertEquals(0.0, parseDoubleSafe("", 0.0), 0.001);
    }

    @Test
    void parseDoubleSafeNull() {
        assertEquals(1.0, parseDoubleSafe(null, 1.0), 0.001);
    }

    // --- AchievementType enum ---

    @Test
    void achievementTypeValues() {
        assertEquals(10, AchievementManager.AchievementType.values().length);
    }

    @Test
    void achievementTypeContainsBlocksBroken() {
        assertDoesNotThrow(() -> AchievementManager.AchievementType.valueOf("BLOCKS_BROKEN"));
    }

    @Test
    void achievementTypeContainsKills() {
        assertDoesNotThrow(() -> AchievementManager.AchievementType.valueOf("KILLS"));
    }

    @Test
    void achievementTypeContainsBossesKilled() {
        assertDoesNotThrow(() -> AchievementManager.AchievementType.valueOf("BOSSES_KILLED"));
    }

    @Test
    void achievementTypeContainsPlaytimeHours() {
        assertDoesNotThrow(() -> AchievementManager.AchievementType.valueOf("PLAYTIME_HOURS"));
    }

    @Test
    void achievementTypeInvalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AchievementManager.AchievementType.valueOf("INVALID_TYPE"));
    }

    // --- RewardType enum ---

    @Test
    void rewardTypeValues() {
        assertEquals(3, AchievementManager.RewardType.values().length);
        assertDoesNotThrow(() -> AchievementManager.RewardType.valueOf("BLOCKS"));
        assertDoesNotThrow(() -> AchievementManager.RewardType.valueOf("MONEY"));
        assertDoesNotThrow(() -> AchievementManager.RewardType.valueOf("TITLE"));
    }

    // --- Achievement class ---

    @Test
    void achievementClassFields() {
        var ach = new AchievementManager.Achievement(
                "miner_1", "§eMinerador Iniciante", "Quebre 100 blocos", "MINERACAO",
                AchievementManager.AchievementType.BLOCKS_BROKEN, 100,
                AchievementManager.RewardType.BLOCKS, "50", null);
        assertEquals("miner_1", ach.id);
        assertEquals("§eMinerador Iniciante", ach.name);
        assertEquals("Quebre 100 blocos", ach.description);
        assertEquals("MINERACAO", ach.category);
        assertEquals(AchievementManager.AchievementType.BLOCKS_BROKEN, ach.type);
        assertEquals(100, ach.goal);
        assertEquals(AchievementManager.RewardType.BLOCKS, ach.rewardType);
        assertEquals("50", ach.rewardValue);
    }
}
