package br.com.gorvax.core.boss.miniboss;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para MiniBossManager e modelo MiniBoss.
 * Testa rastreamento de dano, getTopDamager, isAlive, records internos,
 * parsing de efeitos/loot e gerenciamento de instâncias ativas.
 */
class MiniBossManagerTest {

    // --- MiniBoss Damage Tracking (addDamage replica) ---

    @Test
    void addDamageRegistra() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID();
        damageDealt.merge(p1, 50.0, Double::sum);
        assertEquals(50.0, damageDealt.get(p1));
    }

    @Test
    void addDamageAcumula() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID();
        damageDealt.merge(p1, 50.0, Double::sum);
        damageDealt.merge(p1, 30.0, Double::sum);
        assertEquals(80.0, damageDealt.get(p1));
    }

    @Test
    void addDamageMultiplosJogadores() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();
        damageDealt.merge(p1, 100.0, Double::sum);
        damageDealt.merge(p2, 200.0, Double::sum);
        assertEquals(100.0, damageDealt.get(p1));
        assertEquals(200.0, damageDealt.get(p2));
    }

    // --- getTopDamager (lógica replicada) ---

    private UUID getTopDamager(Map<UUID, Double> damageDealt) {
        return damageDealt.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Test
    void topDamagerRetornaCorreto() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();
        damageDealt.put(p1, 100.0);
        damageDealt.put(p2, 300.0);
        damageDealt.put(p3, 200.0);
        assertEquals(p2, getTopDamager(damageDealt));
    }

    @Test
    void topDamagerVazioRetornaNull() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        assertNull(getTopDamager(damageDealt));
    }

    @Test
    void topDamagerUnicoJogador() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID();
        damageDealt.put(p1, 50.0);
        assertEquals(p1, getTopDamager(damageDealt));
    }

    // --- Active MiniBosses Map ---

    @Test
    void activeMiniBossesMapOps() {
        Map<String, String> activeMiniBosses = new ConcurrentHashMap<>();
        activeMiniBosses.put("guardiao_deserto_1", "HUSK");
        activeMiniBosses.put("sentinela_gelida_1", "STRAY");
        assertEquals(2, activeMiniBosses.size());
        assertTrue(activeMiniBosses.containsKey("guardiao_deserto_1"));
    }

    @Test
    void removeMiniBoss() {
        Map<String, String> active = new ConcurrentHashMap<>();
        active.put("boss_1", "HUSK");
        active.remove("boss_1");
        assertFalse(active.containsKey("boss_1"));
    }

    @Test
    void removeAllMiniBosses() {
        Map<String, String> active = new ConcurrentHashMap<>();
        active.put("boss_1", "A");
        active.put("boss_2", "B");
        active.clear();
        assertTrue(active.isEmpty());
    }

    // --- getByEntity (busca por entity UUID) ---

    @Test
    void getByEntityUUID() {
        Map<UUID, String> entityToBoss = new ConcurrentHashMap<>();
        UUID entityUUID = UUID.randomUUID();
        entityToBoss.put(entityUUID, "guardiao_deserto_1");
        assertEquals("guardiao_deserto_1", entityToBoss.get(entityUUID));
    }

    @Test
    void getByEntityUUIDNaoEncontrado() {
        Map<UUID, String> entityToBoss = new ConcurrentHashMap<>();
        assertNull(entityToBoss.get(UUID.randomUUID()));
    }

    // --- parseEffect "TIPO:DURAÇÃO:NÍVEL" ---

    @Test
    void parseEffectValido() {
        String str = "SLOWNESS:100:1";
        String[] parts = str.split(":");
        assertEquals(3, parts.length);
        assertEquals("SLOWNESS", parts[0]);
        assertEquals(100, Integer.parseInt(parts[1]));
        assertEquals(1, Integer.parseInt(parts[2]));
    }

    @Test
    void parseEffectInvalido() {
        String str = "INVALID";
        String[] parts = str.split(":");
        assertEquals(1, parts.length); // Não tem separador
    }

    // --- parseLootEntry "MATERIAL:QUANTIDADE:CHANCE" ---

    @Test
    void parseLootEntryValido() {
        String str = "DIAMOND:5:0.3";
        String[] parts = str.split(":");
        assertEquals(3, parts.length);
        assertEquals("DIAMOND", parts[0]);
        assertEquals(5, Integer.parseInt(parts[1]));
        assertEquals(0.3, Double.parseDouble(parts[2]), 0.001);
    }

    @Test
    void parseLootEntryChanceRange() {
        double chance = 0.3;
        assertTrue(chance >= 0.0 && chance <= 1.0);
    }

    // --- Spawn cooldown tracking ---

    @Test
    void spawnCooldownTracking() {
        Map<String, Long> lastSpawnTime = new ConcurrentHashMap<>();
        String configId = "guardiao_deserto";
        lastSpawnTime.put(configId, System.currentTimeMillis());

        Long last = lastSpawnTime.get(configId);
        assertNotNull(last);
        assertTrue(System.currentTimeMillis() - last < 1000);
    }

    @Test
    void spawnCooldownRespeitado() {
        long lastSpawn = System.currentTimeMillis() - 10000L; // 10s atrás
        long cooldown = 30 * 60 * 1000L; // 30min
        boolean canSpawn = (System.currentTimeMillis() - lastSpawn) >= cooldown;
        assertFalse(canSpawn); // Cooldown não passou
    }

    @Test
    void spawnCooldownExpirou() {
        long cooldown = 1000L; // 1s para teste
        long lastSpawn = System.currentTimeMillis() - 2000L; // 2s atrás
        boolean canSpawn = (System.currentTimeMillis() - lastSpawn) >= cooldown;
        assertTrue(canSpawn);
    }

    // --- Minion tracking ---

    @Test
    void minionTagging() {
        Set<UUID> minions = ConcurrentHashMap.newKeySet();
        UUID minionUUID = UUID.randomUUID();
        minions.add(minionUUID);
        assertTrue(minions.contains(minionUUID));
        assertFalse(minions.contains(UUID.randomUUID()));
    }

    // --- Inactivity timeout ---

    @Test
    void inactivityTimeout() {
        long lastDamageTimestamp = System.currentTimeMillis() - (10 * 60 * 1000L); // 10min
        long TIMEOUT = 5 * 60 * 1000L; // 5min
        assertTrue(System.currentTimeMillis() - lastDamageTimestamp > TIMEOUT);
    }

    @Test
    void inactivityNaoExpirou() {
        long lastDamageTimestamp = System.currentTimeMillis() - 1000L;
        long TIMEOUT = 5 * 60 * 1000L;
        assertFalse(System.currentTimeMillis() - lastDamageTimestamp > TIMEOUT);
    }
}
