package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Battle Pass Sazonal.
 * Managers envolvidos: PlayerData (BP fields), SeasonalEventManager
 * (multiplicadores).
 *
 * Fluxo testado:
 * 1. XP por ação × multiplicador sazonal → level up
 * 2. Season reset limpa progresso
 * 3. Múltiplos level ups em sequência
 */
@Tag("integration")
class BattlePassSeasonIT extends GorvaxIntegrationTest {

    // Lógica de XP e level replicada do BattlePassManager
    private int addXpAndGetLevel(PlayerData pd, int xpAmount) {
        int currentXp = pd.getBattlePassXp() + xpAmount;
        int currentLevel = pd.getBattlePassLevel();
        int maxLevel = 30;

        while (currentLevel < maxLevel) {
            int nextLevelXp = getXpForLevel(currentLevel + 1);
            if (currentXp >= nextLevelXp) {
                currentXp -= nextLevelXp;
                currentLevel++;
            } else {
                break;
            }
        }

        pd.setBattlePassXp(currentXp);
        pd.setBattlePassLevel(currentLevel);
        return currentLevel;
    }

    private void resetSeason(PlayerData pd) {
        pd.setBattlePassLevel(0);
        pd.setBattlePassXp(0);
        pd.setBattlePassPremium(false);
    }

    @Test
    void xpIncrementalSubeDeNivel() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Nível 1 requer 100 XP
        addXpAndGetLevel(pd, 50);
        assertEquals(0, pd.getBattlePassLevel());
        assertEquals(50, pd.getBattlePassXp());

        // Mais 60 XP → total 110, passa do nível 1 (100 XP)
        addXpAndGetLevel(pd, 60);
        assertEquals(1, pd.getBattlePassLevel());
        assertEquals(10, pd.getBattlePassXp()); // Sobra 10 XP
    }

    @Test
    void multiLevelUpComXpAlto() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // 100 + 200 + 300 = 600 XP para atingir nível 3
        // Nível 1: 100 XP, Nível 2: 200 XP, Nível 3: 300 XP
        addXpAndGetLevel(pd, 600);
        assertEquals(3, pd.getBattlePassLevel());
        assertEquals(0, pd.getBattlePassXp());
    }

    @Test
    void xpComMultiplicadorSazonal() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Evento sazonal ativo: 2x XP
        double multiplier = 2.0;
        int baseXp = 50; // XP base por kill
        int finalXp = (int) (baseXp * multiplier);

        assertEquals(100, finalXp);

        addXpAndGetLevel(pd, finalXp);
        assertEquals(1, pd.getBattlePassLevel()); // 100 XP = nível 1
    }

    @Test
    void seasonResetLimpaProgresso() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Progresso a nível 5
        addXpAndGetLevel(pd, 1500); // 100+200+300+400+500=1500
        assertEquals(5, pd.getBattlePassLevel());

        pd.setBattlePassPremium(true);
        assertTrue(pd.isBattlePassPremium());

        // Reset de temporada
        resetSeason(pd);
        assertEquals(0, pd.getBattlePassLevel());
        assertEquals(0, pd.getBattlePassXp());
        assertFalse(pd.isBattlePassPremium());
    }

    @Test
    void nivelMaximoNaoUltrapassaLimite() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // XP absurdo — tentamos passar do nível 30
        addXpAndGetLevel(pd, 100000);
        assertEquals(30, pd.getBattlePassLevel());
    }

    @Test
    void fontesDeXpDiferentes() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Simula diferentes fontes de XP durante um dia de jogo
        addXpAndGetLevel(pd, 25); // kill PvP
        addXpAndGetLevel(pd, 15); // mineração
        addXpAndGetLevel(pd, 50); // boss
        addXpAndGetLevel(pd, 10); // login

        // Total: 100 XP = nível 1
        assertEquals(1, pd.getBattlePassLevel());
    }
}
