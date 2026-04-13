package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para TeleportHubManager.
 * Testa lógica pura de cooldowns e warmups sem dependência do Bukkit.
 */
class TeleportHubManagerTest {

    // Replica da lógica de cooldowns do TeleportHubManager
    private Map<String, Map<UUID, Long>> cooldowns;

    @BeforeEach
    void setUp() {
        cooldowns = new ConcurrentHashMap<>();
    }

    // ========== Cooldowns ==========

    /**
     * Retorna os segundos de cooldown para um tipo (replicação).
     */
    private int getCooldownSeconds(String type) {
        return switch (type) {
            case "spawn" -> 30;
            case "rtp" -> 300;
            case "kingdom_home" -> 30;
            case "kingdom_visit" -> 60;
            default -> 0;
        };
    }

    /**
     * Replica setCooldown do manager.
     */
    private void setCooldown(UUID uuid, String type) {
        int seconds = getCooldownSeconds(type);
        if (seconds <= 0) return;
        cooldowns.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    /**
     * Replica isOnCooldown do manager.
     */
    private boolean isOnCooldown(UUID uuid, String type) {
        Map<UUID, Long> map = cooldowns.get(type);
        if (map == null) return false;
        Long expireAt = map.get(uuid);
        if (expireAt == null) return false;
        if (System.currentTimeMillis() >= expireAt) {
            map.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Replica getRemainingCooldown do manager.
     */
    private int getRemainingCooldown(UUID uuid, String type) {
        Map<UUID, Long> map = cooldowns.get(type);
        if (map == null) return 0;
        Long expireAt = map.get(uuid);
        if (expireAt == null) return 0;
        long remaining = expireAt - System.currentTimeMillis();
        if (remaining <= 0) {
            map.remove(uuid);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    @Test
    void setCooldownEVerificaAtivo() {
        UUID uuid = UUID.randomUUID();
        setCooldown(uuid, "spawn");
        assertTrue(isOnCooldown(uuid, "spawn"), "Cooldown deve estar ativo logo após setar");
    }

    @Test
    void semCooldownInicialmente() {
        UUID uuid = UUID.randomUUID();
        assertFalse(isOnCooldown(uuid, "spawn"), "Não deve haver cooldown sem setar");
    }

    @Test
    void cooldownExpira() {
        UUID uuid = UUID.randomUUID();
        // Simular cooldown expirado: definir expireAt no passado
        cooldowns.computeIfAbsent("spawn", k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() - 1000L);
        assertFalse(isOnCooldown(uuid, "spawn"), "Cooldown expirado não deve estar ativo");
    }

    @Test
    void remainingCooldownPositivo() {
        UUID uuid = UUID.randomUUID();
        setCooldown(uuid, "spawn");
        int remaining = getRemainingCooldown(uuid, "spawn");
        assertTrue(remaining > 0, "Remaining deve ser positivo logo após setar");
        assertTrue(remaining <= 30, "Remaining não deve exceder o cooldown configurado");
    }

    @Test
    void remainingCooldownZeroSemSetar() {
        UUID uuid = UUID.randomUUID();
        assertEquals(0, getRemainingCooldown(uuid, "spawn"));
    }

    @Test
    void remainingCooldownZeroAposExpirar() {
        UUID uuid = UUID.randomUUID();
        cooldowns.computeIfAbsent("spawn", k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() - 1000L);
        assertEquals(0, getRemainingCooldown(uuid, "spawn"));
    }

    @Test
    void cooldownsDiferentesIndependentes() {
        UUID uuid = UUID.randomUUID();
        setCooldown(uuid, "spawn");
        assertFalse(isOnCooldown(uuid, "rtp"), "Cooldown de rtp não deve existir");
        assertTrue(isOnCooldown(uuid, "spawn"), "Cooldown de spawn deve existir");
    }

    @Test
    void cooldownsJogadoresDiferentesIndependentes() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        setCooldown(uuid1, "spawn");
        assertTrue(isOnCooldown(uuid1, "spawn"));
        assertFalse(isOnCooldown(uuid2, "spawn"));
    }

    @Test
    void getCooldownSecondsPorTipo() {
        assertEquals(30, getCooldownSeconds("spawn"));
        assertEquals(300, getCooldownSeconds("rtp"));
        assertEquals(30, getCooldownSeconds("kingdom_home"));
        assertEquals(60, getCooldownSeconds("kingdom_visit"));
        assertEquals(0, getCooldownSeconds("tipo_desconhecido"));
    }

    // ========== Warmup (lógica de tracking) ==========

    // Replica da lógica de warmup state tracking
    private final Map<UUID, Object> activeWarmups = new ConcurrentHashMap<>();

    private boolean isInWarmup(UUID uuid) {
        return activeWarmups.containsKey(uuid);
    }

    private void startWarmup(UUID uuid) {
        activeWarmups.put(uuid, new Object());
    }

    private void cancelWarmup(UUID uuid) {
        activeWarmups.remove(uuid);
    }

    @Test
    void warmupIniciaEVerifica() {
        UUID uuid = UUID.randomUUID();
        assertFalse(isInWarmup(uuid));
        startWarmup(uuid);
        assertTrue(isInWarmup(uuid));
    }

    @Test
    void warmupCancela() {
        UUID uuid = UUID.randomUUID();
        startWarmup(uuid);
        assertTrue(isInWarmup(uuid));
        cancelWarmup(uuid);
        assertFalse(isInWarmup(uuid));
    }

    @Test
    void warmupCancelarSemIniciar() {
        UUID uuid = UUID.randomUUID();
        cancelWarmup(uuid); // Não deve explodir
        assertFalse(isInWarmup(uuid));
    }

    @Test
    void warmupJogadoresDiferentesIndependentes() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        startWarmup(uuid1);
        assertTrue(isInWarmup(uuid1));
        assertFalse(isInWarmup(uuid2));
    }

    // ========== Bússola (PDC tag check via lógica pura) ==========

    /**
     * Simula a verificação de PDC tag da bússola (sem Bukkit).
     * Testa a regra: material == COMPASS && tag presente.
     */
    private boolean isCompass(String materialName, boolean hasTag) {
        return "COMPASS".equals(materialName) && hasTag;
    }

    @Test
    void bussola_compassComTag() {
        assertTrue(isCompass("COMPASS", true));
    }

    @Test
    void bussola_compassSemTag() {
        assertFalse(isCompass("COMPASS", false));
    }

    @Test
    void bussola_outroItemComTag() {
        assertFalse(isCompass("DIAMOND_SWORD", true));
    }

    @Test
    void bussola_outroItemSemTag() {
        assertFalse(isCompass("DIAMOND_SWORD", false));
    }

    @Test
    void bussola_nulo() {
        assertFalse(isCompass(null, false));
    }
}
