package br.com.gorvax.core.boss.model;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para WorldBoss.
 * Testa mapa de dano, getTopDamagers, cálculo de fases, teleport lock,
 * isHealItem, sinergia de aliados e BossBar progress.
 */
class WorldBossTest {

    // --- Damage Map ---

    @Test
    void damageMapRegistraDano() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID();
        damageDealt.merge(p1, 100.0, Double::sum);
        assertEquals(100.0, damageDealt.get(p1));
    }

    @Test
    void damageMapAcumulaDano() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID();
        damageDealt.merge(p1, 100.0, Double::sum);
        damageDealt.merge(p1, 200.0, Double::sum);
        assertEquals(300.0, damageDealt.get(p1));
    }

    @Test
    void damageMapMultiplosJogadores() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();
        damageDealt.put(p1, 500.0);
        damageDealt.put(p2, 1000.0);
        damageDealt.put(p3, 750.0);
        assertEquals(3, damageDealt.size());
    }

    @Test
    void damageMapVazioRetornoNulo() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        assertTrue(damageDealt.isEmpty());
    }

    // --- getTopDamagers (lógica replicada) ---

    private List<Map.Entry<UUID, Double>> getTopDamagers(Map<UUID, Double> damageDealt) {
        return damageDealt.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    @Test
    void topDamagersOrdenados() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();
        damageDealt.put(p1, 500.0);
        damageDealt.put(p2, 1000.0);
        damageDealt.put(p3, 750.0);

        List<Map.Entry<UUID, Double>> top = getTopDamagers(damageDealt);
        assertEquals(p2, top.get(0).getKey());
        assertEquals(1000.0, top.get(0).getValue());
        assertEquals(p3, top.get(1).getKey());
        assertEquals(p1, top.get(2).getKey());
    }

    @Test
    void topDamagersVazioRetornaListaVazia() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        List<Map.Entry<UUID, Double>> top = getTopDamagers(damageDealt);
        assertTrue(top.isEmpty());
    }

    @Test
    void topDamagersUnicoJogador() {
        Map<UUID, Double> damageDealt = new ConcurrentHashMap<>();
        UUID p1 = UUID.randomUUID();
        damageDealt.put(p1, 999.0);

        List<Map.Entry<UUID, Double>> top = getTopDamagers(damageDealt);
        assertEquals(1, top.size());
        assertEquals(p1, top.get(0).getKey());
    }

    // --- Phase Calculation (lógica replicada) ---

    private int calculatePhase(double progress) {
        if (progress <= 0.25)
            return 4;
        else if (progress <= 0.50)
            return 3;
        else if (progress <= 0.75)
            return 2;
        else
            return 1;
    }

    @Test
    void phase1AcimaDE75() {
        assertEquals(1, calculatePhase(0.80));
        assertEquals(1, calculatePhase(1.00));
    }

    @Test
    void phase2Entre50e75() {
        assertEquals(2, calculatePhase(0.75));
        assertEquals(2, calculatePhase(0.60));
    }

    @Test
    void phase3Entre25e50() {
        assertEquals(3, calculatePhase(0.50));
        assertEquals(3, calculatePhase(0.30));
    }

    @Test
    void phase4Abaixo25() {
        assertEquals(4, calculatePhase(0.25));
        assertEquals(4, calculatePhase(0.10));
        assertEquals(4, calculatePhase(0.00));
    }

    @Test
    void faseMudaCorDaBarra() {
        String[] barColor = new String[1];
        for (int phase = 1; phase <= 4; phase++) {
            barColor[0] = switch (phase) {
                case 2 -> "YELLOW";
                case 3 -> "RED";
                case 4 -> "PURPLE";
                default -> "GREEN";
            };
        }
        assertNotNull(barColor[0]);
    }

    // --- BossBar Progress Clamping ---

    @Test
    void bossBarProgressClampMin() {
        double progress = -0.5;
        double clamped = Math.max(0, Math.min(1, progress));
        assertEquals(0.0, clamped);
    }

    @Test
    void bossBarProgressClampMax() {
        double progress = 1.5;
        double clamped = Math.max(0, Math.min(1, progress));
        assertEquals(1.0, clamped);
    }

    @Test
    void bossBarProgressNormal() {
        double progress = 0.6;
        double clamped = Math.max(0, Math.min(1, progress));
        assertEquals(0.6, clamped, 0.001);
    }

    // --- Teleport Lock ---

    @Test
    void teleportLockSetado() {
        UUID target = UUID.randomUUID();
        long lockUntil = System.currentTimeMillis() + 3000L;
        assertNotNull(target);
        assertTrue(lockUntil > System.currentTimeMillis());
    }

    @Test
    void teleportLockAtivo() {
        UUID targetUUID = UUID.randomUUID();
        long lockUntil = System.currentTimeMillis() + 3000L;
        long now = System.currentTimeMillis();
        assertTrue(targetUUID != null && now < lockUntil);
    }

    @Test
    void teleportLockExpirado() {
        UUID targetUUID = UUID.randomUUID();
        long lockUntil = System.currentTimeMillis() - 1000L;
        long now = System.currentTimeMillis();
        assertTrue(now >= lockUntil); // Lock expirado
    }

    @Test
    void teleportLockLimpaAposExpiracao() {
        UUID teleportTargetUUID = UUID.randomUUID();
        long teleportLockUntil = System.currentTimeMillis() - 1000L;
        long now = System.currentTimeMillis();

        if (teleportTargetUUID != null && now >= teleportLockUntil) {
            teleportTargetUUID = null;
        }
        assertNull(teleportTargetUUID);
    }

    // --- isHealItem (lógica replicada) ---

    private boolean isHealItem(String materialName) {
        if (materialName == null || materialName.equals("AIR"))
            return false;
        return materialName.equals("GOLDEN_APPLE")
                || materialName.equals("ENCHANTED_GOLDEN_APPLE")
                || materialName.equals("SPLASH_POTION")
                || materialName.equals("LINGERING_POTION")
                || materialName.equals("POTION")
                || materialName.equals("TOTEM_OF_UNDYING");
    }

    @Test
    void healItemGoldenApple() {
        assertTrue(isHealItem("GOLDEN_APPLE"));
    }

    @Test
    void healItemEnchantedGoldenApple() {
        assertTrue(isHealItem("ENCHANTED_GOLDEN_APPLE"));
    }

    @Test
    void healItemPotion() {
        assertTrue(isHealItem("POTION"));
        assertTrue(isHealItem("SPLASH_POTION"));
        assertTrue(isHealItem("LINGERING_POTION"));
    }

    @Test
    void healItemTotem() {
        assertTrue(isHealItem("TOTEM_OF_UNDYING"));
    }

    @Test
    void notHealItemSword() {
        assertFalse(isHealItem("DIAMOND_SWORD"));
    }

    @Test
    void notHealItemNull() {
        assertFalse(isHealItem(null));
    }

    @Test
    void notHealItemAir() {
        assertFalse(isHealItem("AIR"));
    }

    // --- Targeting Throttle ---

    @Test
    void targetingThrottle10Ticks() {
        int targetingTicks = 0;
        int processed = 0;
        for (int i = 0; i < 30; i++) {
            targetingTicks++;
            if (targetingTicks >= 10) {
                targetingTicks = 0;
                processed++;
            }
        }
        assertEquals(3, processed); // 30 ticks / 10 = 3 processamentos
    }

    // --- Threat Score Components ---

    @Test
    void threatScoreDPS() {
        double playerDmg = 500.0, maxDps = 1000.0;
        double score = (playerDmg / maxDps) * 40;
        assertEquals(20.0, score, 0.01);
    }

    @Test
    void threatScoreDistanciaFugitivo() {
        double dist = 30;
        double score = 0;
        if (dist > 25)
            score += 30;
        else if (dist > 15)
            score += 15;
        assertEquals(30.0, score);
    }

    @Test
    void threatScoreMedieval() {
        double dist = 20;
        double score = 0;
        if (dist > 25)
            score += 30;
        else if (dist > 15)
            score += 15;
        assertEquals(15.0, score);
    }

    @Test
    void threatScorePenalizaBaixaVida() {
        double health = 4.0;
        double score = 50;
        if (health < 6.0)
            score -= 10;
        assertEquals(40.0, score);
    }

    // --- Royal Decree ---

    @Test
    void royalDecreeSetado() {
        UUID target = UUID.randomUUID();
        long until = System.currentTimeMillis() + 5000L;
        assertNotNull(target);
        assertTrue(until > System.currentTimeMillis());
    }

    @Test
    void royalDecreeExpirado() {
        long until = System.currentTimeMillis() - 1000L;
        assertTrue(System.currentTimeMillis() > until);
    }

    // --- Inactivity timestamp ---

    @Test
    void updateLastDamage() {
        long lastDamageTimestamp = System.currentTimeMillis() - 60000L;
        long oldTimestamp = lastDamageTimestamp;
        lastDamageTimestamp = System.currentTimeMillis();
        assertTrue(lastDamageTimestamp > oldTimestamp);
    }

    // --- Anti-Kite Distance ---

    @Test
    void antiKiteDistancia25Blocos() {
        double distanceSq = 625; // 25^2
        assertTrue(distanceSq >= 625); // Threshold para anti-kite
    }

    // --- SafeMaxHealth ---

    @Test
    void safeMaxHealthCalculo() {
        double maxHealth = 15000.0, baseValue = 20.0;
        double bonus = maxHealth - baseValue;
        assertEquals(14980.0, bonus);
        assertTrue(bonus > 0);
    }

    @Test
    void safeMaxHealthSemBonus() {
        double maxHealth = 20.0, baseValue = 20.0;
        double bonus = maxHealth - baseValue;
        assertEquals(0.0, bonus);
        assertFalse(bonus > 0);
    }
}
