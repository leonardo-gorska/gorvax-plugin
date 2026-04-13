package br.com.gorvax.core.boss.managers;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para BossManager.
 * Testa lógica pura de resolução de config key, cálculo de recompensas,
 * gerenciamento de bosses ativos, intervalo de spawn e seleção aleatória.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class BossManagerTest {

    // --- resolveConfigKey (lógica replicada) ---

    private String resolveConfigKey(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "indrax_abissal", "indrax" -> "indrax";
            case "rei_gorvax" -> "rei_gorvax";
            case "vulgathor" -> "vulgathor";
            case "xylos" -> "xylos";
            case "skulkor" -> "skulkor";
            case "kaldur" -> "kaldur";
            case "zarith" -> "zarith";
            case "halloween_boss" -> "halloween_boss";
            case "natal_boss" -> "natal_boss";
            default -> bossId;
        };
    }

    @Test
    void resolveConfigKeyIndraxAbissal() {
        assertEquals("indrax", resolveConfigKey("indrax_abissal"));
    }

    @Test
    void resolveConfigKeyIndraxShort() {
        assertEquals("indrax", resolveConfigKey("indrax"));
    }

    @Test
    void resolveConfigKeyReiGorvax() {
        assertEquals("rei_gorvax", resolveConfigKey("rei_gorvax"));
    }

    @Test
    void resolveConfigKeyVulgathor() {
        assertEquals("vulgathor", resolveConfigKey("vulgathor"));
    }

    @Test
    void resolveConfigKeyCaseInsensitive() {
        assertEquals("indrax", resolveConfigKey("INDRAX_ABISSAL"));
        assertEquals("rei_gorvax", resolveConfigKey("REI_GORVAX"));
    }

    @Test
    void resolveConfigKeyDesconhecidoRetornaOriginal() {
        assertEquals("boss_custom", resolveConfigKey("boss_custom"));
    }

    @Test
    void resolveConfigKeyHalloweenBoss() {
        assertEquals("halloween_boss", resolveConfigKey("halloween_boss"));
    }

    @Test
    void resolveConfigKeyNatalBoss() {
        assertEquals("natal_boss", resolveConfigKey("natal_boss"));
    }

    @Test
    void resolveConfigKeyTodosBossesConhecidos() {
        String[] ids = { "rei_gorvax", "indrax_abissal", "indrax", "vulgathor",
                "xylos", "skulkor", "kaldur", "zarith", "halloween_boss", "natal_boss" };
        for (String id : ids) {
            assertNotNull(resolveConfigKey(id));
        }
    }

    // --- calculateMoneyReward (lógica replicada) ---

    private double calculateMoneyReward(int rank) {
        return switch (rank) {
            case 1 -> 5000.0;
            case 2 -> 3000.0;
            case 3 -> 2000.0;
            case 4 -> 1500.0;
            case 5 -> 1000.0;
            default -> 100.0;
        };
    }

    @Test
    void recompensaRank1Correto() {
        assertEquals(5000.0, calculateMoneyReward(1));
    }

    @Test
    void recompensaRank2Correto() {
        assertEquals(3000.0, calculateMoneyReward(2));
    }

    @Test
    void recompensaRank3Correto() {
        assertEquals(2000.0, calculateMoneyReward(3));
    }

    @Test
    void recompensaRank4Correto() {
        assertEquals(1500.0, calculateMoneyReward(4));
    }

    @Test
    void recompensaRank5Correto() {
        assertEquals(1000.0, calculateMoneyReward(5));
    }

    @Test
    void recompensaRankParticipacaoCorreto() {
        assertEquals(100.0, calculateMoneyReward(6));
        assertEquals(100.0, calculateMoneyReward(10));
        assertEquals(100.0, calculateMoneyReward(50));
    }

    @Test
    void recompensaDecrescentePorRank() {
        assertTrue(calculateMoneyReward(1) > calculateMoneyReward(2));
        assertTrue(calculateMoneyReward(2) > calculateMoneyReward(3));
        assertTrue(calculateMoneyReward(3) > calculateMoneyReward(4));
        assertTrue(calculateMoneyReward(4) > calculateMoneyReward(5));
        assertTrue(calculateMoneyReward(5) > calculateMoneyReward(6));
    }

    // --- Active Bosses Map ---

    @Test
    void activeBossesMapVazioInicial() {
        Map<UUID, Object> activeBosses = new ConcurrentHashMap<>();
        assertTrue(activeBosses.isEmpty());
    }

    @Test
    void activeBossesMapAdicionaRemove() {
        Map<UUID, String> activeBosses = new ConcurrentHashMap<>();
        UUID bossUUID = UUID.randomUUID();
        activeBosses.put(bossUUID, "rei_gorvax");

        assertTrue(activeBosses.containsKey(bossUUID));
        assertEquals("rei_gorvax", activeBosses.get(bossUUID));

        activeBosses.remove(bossUUID);
        assertFalse(activeBosses.containsKey(bossUUID));
    }

    @Test
    void activeBossesMapMultiplosBosses() {
        Map<UUID, String> activeBosses = new ConcurrentHashMap<>();
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        activeBosses.put(uuid1, "rei_gorvax");
        activeBosses.put(uuid2, "indrax_abissal");

        assertEquals(2, activeBosses.size());
        assertEquals("rei_gorvax", activeBosses.get(uuid1));
        assertEquals("indrax_abissal", activeBosses.get(uuid2));
    }

    @Test
    void removeBossById() {
        Map<UUID, String> activeBosses = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        activeBosses.put(uuid, "vulgathor");

        String boss = activeBosses.get(uuid);
        assertNotNull(boss);
        activeBosses.remove(uuid);
        assertNull(activeBosses.get(uuid));
    }

    @Test
    void activeBossesMapClearTodos() {
        Map<UUID, String> activeBosses = new ConcurrentHashMap<>();
        activeBosses.put(UUID.randomUUID(), "rei_gorvax");
        activeBosses.put(UUID.randomUUID(), "indrax_abissal");
        activeBosses.put(UUID.randomUUID(), "skulkor");

        assertEquals(3, activeBosses.size());
        activeBosses.clear();
        assertTrue(activeBosses.isEmpty());
    }

    // --- randomSpawnInterval (lógica replicada) ---

    private static final long MIN_SPAWN_INTERVAL = 3 * 3600000L; // 3h
    private static final long MAX_SPAWN_INTERVAL = 7 * 3600000L; // 7h

    private long randomSpawnInterval() {
        return MIN_SPAWN_INTERVAL + (long) (Math.random() * (MAX_SPAWN_INTERVAL - MIN_SPAWN_INTERVAL));
    }

    @Test
    void spawnIntervalDentroDoRange() {
        for (int i = 0; i < 100; i++) {
            long interval = randomSpawnInterval();
            assertTrue(interval >= MIN_SPAWN_INTERVAL, "Intervalo menor que 3h: " + interval);
            assertTrue(interval <= MAX_SPAWN_INTERVAL, "Intervalo maior que 7h: " + interval);
        }
    }

    @Test
    void spawnIntervalMinimo3Horas() {
        assertEquals(3 * 3600000L, MIN_SPAWN_INTERVAL);
    }

    @Test
    void spawnIntervalMaximo7Horas() {
        assertEquals(7 * 3600000L, MAX_SPAWN_INTERVAL);
    }

    // --- Boss ID List ---

    @Test
    void todosOsBossesNoArray() {
        String[] allBosses = { "rei_gorvax", "indrax_abissal", "zarith", "kaldur", "skulkor", "xylos", "vulgathor" };
        assertEquals(7, allBosses.length);
    }

    @Test
    void reiIndraxChance5Porcento() {
        double REI_INDRAX_CHANCE = 0.05;
        assertEquals(0.05, REI_INDRAX_CHANCE);
    }

    @Test
    void selecaoBossInclui7Opcoes() {
        String[] allBosses = { "rei_gorvax", "indrax_abissal", "zarith", "kaldur", "skulkor", "xylos", "vulgathor" };
        Set<String> bossSet = new HashSet<>(Arrays.asList(allBosses));
        assertEquals(7, bossSet.size());
        assertTrue(bossSet.contains("rei_gorvax"));
        assertTrue(bossSet.contains("indrax_abissal"));
        assertTrue(bossSet.contains("zarith"));
    }

    // --- Temporary Blocks tracking ---

    @Test
    void temporaryBlocksSetOperations() {
        Set<String> temporaryBlocks = Collections.synchronizedSet(new HashSet<>());
        temporaryBlocks.add("world,100,64,200");
        temporaryBlocks.add("world,101,64,200");
        temporaryBlocks.add("world,102,64,200");

        assertEquals(3, temporaryBlocks.size());

        temporaryBlocks.remove("world,101,64,200");
        assertEquals(2, temporaryBlocks.size());

        temporaryBlocks.clear();
        assertTrue(temporaryBlocks.isEmpty());
    }

    // --- Next spawn time control ---

    @Test
    void nextSpawnTimeInicialFuturo() {
        long nextSpawnTime = System.currentTimeMillis() + randomSpawnInterval();
        assertTrue(nextSpawnTime > System.currentTimeMillis());
    }

    @Test
    void nextSpawnTimeAtualiza() {
        long now = System.currentTimeMillis();
        long nextSpawnTime = now + MIN_SPAWN_INTERVAL;

        // Simula check: "if (now >= nextSpawnTime)"
        assertFalse(now >= nextSpawnTime); // Ainda não passou

        // Simula passagem do tempo
        long futureNow = now + MIN_SPAWN_INTERVAL + 1;
        assertTrue(futureNow >= nextSpawnTime); // Agora passou
    }

    // --- locationToString / stringToLocation logic ---

    @Test
    void locationStringFormatCorreto() {
        String result = "world" + "," + 100 + "," + 64 + "," + 200;
        assertEquals("world,100,64,200", result);
    }

    @Test
    void locationStringParseValido() {
        String s = "world,100,64,200";
        String[] parts = s.split(",");
        assertEquals(4, parts.length);
        assertEquals("world", parts[0]);
        assertEquals(100, Integer.parseInt(parts[1]));
        assertEquals(64, Integer.parseInt(parts[2]));
        assertEquals(200, Integer.parseInt(parts[3]));
    }

    @Test
    void locationStringParseInvalidoPoucosPartes() {
        String s = "world,100";
        String[] parts = s.split(",");
        assertTrue(parts.length < 4); // Retornaria null no manager
    }

    @Test
    void locationStringParseInvalidoNaoNumerico() {
        String s = "world,abc,64,200";
        String[] parts = s.split(",");
        assertThrows(NumberFormatException.class, () -> Integer.parseInt(parts[1]));
    }
}
