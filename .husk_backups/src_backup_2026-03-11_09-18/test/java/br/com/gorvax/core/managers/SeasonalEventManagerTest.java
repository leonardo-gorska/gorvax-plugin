package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para SeasonalEventManager.
 * Testa lógica pura de getMonthName, stripColors, e SeasonalEvent record.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class SeasonalEventManagerTest {

    // --- getMonthName (static, testável diretamente) ---

    @Test
    void getMonthNameJaneiro() {
        assertEquals("Janeiro", SeasonalEventManager.getMonthName(1));
    }

    @Test
    void getMonthNameFevereiro() {
        assertEquals("Fevereiro", SeasonalEventManager.getMonthName(2));
    }

    @Test
    void getMonthNameMarco() {
        assertEquals("Março", SeasonalEventManager.getMonthName(3));
    }

    @Test
    void getMonthNameAbril() {
        assertEquals("Abril", SeasonalEventManager.getMonthName(4));
    }

    @Test
    void getMonthNameMaio() {
        assertEquals("Maio", SeasonalEventManager.getMonthName(5));
    }

    @Test
    void getMonthNameJunho() {
        assertEquals("Junho", SeasonalEventManager.getMonthName(6));
    }

    @Test
    void getMonthNameJulho() {
        assertEquals("Julho", SeasonalEventManager.getMonthName(7));
    }

    @Test
    void getMonthNameAgosto() {
        assertEquals("Agosto", SeasonalEventManager.getMonthName(8));
    }

    @Test
    void getMonthNameSetembro() {
        assertEquals("Setembro", SeasonalEventManager.getMonthName(9));
    }

    @Test
    void getMonthNameOutubro() {
        assertEquals("Outubro", SeasonalEventManager.getMonthName(10));
    }

    @Test
    void getMonthNameNovembro() {
        assertEquals("Novembro", SeasonalEventManager.getMonthName(11));
    }

    @Test
    void getMonthNameDezembro() {
        assertEquals("Dezembro", SeasonalEventManager.getMonthName(12));
    }

    @Test
    void getMonthNameInvalido() {
        assertEquals("Desconhecido", SeasonalEventManager.getMonthName(0));
        assertEquals("Desconhecido", SeasonalEventManager.getMonthName(13));
        assertEquals("Desconhecido", SeasonalEventManager.getMonthName(-1));
    }

    // --- stripColors (lógica replicada) ---

    private String stripColors(String text) {
        if (text == null)
            return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    @Test
    void stripColorsSimples() {
        assertEquals("Olá Mundo", stripColors("§aOlá §bMundo"));
    }

    @Test
    void stripColorsSemCores() {
        assertEquals("Texto Normal", stripColors("Texto Normal"));
    }

    @Test
    void stripColorsVazio() {
        assertEquals("", stripColors(""));
    }

    @Test
    void stripColorsNull() {
        assertEquals("", stripColors(null));
    }

    @Test
    void stripColorsFormatacao() {
        assertEquals("Negrito", stripColors("§lNegrito"));
    }

    @Test
    void stripColorsMultiplos() {
        assertEquals("Festival de Verão", stripColors("§6§lFestival §ede §fVerão"));
    }

    @Test
    void stripColorsComNumericos() {
        assertEquals("Cinza", stripColors("§7Cinza"));
        assertEquals("Cor", stripColors("§0§1§2§3§4§5§6§7§8§9Cor"));
    }

    // --- SeasonalEvent record ---

    @Test
    void seasonalEventRecordCampos() {
        var event = new SeasonalEventManager.SeasonalEvent(
                "verao", "§6Festival de Verão", "Evento de verão com bônus", 1, 1, 31,
                null, 1.5, 1.2, 1.3,
                java.util.List.of(), "§eEvento começou!", "§eEvento terminou!", null);
        assertEquals("verao", event.id());
        assertEquals("§6Festival de Verão", event.name());
        assertEquals("Evento de verão com bônus", event.description());
        assertEquals(1, event.month());
        assertEquals(1, event.startDay());
        assertEquals(31, event.endDay());
        assertNull(event.bossId());
        assertEquals(1.5, event.xpMultiplier(), 0.001);
        assertEquals(1.2, event.lootMultiplier(), 0.001);
        assertEquals(1.3, event.moneyMultiplier(), 0.001);
        assertTrue(event.achievementIds().isEmpty());
    }

    @Test
    void seasonalEventRecordEquality() {
        var a = new SeasonalEventManager.SeasonalEvent(
                "natal", "§cNatal", "Natal feliz", 12, 1, 31,
                "boss_santa", 2.0, 1.5, 2.0,
                java.util.List.of("ach1"), "start", "end", null);
        var b = new SeasonalEventManager.SeasonalEvent(
                "natal", "§cNatal", "Natal feliz", 12, 1, 31,
                "boss_santa", 2.0, 1.5, 2.0,
                java.util.List.of("ach1"), "start", "end", null);
        assertEquals(a, b);
    }

    @Test
    void seasonalEventRecordInequality() {
        var a = new SeasonalEventManager.SeasonalEvent(
                "natal", "§cNatal", "desc", 12, 1, 31,
                null, 2.0, 1.5, 2.0,
                java.util.List.of(), "s", "e", null);
        var b = new SeasonalEventManager.SeasonalEvent(
                "pascoa", "§ePáscoa", "desc2", 4, 1, 30,
                null, 1.2, 1.1, 1.0,
                java.util.List.of(), "s", "e", null);
        assertNotEquals(a, b);
    }

    @Test
    void seasonalEventMultiplicadoresNeutros() {
        var event = new SeasonalEventManager.SeasonalEvent(
                "normal", "Normal", "desc", 6, 1, 30,
                null, 1.0, 1.0, 1.0,
                java.util.List.of(), "s", "e", null);
        assertEquals(1.0, event.xpMultiplier(), 0.001);
        assertEquals(1.0, event.lootMultiplier(), 0.001);
        assertEquals(1.0, event.moneyMultiplier(), 0.001);
    }

    @Test
    void seasonalEventMesesValidos() {
        for (int m = 1; m <= 12; m++) {
            assertNotEquals("Desconhecido", SeasonalEventManager.getMonthName(m));
        }
    }
}
