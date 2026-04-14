package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ReputationManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Fluxo de Combate.
 * Managers envolvidos: CombatManager, DuelManager, ReputationManager,
 * PlayerData.
 */
@Tag("integration")
class CombatFlowIT extends GorvaxIntegrationTest {

    @BeforeEach
    void setup() {
        combatTags.clear();
        killStreaks.clear();
    }

    @Test
    void fluxoCompletoPvPComKarma() {
        UUID attacker = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        PlayerData pdA = createPlayerData(attacker);
        PlayerData pdV = createPlayerData(victim);

        tagPlayer(attacker, 15000L);
        tagPlayer(victim, 15000L);
        assertTrue(isInCombat(attacker));
        assertTrue(isInCombat(victim));

        pdA.incrementKills();
        pdV.incrementDeaths();
        registerKill(attacker);
        resetKillStreak(victim);

        assertEquals(1, getKillStreak(attacker));
        assertEquals(0, getKillStreak(victim));

        pdA.addKarma(-3);
        assertEquals(ReputationManager.KarmaRank.NEUTRO, getKarmaRank(pdA.getKarma()));
    }

    @Test
    void killStreakCom10KillsGeraBounty() {
        UUID killer = UUID.randomUUID();
        PlayerData pd = createPlayerData(killer);
        for (int i = 0; i < 10; i++) {
            pd.incrementKills();
            registerKill(killer);
        }
        assertEquals(10, getKillStreak(killer));
        assertTrue(getKillStreak(killer) >= 10);
    }

    @Test
    void morteResetaKillStreak() {
        UUID p = UUID.randomUUID();
        for (int i = 0; i < 5; i++)
            registerKill(p);
        assertEquals(5, getKillStreak(p));
        resetKillStreak(p);
        assertEquals(0, getKillStreak(p));
    }

    @Test
    void combatTagExpiraCorretamente() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        tagPlayer(uuid, 100L);
        assertTrue(isInCombat(uuid));
        Thread.sleep(150);
        assertFalse(isInCombat(uuid));
    }

    @Test
    void dueloSimuladoComAposta() {
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        PlayerData pdC = createPlayerData(c);
        PlayerData pdD = createPlayerData(d);

        pdC.incrementKills();
        pdD.incrementDeaths();
        double winnings = 500.0 * 2 * 0.95;
        pdC.addMoneyEarned(winnings);
        pdD.addMoneySpent(500.0);

        assertEquals(950.0, pdC.getTotalMoneyEarned(), 0.01);
        assertEquals(500.0, pdD.getTotalMoneySpent(), 0.01);
    }

    @Test
    void karmaCaiParaProcurado() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);
        for (int i = 0; i < 40; i++) {
            pd.incrementKills();
            pd.addKarma(-3);
        }
        assertEquals(-120, pd.getKarma());
        assertEquals(ReputationManager.KarmaRank.PROCURADO, getKarmaRank(pd.getKarma()));
        assertEquals(1.25, getPriceMultiplier(pd.getKarma()), 0.001);
    }
}
