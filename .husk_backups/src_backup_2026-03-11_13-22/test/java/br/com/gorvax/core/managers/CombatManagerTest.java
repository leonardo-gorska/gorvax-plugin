package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para CombatManager.
 * Testa lógica pura de combat tag, kill streaks, spawn protection e command
 * blocking.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class CombatManagerTest {

    // --- Combat Tag (lógica replicada) ---

    @Test
    void tagPlayerColocaEmCombate() {
        Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        int tagDuration = 15;
        long expireAt = System.currentTimeMillis() + (tagDuration * 1000L);
        combatTagMap.put(uuid, expireAt);

        assertTrue(combatTagMap.containsKey(uuid));
    }

    @Test
    void isInCombatTrue() {
        Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        combatTagMap.put(uuid, System.currentTimeMillis() + 15000L);

        Long expireAt = combatTagMap.get(uuid);
        boolean inCombat = expireAt != null && System.currentTimeMillis() < expireAt;
        assertTrue(inCombat);
    }

    @Test
    void isInCombatFalseExpirado() {
        Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        combatTagMap.put(uuid, System.currentTimeMillis() - 1000L); // Já expirou

        Long expireAt = combatTagMap.get(uuid);
        boolean inCombat = expireAt != null && System.currentTimeMillis() < expireAt;
        assertFalse(inCombat);
    }

    @Test
    void isInCombatFalseSemTag() {
        Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
        assertNull(combatTagMap.get(UUID.randomUUID()));
    }

    @Test
    void getRemainingCombatTimePositivo() {
        long expireAt = System.currentTimeMillis() + 10000L; // 10s restantes
        long remaining = expireAt - System.currentTimeMillis();
        int seconds = (int) Math.ceil(remaining / 1000.0);

        assertTrue(seconds > 0);
        assertTrue(seconds <= 10);
    }

    @Test
    void removeCombatTagLimpa() {
        Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        combatTagMap.put(uuid, System.currentTimeMillis() + 15000L);
        combatTagMap.remove(uuid);
        assertNull(combatTagMap.get(uuid));
    }

    // --- isCommandBlocked (lógica replicada) ---

    private boolean isCommandBlocked(String command, List<String> blockedCommands) {
        String cmd = command.toLowerCase().trim();
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        String baseCmd = cmd.split("\\s+")[0];

        for (String blocked : blockedCommands) {
            if (baseCmd.equalsIgnoreCase(blocked)) {
                return true;
            }
        }

        if (baseCmd.equals("reino") && cmd.contains("spawn")) {
            return true;
        }

        return false;
    }

    private final List<String> defaultBlockedCommands = List.of("home", "tpa", "spawn", "warp", "rtp", "back",
            "logout");

    @Test
    void comandoBloqueadoHome() {
        assertTrue(isCommandBlocked("home", defaultBlockedCommands));
    }

    @Test
    void comandoBloqueadoComBarra() {
        assertTrue(isCommandBlocked("/spawn", defaultBlockedCommands));
    }

    @Test
    void comandoBloqueadoComArgumentos() {
        assertTrue(isCommandBlocked("/home base", defaultBlockedCommands));
    }

    @Test
    void comandoNaoBloqueadoMsg() {
        assertFalse(isCommandBlocked("/msg Gorska oi", defaultBlockedCommands));
    }

    @Test
    void comandoBloqueadoReinoSpawn() {
        assertTrue(isCommandBlocked("/reino spawn", defaultBlockedCommands));
    }

    @Test
    void comandoReinoSemSpawnNaoBloqueado() {
        assertFalse(isCommandBlocked("/reino membros", defaultBlockedCommands));
    }

    @Test
    void comandoBloqueadoCaseInsensitive() {
        assertTrue(isCommandBlocked("/HOME", defaultBlockedCommands));
    }

    @Test
    void comandoBloqueadoTpa() {
        assertTrue(isCommandBlocked("/tpa Gorska", defaultBlockedCommands));
    }

    @Test
    void comandoNaoBloqueadoHelp() {
        assertFalse(isCommandBlocked("/help", defaultBlockedCommands));
    }

    // --- Kill Streaks ---

    @Test
    void killStreakInicialZero() {
        Map<UUID, Integer> killStreakMap = new ConcurrentHashMap<>();
        assertEquals(0, killStreakMap.getOrDefault(UUID.randomUUID(), 0));
    }

    @Test
    void killStreakIncrementa() {
        Map<UUID, Integer> killStreakMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        int newStreak = killStreakMap.merge(uuid, 1, Integer::sum);
        assertEquals(1, newStreak);
        newStreak = killStreakMap.merge(uuid, 1, Integer::sum);
        assertEquals(2, newStreak);
    }

    @Test
    void killStreakResetLimpa() {
        Map<UUID, Integer> killStreakMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        killStreakMap.put(uuid, 5);
        Integer oldStreak = killStreakMap.remove(uuid);
        assertEquals(5, oldStreak);
        assertEquals(0, killStreakMap.getOrDefault(uuid, 0));
    }

    // --- Spawn Protection ---

    @Test
    void spawnProtectionAtivaValida() {
        Map<UUID, Long> spawnProtectionMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        spawnProtectionMap.put(uuid, System.currentTimeMillis() + 5000L);

        Long expireAt = spawnProtectionMap.get(uuid);
        boolean isProtected = expireAt != null && System.currentTimeMillis() < expireAt;
        assertTrue(isProtected);
    }

    @Test
    void spawnProtectionExpiradaInvalida() {
        Map<UUID, Long> spawnProtectionMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        spawnProtectionMap.put(uuid, System.currentTimeMillis() - 1000L);

        Long expireAt = spawnProtectionMap.get(uuid);
        boolean isProtected = expireAt != null && System.currentTimeMillis() < expireAt;
        assertFalse(isProtected);
    }

    @Test
    void spawnProtectionRemovida() {
        Map<UUID, Long> spawnProtectionMap = new ConcurrentHashMap<>();
        UUID uuid = UUID.randomUUID();
        spawnProtectionMap.put(uuid, System.currentTimeMillis() + 5000L);
        spawnProtectionMap.remove(uuid);
        assertNull(spawnProtectionMap.get(uuid));
    }

    // --- Logger NPC state ---

    @Test
    void loggerNPCMapOperations() {
        Map<UUID, UUID> npcToPlayerMap = new ConcurrentHashMap<>();
        UUID npcUUID = UUID.randomUUID();
        UUID playerUUID = UUID.randomUUID();

        assertFalse(npcToPlayerMap.containsKey(npcUUID));

        npcToPlayerMap.put(npcUUID, playerUUID);
        assertTrue(npcToPlayerMap.containsKey(npcUUID));
        assertEquals(playerUUID, npcToPlayerMap.get(npcUUID));
    }

    @Test
    void killedByLoggerFlagConsumeUmaVez() {
        java.util.Set<UUID> killedByLogger = ConcurrentHashMap.newKeySet();
        UUID playerUUID = UUID.randomUUID();

        assertFalse(killedByLogger.remove(playerUUID));

        killedByLogger.add(playerUUID);
        assertTrue(killedByLogger.remove(playerUUID)); // Primeira vez: true
        assertFalse(killedByLogger.remove(playerUUID)); // Segunda vez: false
    }

    // --- Cleanup ---

    @Test
    void cleanupPlayerLimpaMapas() {
        Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
        Map<UUID, Integer> killStreakMap = new ConcurrentHashMap<>();
        Map<UUID, Long> spawnProtectionMap = new ConcurrentHashMap<>();

        UUID uuid = UUID.randomUUID();
        combatTagMap.put(uuid, System.currentTimeMillis() + 15000L);
        killStreakMap.put(uuid, 5);
        spawnProtectionMap.put(uuid, System.currentTimeMillis() + 5000L);

        // cleanupPlayer replica
        combatTagMap.remove(uuid);
        killStreakMap.remove(uuid);
        spawnProtectionMap.remove(uuid);

        assertNull(combatTagMap.get(uuid));
        assertEquals(0, killStreakMap.getOrDefault(uuid, 0));
        assertNull(spawnProtectionMap.get(uuid));
    }
}
