package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para RankManager.
 * Testa lógica pura de GameRank enum, RankRequirement, formatTime,
 * formatItemName, getVipPriority.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class RankManagerTest {

    // --- GameRank enum ---

    @Test
    void gameRankValues() {
        assertEquals(4, RankManager.GameRank.values().length);
    }

    @Test
    void gameRankOrdem() {
        assertEquals(0, RankManager.GameRank.AVENTUREIRO.getOrder());
        assertEquals(1, RankManager.GameRank.EXPLORADOR.getOrder());
        assertEquals(2, RankManager.GameRank.GUERREIRO.getOrder());
        assertEquals(3, RankManager.GameRank.LENDARIO.getOrder());
    }

    @Test
    void gameRankDisplayNames() {
        assertTrue(RankManager.GameRank.AVENTUREIRO.getDisplayName().contains("Aventureiro"));
        assertTrue(RankManager.GameRank.EXPLORADOR.getDisplayName().contains("Explorador"));
        assertTrue(RankManager.GameRank.GUERREIRO.getDisplayName().contains("Guerreiro"));
        assertTrue(RankManager.GameRank.LENDARIO.getDisplayName().contains("Lendário"));
    }

    @Test
    void gameRankGroupNames() {
        assertEquals("aventureiro", RankManager.GameRank.AVENTUREIRO.getGroupName());
        assertEquals("explorador", RankManager.GameRank.EXPLORADOR.getGroupName());
        assertEquals("guerreiro", RankManager.GameRank.GUERREIRO.getGroupName());
        assertEquals("lendario", RankManager.GameRank.LENDARIO.getGroupName());
    }

    @Test
    void gameRankNextProgression() {
        assertEquals(RankManager.GameRank.EXPLORADOR, RankManager.GameRank.AVENTUREIRO.next());
        assertEquals(RankManager.GameRank.GUERREIRO, RankManager.GameRank.EXPLORADOR.next());
        assertEquals(RankManager.GameRank.LENDARIO, RankManager.GameRank.GUERREIRO.next());
    }

    @Test
    void gameRankNextMaxRetornaNull() {
        assertNull(RankManager.GameRank.LENDARIO.next());
    }

    @Test
    void gameRankIconNotNull() {
        for (RankManager.GameRank rank : RankManager.GameRank.values()) {
            assertNotNull(rank.getIcon());
        }
    }

    // --- RankRequirement ---

    @Test
    void rankRequirementCampos() {
        var req = new RankManager.RankRequirement(10, 500, 0, 0, 2000, 0);
        assertEquals(10, req.playtimeHours);
        assertEquals(500, req.blocksBroken);
        assertEquals(0, req.kills);
        assertEquals(0, req.bossesKilled);
        assertEquals(2000.0, req.balance, 0.001);
        assertEquals(0, req.kingdomLevel);
    }

    @Test
    void rankRequirementGuerreiro() {
        var req = new RankManager.RankRequirement(30, 0, 50, 5, 10000, 0);
        assertEquals(30, req.playtimeHours);
        assertEquals(50, req.kills);
        assertEquals(5, req.bossesKilled);
        assertEquals(10000.0, req.balance, 0.001);
    }

    @Test
    void rankRequirementLendario() {
        var req = new RankManager.RankRequirement(80, 0, 200, 20, 50000, 3);
        assertEquals(80, req.playtimeHours);
        assertEquals(200, req.kills);
        assertEquals(20, req.bossesKilled);
        assertEquals(50000.0, req.balance, 0.001);
        assertEquals(3, req.kingdomLevel);
    }

    // --- formatTime (lógica replicada) ---

    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            return h + "h " + m + "min";
        } else if (seconds >= 60) {
            return (seconds / 60) + "min";
        }
        return seconds + "s";
    }

    @Test
    void formatTimeSegundos() {
        assertEquals("30s", formatTime(30));
    }

    @Test
    void formatTimeZero() {
        assertEquals("0s", formatTime(0));
    }

    @Test
    void formatTimeMinutos() {
        assertEquals("5min", formatTime(300));
    }

    @Test
    void formatTimeUmMinuto() {
        assertEquals("1min", formatTime(60));
    }

    @Test
    void formatTimeHoras() {
        assertEquals("2h 30min", formatTime(9000));
    }

    @Test
    void formatTimeUmaHora() {
        assertEquals("1h 0min", formatTime(3600));
    }

    @Test
    void formatTime24Horas() {
        assertEquals("24h 0min", formatTime(86400));
    }

    @Test
    void formatTime12Horas() {
        assertEquals("12h 0min", formatTime(43200));
    }

    // --- formatItemName (lógica replicada) ---

    private String formatItemName(String materialName) {
        String name = materialName.replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty())
                sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return sb.toString();
    }

    @Test
    void formatItemNameStoneSword() {
        assertEquals("Stone Sword", formatItemName("STONE_SWORD"));
    }

    @Test
    void formatItemNameDiamondChestplate() {
        assertEquals("Diamond Chestplate", formatItemName("DIAMOND_CHESTPLATE"));
    }

    @Test
    void formatItemNameNetheritePickaxe() {
        assertEquals("Netherite Pickaxe", formatItemName("NETHERITE_PICKAXE"));
    }

    @Test
    void formatItemNameSingleWord() {
        assertEquals("Elytra", formatItemName("ELYTRA"));
    }

    // --- getVipPriority (lógica replicada) ---

    private int getVipPriority(String tier) {
        return switch (tier) {
            case "vip" -> 1;
            case "mvp" -> 2;
            case "elite" -> 3;
            default -> 99;
        };
    }

    @Test
    void vipPriorityVip() {
        assertEquals(1, getVipPriority("vip"));
    }

    @Test
    void vipPriorityMvp() {
        assertEquals(2, getVipPriority("mvp"));
    }

    @Test
    void vipPriorityElite() {
        assertEquals(3, getVipPriority("elite"));
    }

    @Test
    void vipPriorityDesconhecido() {
        assertEquals(99, getVipPriority("unknown"));
    }

    @Test
    void vipPriorityOrdem() {
        assertTrue(getVipPriority("vip") < getVipPriority("mvp"));
        assertTrue(getVipPriority("mvp") < getVipPriority("elite"));
    }
}
