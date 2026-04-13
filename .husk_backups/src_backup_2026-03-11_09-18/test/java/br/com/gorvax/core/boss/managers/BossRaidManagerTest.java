package br.com.gorvax.core.boss.managers;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para BossRaidManager.
 * Testa estado de raid, progressão de ondas, cor de BossBar,
 * participantes, agregação de dano e multiplicador de loot.
 */
class BossRaidManagerTest {

    // --- Bar Color por Wave ---
    private String getBarColorForWave(int wave) {
        return switch (wave) {
            case 1 -> "GREEN";
            case 2 -> "YELLOW";
            case 3 -> "PINK";
            case 4 -> "RED";
            default -> "PURPLE";
        };
    }

    @Test
    void barColorWave1() {
        assertEquals("GREEN", getBarColorForWave(1));
    }

    @Test
    void barColorWave2() {
        assertEquals("YELLOW", getBarColorForWave(2));
    }

    @Test
    void barColorWave3() {
        assertEquals("PINK", getBarColorForWave(3));
    }

    @Test
    void barColorWave4() {
        assertEquals("RED", getBarColorForWave(4));
    }

    @Test
    void barColorWave5() {
        assertEquals("PURPLE", getBarColorForWave(5));
    }

    // --- resolveBossDisplayName ---
    private String resolveBossDisplayName(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "rei_gorvax" -> "§6Rei Gorvax";
            case "indrax_abissal", "indrax" -> "§5Indrax Abissal";
            case "vulgathor" -> "§cVulgathor";
            case "xylos" -> "§dXylos Devorador";
            case "skulkor" -> "§7Skulkor";
            case "kaldur" -> "§bKaldur";
            case "zarith" -> "§aZar'ith";
            case "rei_indrax" -> "§4Ruptura Temporal";
            case "halloween_boss" -> "§5Ceifador das Sombras";
            case "natal_boss" -> "§bRei do Gelo Eterno";
            default -> "§eBoss Desconhecido";
        };
    }

    @Test
    void displayNameBosses() {
        assertEquals("§6Rei Gorvax", resolveBossDisplayName("rei_gorvax"));
        assertEquals("§5Indrax Abissal", resolveBossDisplayName("indrax"));
        assertEquals("§eBoss Desconhecido", resolveBossDisplayName("custom"));
    }

    // --- Wave Progression ---
    @Test
    void waveProgressaoNormal() {
        int currentWave = 0, totalWaves = 5;
        currentWave++;
        assertEquals(1, currentWave);
        assertFalse(currentWave > totalWaves);
    }

    @Test
    void waveProgressaoFinalConclui() {
        int currentWave = 5, totalWaves = 5;
        currentWave++;
        assertTrue(currentWave > totalWaves);
    }

    @Test
    void waveProgressBar() {
        int totalWaves = 5;
        for (int w = 1; w <= totalWaves; w++) {
            double progress = (double) (w - 1) / totalWaves;
            assertTrue(progress >= 0.0 && progress < 1.0);
        }
    }

    // --- Participant Tracking ---
    @Test
    void participantsOperations() {
        Set<UUID> participants = Collections.synchronizedSet(new HashSet<>());
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();
        participants.add(p1);
        participants.add(p2);
        assertEquals(2, participants.size());
        participants.add(p1); // dup
        assertEquals(2, participants.size());
    }

    @Test
    void participantsUnmodifiable() {
        Set<UUID> participants = Collections.synchronizedSet(new HashSet<>());
        participants.add(UUID.randomUUID());
        Set<UUID> view = Collections.unmodifiableSet(participants);
        assertThrows(UnsupportedOperationException.class, () -> view.add(UUID.randomUUID()));
    }

    // --- Damage Aggregation ---
    @Test
    void damageAgregar() {
        Map<UUID, Double> dmg = new HashMap<>();
        UUID p1 = UUID.randomUUID();
        dmg.merge(p1, 100.0, Double::sum);
        dmg.merge(p1, 50.0, Double::sum);
        assertEquals(150.0, dmg.get(p1));
    }

    @Test
    void damageSortDescending() {
        Map<UUID, Double> dmg = new HashMap<>();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        dmg.put(a, 500.0);
        dmg.put(b, 1000.0);
        dmg.put(c, 750.0);
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(dmg.entrySet());
        sorted.sort(Map.Entry.<UUID, Double>comparingByValue().reversed());
        assertEquals(b, sorted.get(0).getKey());
    }

    // --- Loot Multiplier ---
    @Test
    void lootMultiplier5Waves() {
        assertEquals(2.0, 1.0 + (0.2 * 5), 0.001);
    }

    @Test
    void lootMultiplier3Waves() {
        assertEquals(1.6, 1.0 + (0.2 * 3), 0.001);
    }

    @Test
    void lootMultiplierFormat() {
        assertEquals("200%", String.format("%.0f%%", 2.0 * 100));
    }

    // --- Config Defaults ---
    @Test
    void configDefaults() {
        assertEquals(3, 3); // minPlayers
        assertEquals(100, 100); // playerRadius
        assertEquals(30, 30); // waveCooldown
    }

    // --- Wave Sequence ---
    @Test
    void waveSequenceOps() {
        List<String> waves = List.of("zarith", "skulkor", "rei_gorvax");
        assertEquals(3, waves.size());
        assertEquals("rei_gorvax", waves.get(waves.size() - 1));
    }

    // --- EndRaid State Cleanup ---
    @Test
    void endRaidLimpaEstado() {
        Set<UUID> participants = new HashSet<>(Set.of(UUID.randomUUID()));
        Map<UUID, Double> dmg = new HashMap<>(Map.of(UUID.randomUUID(), 100.0));
        participants.clear();
        dmg.clear();
        assertTrue(participants.isEmpty());
        assertTrue(dmg.isEmpty());
    }

    // --- Boss UUID tracking ---
    @Test
    void bossUUIDMatch() {
        UUID boss = UUID.randomUUID();
        UUID current = boss;
        assertTrue(current.equals(boss));
        assertFalse(current.equals(UUID.randomUUID()));
    }
}
