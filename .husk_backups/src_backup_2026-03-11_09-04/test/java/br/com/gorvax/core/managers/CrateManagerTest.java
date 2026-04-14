package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para CrateManager.
 * Testa lógica pura dos records CrateType/CrateReward e weighted random.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class CrateManagerTest {

    // --- CrateReward record ---

    @Test
    void crateRewardRecord() {
        var reward = new CrateManager.CrateReward(
                "money", 100.0, 50, "§a$100", null, null, null, null);
        assertEquals("money", reward.type());
        assertEquals(100.0, reward.amount(), 0.001);
        assertEquals(50, reward.weight());
        assertEquals("§a$100", reward.display());
        assertNull(reward.material());
        assertNull(reward.itemId());
        assertNull(reward.title());
        assertNull(reward.crateKeyType());
    }

    @Test
    void crateRewardEquality() {
        var a = new CrateManager.CrateReward("money", 100, 50, "§a$100", null, null, null, null);
        var b = new CrateManager.CrateReward("money", 100, 50, "§a$100", null, null, null, null);
        assertEquals(a, b);
    }

    // --- CrateType record ---

    @Test
    void crateTypeRecord() {
        List<CrateManager.CrateReward> rewards = List.of(
                new CrateManager.CrateReward("money", 100, 50, "display", null, null, null, null),
                new CrateManager.CrateReward("money", 500, 10, "display2", null, null, null, null));

        var crate = new CrateManager.CrateType("raro", "§bCrate Rara", null, "§b", true, rewards, 60);
        assertEquals("raro", crate.id());
        assertEquals("§bCrate Rara", crate.name());
        assertEquals("§b", crate.color());
        assertTrue(crate.broadcastOnOpen());
        assertEquals(2, crate.rewards().size());
        assertEquals(60, crate.totalWeight());
    }

    // --- Weighted random (lógica replicada) ---

    /**
     * Replica a lógica de sorteio de getRandomReward para testar sem Bukkit.
     */
    private CrateManager.CrateReward getRandomReward(List<CrateManager.CrateReward> rewards, int totalWeight) {
        if (rewards.isEmpty())
            return null;
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (CrateManager.CrateReward reward : rewards) {
            cumulative += reward.weight();
            if (roll < cumulative) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    @Test
    void weightedRandomNuncaRetornaNull() {
        List<CrateManager.CrateReward> rewards = List.of(
                new CrateManager.CrateReward("money", 100, 50, "d1", null, null, null, null),
                new CrateManager.CrateReward("money", 500, 10, "d2", null, null, null, null));

        for (int i = 0; i < 100; i++) {
            CrateManager.CrateReward result = getRandomReward(rewards, 60);
            assertNotNull(result);
        }
    }

    @Test
    void weightedRandomListaVazia() {
        assertNull(getRandomReward(List.of(), 0));
    }

    @Test
    void weightedRandomUnicoItem() {
        var single = new CrateManager.CrateReward("title", 0, 100, "d", null, null, "Título", null);
        List<CrateManager.CrateReward> rewards = List.of(single);

        for (int i = 0; i < 50; i++) {
            assertEquals(single, getRandomReward(rewards, 100));
        }
    }

    @RepeatedTest(10)
    void weightedRandomDistribuicaoRazoavel() {
        var common = new CrateManager.CrateReward("money", 100, 90, "d1", null, null, null, null);
        var rare = new CrateManager.CrateReward("money", 1000, 10, "d2", null, null, null, null);
        List<CrateManager.CrateReward> rewards = List.of(common, rare);

        int commonCount = 0;
        int rareCount = 0;
        int runs = 1000;

        for (int i = 0; i < runs; i++) {
            CrateManager.CrateReward result = getRandomReward(rewards, 100);
            if (result.equals(common))
                commonCount++;
            else
                rareCount++;
        }

        // Com peso 90/10, esperamos ~900/100
        assertTrue(commonCount > 750, "Common deveria aparecer > 750 vezes, foi: " + commonCount);
        assertTrue(rareCount > 30, "Rare deveria aparecer > 30 vezes, foi: " + rareCount);
        assertTrue(rareCount < 200, "Rare deveria aparecer < 200 vezes, foi: " + rareCount);
    }

    // --- Teste deterministico de roll fixo (simula sem RNG) ---

    private CrateManager.CrateReward getRewardForRoll(List<CrateManager.CrateReward> rewards, int roll) {
        int cumulative = 0;
        for (CrateManager.CrateReward reward : rewards) {
            cumulative += reward.weight();
            if (roll < cumulative) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    @Test
    void rollZeroRetornaPrimeiro() {
        var r1 = new CrateManager.CrateReward("money", 100, 30, "d1", null, null, null, null);
        var r2 = new CrateManager.CrateReward("money", 500, 70, "d2", null, null, null, null);
        assertEquals(r1, getRewardForRoll(List.of(r1, r2), 0));
    }

    @Test
    void rollNoLimiteDoSegundo() {
        var r1 = new CrateManager.CrateReward("money", 100, 30, "d1", null, null, null, null);
        var r2 = new CrateManager.CrateReward("money", 500, 70, "d2", null, null, null, null);
        assertEquals(r2, getRewardForRoll(List.of(r1, r2), 30));
    }

    @Test
    void rollMaximoRetornaUltimo() {
        var r1 = new CrateManager.CrateReward("money", 100, 30, "d1", null, null, null, null);
        var r2 = new CrateManager.CrateReward("money", 500, 70, "d2", null, null, null, null);
        assertEquals(r2, getRewardForRoll(List.of(r1, r2), 99));
    }

    @Test
    void rollComTresItens() {
        var r1 = new CrateManager.CrateReward("money", 100, 10, "d1", null, null, null, null);
        var r2 = new CrateManager.CrateReward("money", 500, 20, "d2", null, null, null, null);
        var r3 = new CrateManager.CrateReward("title", 0, 70, "d3", null, null, "t", null);

        assertEquals(r1, getRewardForRoll(List.of(r1, r2, r3), 0));
        assertEquals(r1, getRewardForRoll(List.of(r1, r2, r3), 9));
        assertEquals(r2, getRewardForRoll(List.of(r1, r2, r3), 10));
        assertEquals(r2, getRewardForRoll(List.of(r1, r2, r3), 29));
        assertEquals(r3, getRewardForRoll(List.of(r1, r2, r3), 30));
        assertEquals(r3, getRewardForRoll(List.of(r1, r2, r3), 99));
    }
}
