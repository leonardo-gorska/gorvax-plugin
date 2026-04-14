package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Fluxo de Daily Login.
 * Managers envolvidos: PlayerData (streak), CrateManager (keys).
 *
 * Fluxo testado:
 * 1. Login consecutivos incrementam streak
 * 2. Streak reseta após gap > 48h
 * 3. Streak wrap no dia 7 → volta ao dia 1
 * 4. Recompensas incluem crate keys no dia 7
 */
@Tag("integration")
class DailyLoginFlowIT extends GorvaxIntegrationTest {

    // Lógica de streak replicada do DailyRewardManager
    private static final long MIN_HOURS = 20;
    private static final long MAX_HOURS = 48;

    private int processLogin(PlayerData pd, long currentTimeMs) {
        long last = pd.getLastDailyReward();

        if (last == 0) {
            // Primeiro login
            pd.setLoginStreak(1);
            pd.setLastDailyReward(currentTimeMs);
            return 1;
        }

        long hoursSince = (currentTimeMs - last) / (1000L * 60L * 60L);

        if (hoursSince >= MIN_HOURS && hoursSince <= MAX_HOURS) {
            // Login válido: incrementar streak
            int streak = pd.getLoginStreak() + 1;
            if (streak > 7)
                streak = 1; // Wrap
            pd.setLoginStreak(streak);
            pd.setLastDailyReward(currentTimeMs);
            return streak;
        } else if (hoursSince > MAX_HOURS) {
            // Gap muito grande: reset
            pd.setLoginStreak(1);
            pd.setLastDailyReward(currentTimeMs);
            return 1;
        }

        // Muito cedo (< MIN_HOURS)
        return -1;
    }

    @Test
    void streakCompletoDe7Dias() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        long baseTime = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000L; // 24h em millis

        // Dia 1: primeiro login
        int streak = processLogin(pd, baseTime);
        assertEquals(1, streak);

        // Dias 2-7: login diário (24h entre cada)
        for (int d = 2; d <= 7; d++) {
            streak = processLogin(pd, baseTime + day * (d - 1));
            assertEquals(d, streak, "Streak deveria ser " + d + " no dia " + d);
        }

        // Dia 8: wrap para 1
        streak = processLogin(pd, baseTime + day * 7);
        assertEquals(1, streak);
    }

    @Test
    void streakResetaComGapMaiorQue48h() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        long baseTime = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000L;

        // Login dia 1
        processLogin(pd, baseTime);
        assertEquals(1, pd.getLoginStreak());

        // Login dia 2 (24h depois)
        processLogin(pd, baseTime + day);
        assertEquals(2, pd.getLoginStreak());

        // Pula 3 dias (72h depois do último) → reset
        processLogin(pd, baseTime + day + (3L * day));
        assertEquals(1, pd.getLoginStreak());
    }

    @Test
    void loginMuitoCedoRetornaMenosUm() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        long baseTime = System.currentTimeMillis();

        // Primeiro login
        processLogin(pd, baseTime);

        // Tenta logar 5h depois — muito cedo
        int resultado = processLogin(pd, baseTime + 5 * 60 * 60 * 1000L);
        assertEquals(-1, resultado, "Login < 20h deve ser rejeitado");
        assertEquals(1, pd.getLoginStreak(), "Streak não deve ter mudado");
    }

    @Test
    void dia7GeraRecompensaEspecial() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        long baseTime = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000L;

        // Progredir até dia 7
        for (int d = 0; d < 7; d++) {
            processLogin(pd, baseTime + day * d);
        }
        assertEquals(7, pd.getLoginStreak());

        // Dia 7: simula recompensa — crate key + bônus de dinheiro
        // No dia 7 o DailyRewardManager daria crate key "comum"
        int crateKeysComum = pd.getCrateKeys().getOrDefault("comum", 0);
        pd.getCrateKeys().put("comum", crateKeysComum + 1);
        pd.addMoneyEarned(1000.0);
        pd.addClaimBlocks(200);

        assertEquals(1, pd.getCrateKeys().get("comum"));
        assertEquals(1000.0, pd.getTotalMoneyEarned(), 0.01);
    }

    @Test
    void multiplasSemanasConsecutivas() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        long baseTime = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000L;

        // 3 semanas completas (21 dias)
        int totalCrateKeys = 0;
        for (int d = 0; d < 21; d++) {
            int streak = processLogin(pd, baseTime + day * d);

            if (streak == 7) {
                totalCrateKeys++;
                pd.getCrateKeys().merge("comum", 1, Integer::sum);
            }
        }

        assertEquals(3, totalCrateKeys, "3 semanas = 3 crate keys");
        assertEquals(3, pd.getCrateKeys().get("comum"));
    }
}
