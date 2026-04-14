package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para QuestManager.
 * Testa lógica pura de isSameDay, isSameWeek, matchesTarget, QuestType e
 * records.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class QuestManagerTest {

    // --- isSameDay (lógica replicada) ---

    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    @Test
    void mesmoDiaMesmoTimestamp() {
        long now = System.currentTimeMillis();
        assertTrue(isSameDay(now, now));
    }

    @Test
    void mesmoDiaHorasDiferentes() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.MARCH, 10, 8, 0, 0);
        long morning = c.getTimeInMillis();
        c.set(2026, Calendar.MARCH, 10, 22, 0, 0);
        long evening = c.getTimeInMillis();
        assertTrue(isSameDay(morning, evening));
    }

    @Test
    void diasDiferentes() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.MARCH, 10, 23, 59, 59);
        long day1 = c.getTimeInMillis();
        c.set(2026, Calendar.MARCH, 11, 0, 0, 1);
        long day2 = c.getTimeInMillis();
        assertFalse(isSameDay(day1, day2));
    }

    @Test
    void mesmoAnosDiferentes() {
        Calendar c = Calendar.getInstance();
        c.set(2025, Calendar.MARCH, 10, 12, 0, 0);
        long year1 = c.getTimeInMillis();
        c.set(2026, Calendar.MARCH, 10, 12, 0, 0);
        long year2 = c.getTimeInMillis();
        assertFalse(isSameDay(year1, year2));
    }

    // --- isSameWeek (lógica replicada) ---

    private boolean isSameWeek(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR);
    }

    @Test
    void mesmaSemanaMesmoTimestamp() {
        long now = System.currentTimeMillis();
        assertTrue(isSameWeek(now, now));
    }

    @Test
    void mesmaSemanaDiasDiferentes() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.MARCH, 9, 12, 0, 0); // Segunda
        long monday = c.getTimeInMillis();
        c.set(2026, Calendar.MARCH, 13, 12, 0, 0); // Sexta (mesma semana)
        long friday = c.getTimeInMillis();
        assertTrue(isSameWeek(monday, friday));
    }

    @Test
    void semanasDiferentes() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.MARCH, 10, 12, 0, 0);
        long week1 = c.getTimeInMillis();
        c.set(2026, Calendar.MARCH, 20, 12, 0, 0);
        long week2 = c.getTimeInMillis();
        assertFalse(isSameWeek(week1, week2));
    }

    // --- matchesTarget (lógica replicada) ---

    private boolean matchesTarget(String defTarget, String target) {
        if (defTarget.equalsIgnoreCase("ANY"))
            return true;
        if (target == null || target.equalsIgnoreCase("ANY"))
            return true;
        return defTarget.equalsIgnoreCase(target);
    }

    @Test
    void matchesTargetQuestAny() {
        assertTrue(matchesTarget("ANY", "ZOMBIE"));
    }

    @Test
    void matchesTargetPlayerAny() {
        assertTrue(matchesTarget("ZOMBIE", "ANY"));
    }

    @Test
    void matchesTargetBothAny() {
        assertTrue(matchesTarget("ANY", "ANY"));
    }

    @Test
    void matchesTargetNull() {
        assertTrue(matchesTarget("ZOMBIE", null));
    }

    @Test
    void matchesTargetExato() {
        assertTrue(matchesTarget("ZOMBIE", "ZOMBIE"));
    }

    @Test
    void matchesTargetExatoCaseInsensitive() {
        assertTrue(matchesTarget("ZOMBIE", "zombie"));
    }

    @Test
    void matchesTargetDiferente() {
        assertFalse(matchesTarget("ZOMBIE", "SKELETON"));
    }

    // --- QuestType enum ---

    @Test
    void questTypeEnumValues() {
        assertEquals(6, QuestManager.QuestType.values().length);
    }

    @Test
    void questTypeContainsExpected() {
        assertNotNull(QuestManager.QuestType.valueOf("KILL_MOB"));
        assertNotNull(QuestManager.QuestType.valueOf("KILL_PLAYER"));
        assertNotNull(QuestManager.QuestType.valueOf("MINE_BLOCK"));
        assertNotNull(QuestManager.QuestType.valueOf("SELL_MARKET"));
        assertNotNull(QuestManager.QuestType.valueOf("BOSS_PARTICIPATE"));
        assertNotNull(QuestManager.QuestType.valueOf("DAILY_COMPLETE"));
    }

    // --- QuestDefinition record ---

    @Test
    void questDefinitionRecord() {
        var def = new QuestManager.QuestDefinition(
                "matar_zumbis", QuestManager.QuestType.KILL_MOB, "ZOMBIE", 10,
                "§eMatar Zumbis", "Mate 10 zumbis", null,
                500.0, 50, "raro", null);
        assertEquals("matar_zumbis", def.id());
        assertEquals(QuestManager.QuestType.KILL_MOB, def.type());
        assertEquals("ZOMBIE", def.target());
        assertEquals(10, def.amount());
        assertEquals("§eMatar Zumbis", def.name());
        assertEquals(500.0, def.rewardMoney(), 0.001);
        assertEquals(50, def.rewardClaimBlocks());
        assertEquals("raro", def.rewardCrateKey());
    }

    // --- LoreQuestStep record ---

    @Test
    void loreQuestStepRecord() {
        var step = new QuestManager.LoreQuestStep(
                QuestManager.QuestType.KILL_MOB, "ZOMBIE", 5, "§7Derrote as criaturas...");
        assertEquals(QuestManager.QuestType.KILL_MOB, step.type());
        assertEquals("ZOMBIE", step.target());
        assertEquals(5, step.amount());
        assertEquals("§7Derrote as criaturas...", step.dialogue());
    }

    // --- LoreQuestDefinition record ---

    @Test
    void loreQuestDefinitionRecord() {
        var steps = java.util.List.of(
                new QuestManager.LoreQuestStep(QuestManager.QuestType.KILL_MOB, "ZOMBIE", 5, "diag1"),
                new QuestManager.LoreQuestStep(QuestManager.QuestType.MINE_BLOCK, "DIAMOND_ORE", 3, "diag2"));

        var lq = new QuestManager.LoreQuestDefinition(
                "lq_1", "§eA Busca", "Descrição lore", null,
                steps, 1000.0, 50, "Título Lendário", "book_id");
        assertEquals("lq_1", lq.id());
        assertEquals("§eA Busca", lq.name());
        assertEquals(2, lq.steps().size());
        assertEquals(1000.0, lq.rewardMoney(), 0.001);
        assertEquals(50, lq.rewardKarma());
        assertEquals("Título Lendário", lq.rewardTitle());
        assertEquals("book_id", lq.rewardBook());
    }
}
