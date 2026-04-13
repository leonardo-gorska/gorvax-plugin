package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para BattlePassManager.
 * Testa lógica pura de XP, reward descriptions, capitalize, e RewardEntry
 * record.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class BattlePassManagerTest {

    // --- getXpForLevel (lógica replicada: xpBase + (level - 1) * xpIncrement) ---

    private int getXpForLevel(int level, int xpBase, int xpIncrement) {
        return xpBase + (level - 1) * xpIncrement;
    }

    @Test
    void xpParaNivel1() {
        assertEquals(100, getXpForLevel(1, 100, 20));
    }

    @Test
    void xpParaNivel2() {
        assertEquals(120, getXpForLevel(2, 100, 20));
    }

    @Test
    void xpParaNivel10() {
        assertEquals(280, getXpForLevel(10, 100, 20));
    }

    @Test
    void xpParaNivel30() {
        assertEquals(680, getXpForLevel(30, 100, 20));
    }

    @Test
    void xpComBaseCustomizada() {
        assertEquals(200, getXpForLevel(1, 200, 50));
        assertEquals(250, getXpForLevel(2, 200, 50));
    }

    @Test
    void xpCrescimentoLinear() {
        int prev = getXpForLevel(1, 100, 20);
        for (int i = 2; i <= 30; i++) {
            int curr = getXpForLevel(i, 100, 20);
            assertEquals(20, curr - prev, "Incremento constante de 20 entre níveis");
            prev = curr;
        }
    }

    // --- capitalize (lógica replicada) ---

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Test
    void capitalizeNormal() {
        assertEquals("Raro", capitalize("raro"));
    }

    @Test
    void capitalizeJaMaiusculo() {
        assertEquals("Lendário", capitalize("Lendário"));
    }

    @Test
    void capitalizeVazio() {
        assertEquals("", capitalize(""));
    }

    @Test
    void capitalizeNull() {
        assertNull(capitalize(null));
    }

    @Test
    void capitalizeUmCaractere() {
        assertEquals("A", capitalize("a"));
    }

    // --- getRewardDescription (lógica replicada) ---

    private String getRewardDescription(String type, int amount, String extra) {
        return switch (type) {
            case "money" -> "§a$" + amount;
            case "claim_blocks" -> "§e" + amount + " blocos";
            case "crate_key" -> "§b" + amount + "x Key " + capitalize(extra);
            case "title" -> "§6Título: " + extra;
            case "cosmetic" -> "§dCosmético: " + extra;
            case "custom_item" -> "§cItem: " + extra;
            default -> "§7Recompensa";
        };
    }

    @Test
    void rewardDescriptionMoney() {
        assertEquals("§a$500", getRewardDescription("money", 500, ""));
    }

    @Test
    void rewardDescriptionClaimBlocks() {
        assertEquals("§e100 blocos", getRewardDescription("claim_blocks", 100, ""));
    }

    @Test
    void rewardDescriptionCrateKey() {
        assertEquals("§b2x Key Raro", getRewardDescription("crate_key", 2, "raro"));
    }

    @Test
    void rewardDescriptionTitle() {
        assertEquals("§6Título: §eRei do PvP", getRewardDescription("title", 0, "§eRei do PvP"));
    }

    @Test
    void rewardDescriptionCosmetic() {
        assertEquals("§dCosmético: flames_walk", getRewardDescription("cosmetic", 0, "flames_walk"));
    }

    @Test
    void rewardDescriptionCustomItem() {
        assertEquals("§cItem: espada_gorvax", getRewardDescription("custom_item", 0, "espada_gorvax"));
    }

    @Test
    void rewardDescriptionTipoDesconhecido() {
        assertEquals("§7Recompensa", getRewardDescription("unknown", 0, ""));
    }

    // --- RewardEntry record ---

    @Test
    void rewardEntryRecord() {
        var entry = new BattlePassManager.RewardEntry("money", 1000, "");
        assertEquals("money", entry.type());
        assertEquals(1000, entry.amount());
        assertEquals("", entry.extra());
    }

    @Test
    void rewardEntryEquality() {
        var a = new BattlePassManager.RewardEntry("title", 0, "Rei");
        var b = new BattlePassManager.RewardEntry("title", 0, "Rei");
        assertEquals(a, b);
    }

    @Test
    void rewardEntryInequality() {
        var a = new BattlePassManager.RewardEntry("money", 100, "");
        var b = new BattlePassManager.RewardEntry("money", 200, "");
        assertNotEquals(a, b);
    }

    // --- Simulação de level-up ---

    @Test
    void levelUpSimulationSingle() {
        int xpBase = 100;
        int xpIncrement = 20;
        int level = 1;
        int xp = 0;

        // Ganha 150 XP — suficiente para nível 2 (precisa 120)
        xp += 150;
        int needed = getXpForLevel(level + 1, xpBase, xpIncrement);
        while (xp >= needed && level < 30) {
            xp -= needed;
            level++;
            needed = getXpForLevel(level + 1, xpBase, xpIncrement);
        }
        assertEquals(2, level);
        assertEquals(30, xp); // 150 - 120 = 30 sobra
    }

    @Test
    void levelUpSimulationMultiple() {
        int xpBase = 100;
        int xpIncrement = 20;
        int level = 1;
        int xp = 0;

        // Ganha 1000 XP — suficiente para vários níveis
        xp += 1000;
        int needed = getXpForLevel(level + 1, xpBase, xpIncrement);
        while (xp >= needed && level < 30) {
            xp -= needed;
            level++;
            needed = getXpForLevel(level + 1, xpBase, xpIncrement);
        }
        assertTrue(level > 5, "Deveria subir vários níveis com 1000 XP");
    }

    @Test
    void levelUpNaoUltrapassaMax() {
        int xpBase = 100;
        int xpIncrement = 20;
        int level = 29;
        int xp = 0;

        // Ganha muito XP
        xp += 99999;
        int needed = getXpForLevel(level + 1, xpBase, xpIncrement);
        while (xp >= needed && level < 30) {
            xp -= needed;
            level++;
            needed = getXpForLevel(level + 1, xpBase, xpIncrement);
        }
        assertEquals(30, level);
    }
}
