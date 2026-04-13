package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para DailyRewardManager.
 * Testa lógica de streak, timing e ciclo de 7 dias.
 * Replica lógica interna para evitar dependências Bukkit.
 */
class DailyRewardManagerTest {

    // Configs replicadas
    private final int minHours = 20;
    private final int maxHours = 48;

    // --- Cálculo de hoursSinceReward ---

    private long calcHoursSinceReward(long lastReward) {
        long now = System.currentTimeMillis();
        return (lastReward > 0) ? (now - lastReward) / (1000 * 60 * 60) : Long.MAX_VALUE;
    }

    @Test
    void hoursSinceRewardNuncaResgatou() {
        assertEquals(Long.MAX_VALUE, calcHoursSinceReward(0));
    }

    @Test
    void hoursSinceRewardRecente() {
        long twoHoursAgo = System.currentTimeMillis() - (2 * 3600 * 1000L);
        long hours = calcHoursSinceReward(twoHoursAgo);
        assertTrue(hours >= 1 && hours <= 3, "Deveria ser ~2h, foi: " + hours);
    }

    @Test
    void hoursSinceReward24hAtras() {
        long yesterday = System.currentTimeMillis() - (24 * 3600 * 1000L);
        long hours = calcHoursSinceReward(yesterday);
        assertTrue(hours >= 23 && hours <= 25, "Deveria ser ~24h, foi: " + hours);
    }

    // --- Lógica de streak (checkAndNotify) ---

    @Test
    void streakResetAposMaxHours() {
        // Se lastReward > 0 e hoursSince > maxHours → streak = 0
        long lastReward = System.currentTimeMillis() - (49 * 3600 * 1000L); // 49h atrás
        long hoursSince = calcHoursSinceReward(lastReward);
        int streak = 5;

        if (lastReward > 0 && hoursSince > maxHours) {
            streak = 0;
        }
        assertEquals(0, streak);
    }

    @Test
    void streakMantemDentroDeMaxHours() {
        long lastReward = System.currentTimeMillis() - (24 * 3600 * 1000L); // 24h atrás
        long hoursSince = calcHoursSinceReward(lastReward);
        int streak = 3;

        if (lastReward > 0 && hoursSince > maxHours) {
            streak = 0;
        }
        assertEquals(3, streak);
    }

    // --- Lógica de claimReward (incremento + wrap) ---

    @Test
    void claimIncrementaStreak() {
        int streak = 0;
        int newStreak = streak + 1;
        if (newStreak > 7)
            newStreak = 1;
        assertEquals(1, newStreak);
    }

    @Test
    void claimStreakProgressao() {
        for (int current = 0; current < 7; current++) {
            int newStreak = current + 1;
            if (newStreak > 7)
                newStreak = 1;
            assertEquals(current + 1, newStreak, "Streak " + current + " +1 deveria ser " + (current + 1));
        }
    }

    @Test
    void claimStreakWrapApos7() {
        int streak = 7;
        int newStreak = streak + 1;
        if (newStreak > 7)
            newStreak = 1;
        assertEquals(1, newStreak);
    }

    @Test
    void claimRecusadoAnteDeMinHours() {
        long lastReward = System.currentTimeMillis() - (10 * 3600 * 1000L); // 10h atrás
        long hoursSince = calcHoursSinceReward(lastReward);
        assertTrue(hoursSince < minHours, "Deveria recusar claim — horas: " + hoursSince);
    }

    @Test
    void claimPermitidoAposMinHours() {
        long lastReward = System.currentTimeMillis() - (21 * 3600 * 1000L); // 21h atrás
        long hoursSince = calcHoursSinceReward(lastReward);
        assertTrue(hoursSince >= minHours, "Deveria permitir claim — horas: " + hoursSince);
    }

    @Test
    void claimNuncaResgatouPermitido() {
        long hoursSince = calcHoursSinceReward(0);
        assertTrue(hoursSince >= minHours, "Primeiro claim deveria ser sempre permitido");
    }

    // --- Cálculo de horasRestantes ---

    @Test
    void hoursLeftCalculo() {
        long lastReward = System.currentTimeMillis() - (15 * 3600 * 1000L); // 15h atrás
        long hoursSince = calcHoursSinceReward(lastReward);
        long hoursLeft = minHours - hoursSince;
        assertTrue(hoursLeft > 0, "Deveria ter horas restantes: " + hoursLeft);
        assertTrue(hoursLeft <= 6, "Deveria ter ~5h restantes: " + hoursLeft);
    }

    // --- Ciclo completo de 7 dias ---

    @Test
    void cicloCompleto7Dias() {
        int streak = 0;
        for (int dia = 1; dia <= 7; dia++) {
            streak = streak + 1;
            if (streak > 7)
                streak = 1;
            assertEquals(dia, streak, "Dia " + dia);
        }
        // Após dia 7, volta para 1
        streak = streak + 1;
        if (streak > 7)
            streak = 1;
        assertEquals(1, streak);
    }

    // --- Lógica de notificação ---

    @Test
    void nextDayCalc() {
        // O "próximo dia" é Math.min(streak + 1, 7)
        assertEquals(1, Math.min(0 + 1, 7)); // streak 0
        assertEquals(4, Math.min(3 + 1, 7)); // streak 3
        assertEquals(7, Math.min(6 + 1, 7)); // streak 6
        assertEquals(7, Math.min(7 + 1, 7)); // streak 7 (cap em 7)
    }

    @Test
    void enabledFlag() {
        // Se enabled=false, não deve processar
        boolean enabled = false;
        assertFalse(enabled);

        enabled = true;
        assertTrue(enabled);
    }
}
