package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Encontro com Boss.
 * Managers envolvidos: BossManager (lógica), LootManager (lógica),
 * BossRaidManager (lógica), PlayerData (stats).
 *
 * Fluxo testado:
 * 1. Múltiplos jogadores causam dano a um boss
 * 2. Ranking de dano determinado corretamente
 * 3. Loot distribuído proporcionalmente ao rank
 * 4. Stats de boss atualizados em todos os participantes
 */
@Tag("integration")
class BossEncounterIT extends GorvaxIntegrationTest {

    // ===== Lógica replicada de damage tracking =====

    private final Map<UUID, Double> damageMap = new LinkedHashMap<>();

    private void recordDamage(UUID player, double amount) {
        damageMap.merge(player, amount, Double::sum);
    }

    private List<Map.Entry<UUID, Double>> getTopDamagers(int limit) {
        return damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ===== Lógica replicada de money reward (BossManager) =====

    private double calculateMoneyReward(int rank, double baseReward) {
        return switch (rank) {
            case 1 -> baseReward;
            case 2 -> baseReward * 0.75;
            case 3 -> baseReward * 0.50;
            default -> baseReward * 0.25;
        };
    }

    // ===== Lógica replicada de XP por rank (LootManager) =====

    private int calculateXpReward(int rank) {
        return switch (rank) {
            case 1 -> 500;
            case 2 -> 350;
            case 3 -> 250;
            default -> 100;
        };
    }

    // ===== Testes =====

    @Test
    void encontroCompletoBossCom3Jogadores() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        PlayerData pd1 = createPlayerData(p1);
        PlayerData pd2 = createPlayerData(p2);
        PlayerData pd3 = createPlayerData(p3);

        // Fase 1: jogadores causam dano ao boss
        recordDamage(p1, 1200.0); // Top 1
        recordDamage(p2, 800.0); // Top 2
        recordDamage(p3, 500.0); // Top 3
        recordDamage(p1, 300.0); // p1 continua causando dano

        // Fase 2: boss morre → calcular ranking
        var topDamagers = getTopDamagers(10);
        assertEquals(3, topDamagers.size());
        assertEquals(p1, topDamagers.get(0).getKey()); // 1500 total
        assertEquals(p2, topDamagers.get(1).getKey()); // 800 total
        assertEquals(p3, topDamagers.get(2).getKey()); // 500 total

        // Verificar dano total
        assertEquals(1500.0, topDamagers.get(0).getValue(), 0.01);
        assertEquals(800.0, topDamagers.get(1).getValue(), 0.01);
        assertEquals(500.0, topDamagers.get(2).getValue(), 0.01);

        // Fase 3: distribuir loot baseado em rank
        double baseReward = 1000.0;

        double reward1 = calculateMoneyReward(1, baseReward);
        double reward2 = calculateMoneyReward(2, baseReward);
        double reward3 = calculateMoneyReward(3, baseReward);

        assertEquals(1000.0, reward1, 0.01);
        assertEquals(750.0, reward2, 0.01);
        assertEquals(500.0, reward3, 0.01);

        // XP por rank
        assertEquals(500, calculateXpReward(1));
        assertEquals(350, calculateXpReward(2));
        assertEquals(250, calculateXpReward(3));

        // Fase 4: atualizar stats dos participantes
        pd1.addMoneyEarned(reward1);
        pd2.addMoneyEarned(reward2);
        pd3.addMoneyEarned(reward3);

        pd1.incrementBossesKilled();
        pd2.incrementBossesKilled();
        pd3.incrementBossesKilled();

        // Top 1 ganha incremento de topDamage
        pd1.incrementBossTopDamage();

        // Verificar stats finais
        assertEquals(1, pd1.getBossesKilled());
        assertEquals(1, pd2.getBossesKilled());
        assertEquals(1, pd3.getBossesKilled());
        assertEquals(1, pd1.getBossTopDamage());
        assertEquals(0, pd2.getBossTopDamage());
        assertEquals(1000.0, pd1.getTotalMoneyEarned(), 0.01);
    }

    @Test
    void jogadorForaDoTop3RecebeRecompensaMinima() {
        UUID p4 = UUID.randomUUID();

        recordDamage(UUID.randomUUID(), 1000.0);
        recordDamage(UUID.randomUUID(), 800.0);
        recordDamage(UUID.randomUUID(), 600.0);
        recordDamage(p4, 100.0); // rank 4+

        double reward = calculateMoneyReward(4, 1000.0);
        assertEquals(250.0, reward, 0.01);

        int xp = calculateXpReward(4);
        assertEquals(100, xp);
    }

    @Test
    void multiplosEncountrosAcumulamStats() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // 5 bosses mortos
        for (int i = 0; i < 5; i++) {
            pd.incrementBossesKilled();
            pd.addMoneyEarned(calculateMoneyReward(1, 1000.0));
        }

        assertEquals(5, pd.getBossesKilled());
        assertEquals(5000.0, pd.getTotalMoneyEarned(), 0.01);
    }

    @Test
    void topDamagersOrdenacaoComEmpate() {
        damageMap.clear();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        recordDamage(p1, 500.0);
        recordDamage(p2, 500.0);

        var top = getTopDamagers(10);
        assertEquals(2, top.size());
        // Ambos com 500 — ordem depende de inserção no LinkedHashMap
        assertEquals(500.0, top.get(0).getValue(), 0.01);
        assertEquals(500.0, top.get(1).getValue(), 0.01);
    }
}
