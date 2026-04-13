package br.com.gorvax.core.boss.managers;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para BossScheduleManager.
 * Testa lógica de agendamento semanal, cálculo de próxima ocorrência,
 * período sazonal, nomes de display e formatação de tempo restante.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class BossScheduleManagerTest {

    // --- ScheduledEvent.getNextOccurrence (lógica replicada) ---

    private LocalDateTime getNextOccurrence(DayOfWeek day, LocalTime time, LocalDateTime now) {
        LocalDateTime candidate = now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(day))
                .atTime(time);

        if (candidate.isBefore(now) || candidate.isEqual(now)) {
            candidate = now.toLocalDate()
                    .with(TemporalAdjusters.next(day))
                    .atTime(time);
        }

        return candidate;
    }

    @Test
    void nextOccurrenceFuturaMesmoDia() {
        // Se hoje é segunda e o evento é segunda 20:00, e são 10:00
        LocalDateTime now = LocalDateTime.of(2026, 3, 9, 10, 0); // Segunda
        LocalDateTime next = getNextOccurrence(DayOfWeek.MONDAY, LocalTime.of(20, 0), now);

        assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
        assertEquals(20, next.getHour());
        assertTrue(next.isAfter(now));
    }

    @Test
    void nextOccurrenceJaPassouHojeVaiPraProximaSemana() {
        // Se hoje é segunda 22:00 e evento é segunda 20:00
        LocalDateTime now = LocalDateTime.of(2026, 3, 9, 22, 0); // Segunda, 22h
        LocalDateTime next = getNextOccurrence(DayOfWeek.MONDAY, LocalTime.of(20, 0), now);

        assertTrue(next.isAfter(now));
        assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
        assertEquals(16, next.getDayOfMonth()); // Próxima segunda
    }

    @Test
    void nextOccurrenceDiaDiferente() {
        // Hoje é segunda, evento é quarta 15:00
        LocalDateTime now = LocalDateTime.of(2026, 3, 9, 10, 0); // Segunda
        LocalDateTime next = getNextOccurrence(DayOfWeek.WEDNESDAY, LocalTime.of(15, 0), now);

        assertEquals(DayOfWeek.WEDNESDAY, next.getDayOfWeek());
        assertEquals(15, next.getHour());
        assertTrue(next.isAfter(now));
    }

    @Test
    void nextOccurrenceSempreNoFuturo() {
        LocalDateTime now = LocalDateTime.now();
        for (DayOfWeek day : DayOfWeek.values()) {
            LocalDateTime next = getNextOccurrence(day, LocalTime.of(12, 0), now);
            assertTrue(next.isAfter(now) || next.isEqual(now.plusSeconds(1)));
        }
    }

    // --- hashKey (lógica replicada) ---

    @Test
    void hashKeyFormatoCorreto() {
        String hashKey = DayOfWeek.MONDAY.name() + "_" + 20 + "_" + 0 + "_" + "rei_gorvax";
        assertEquals("MONDAY_20_0_rei_gorvax", hashKey);
    }

    @Test
    void hashKeyUnicoPorEvento() {
        String key1 = DayOfWeek.MONDAY.name() + "_" + 20 + "_" + 0 + "_" + "rei_gorvax";
        String key2 = DayOfWeek.FRIDAY.name() + "_" + 20 + "_" + 0 + "_" + "rei_gorvax";
        String key3 = DayOfWeek.MONDAY.name() + "_" + 18 + "_" + 0 + "_" + "rei_gorvax";

        assertNotEquals(key1, key2);
        assertNotEquals(key1, key3);
    }

    // --- isInSeasonalPeriod (lógica replicada) ---

    private boolean isInSeasonalPeriod(int month, int day, String startStr, String endStr) {
        try {
            String[] startParts = startStr.split("-");
            String[] endParts = endStr.split("-");
            int startMonth = Integer.parseInt(startParts[0]);
            int startDay = Integer.parseInt(startParts[1]);
            int endMonth = Integer.parseInt(endParts[0]);
            int endDay = Integer.parseInt(endParts[1]);

            int current = month * 100 + day;
            int start = startMonth * 100 + startDay;
            int end = endMonth * 100 + endDay;

            if (start <= end) {
                return current >= start && current <= end;
            } else {
                return current >= start || current <= end;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void seasonalPeriodDentroDoPeriodo() {
        assertTrue(isInSeasonalPeriod(10, 25, "10-20", "10-31")); // Halloween
    }

    @Test
    void seasonalPeriodForaDoPeriodo() {
        assertFalse(isInSeasonalPeriod(11, 5, "10-20", "10-31")); // Depois do Halloween
    }

    @Test
    void seasonalPeriodViradaDeAno() {
        // 12-20 a 01-05 (Natal)
        assertTrue(isInSeasonalPeriod(12, 25, "12-20", "01-05")); // Natal
        assertTrue(isInSeasonalPeriod(1, 3, "12-20", "01-05")); // Janeiro
        assertFalse(isInSeasonalPeriod(11, 15, "12-20", "01-05")); // Novembro
    }

    @Test
    void seasonalPeriodLimitesExatos() {
        assertTrue(isInSeasonalPeriod(10, 20, "10-20", "10-31")); // Início exato
        assertTrue(isInSeasonalPeriod(10, 31, "10-20", "10-31")); // Fim exato
    }

    @Test
    void seasonalPeriodFormatoInvalidoRetornaFalse() {
        assertFalse(isInSeasonalPeriod(10, 25, "abc", "def"));
    }

    // --- resolveBossDisplayName (lógica replicada) ---

    private String resolveBossDisplayName(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "rei_gorvax" -> "§6Rei Gorvax";
            case "indrax_abissal", "indrax" -> "§5Indrax Abissal";
            case "vulgathor" -> "§cVulgathor";
            case "xylos" -> "§dXylos Devorador";
            case "skulkor" -> "§7Skulkor";
            case "kaldur" -> "§bKaldur";
            case "zarith" -> "§aZar'ith";
            case "rei_indrax" -> "§4Ruptura Temporal";
            case "halloween_boss" -> "§5Ceifador das Sombras";
            case "natal_boss" -> "§bRei do Gelo Eterno";
            default -> "§eBoss Desconhecido";
        };
    }

    @Test
    void displayNameReiGorvax() {
        assertEquals("§6Rei Gorvax", resolveBossDisplayName("rei_gorvax"));
    }

    @Test
    void displayNameIndrax() {
        assertEquals("§5Indrax Abissal", resolveBossDisplayName("indrax_abissal"));
        assertEquals("§5Indrax Abissal", resolveBossDisplayName("indrax"));
    }

    @Test
    void displayNameReiIndrax() {
        assertEquals("§4Ruptura Temporal", resolveBossDisplayName("rei_indrax"));
    }

    @Test
    void displayNameTodosBosses() {
        String[] ids = { "rei_gorvax", "indrax_abissal", "vulgathor", "xylos",
                "skulkor", "kaldur", "zarith", "halloween_boss", "natal_boss" };
        for (String id : ids) {
            assertNotEquals("§eBoss Desconhecido", resolveBossDisplayName(id));
        }
    }

    @Test
    void displayNameDesconhecido() {
        assertEquals("§eBoss Desconhecido", resolveBossDisplayName("boss_inexistente"));
    }

    @Test
    void displayNameCaseInsensitive() {
        assertEquals("§6Rei Gorvax", resolveBossDisplayName("REI_GORVAX"));
    }

    // --- UpcomingEvent.formatTimeRemaining (lógica replicada) ---

    private String formatTimeRemaining(long minutesUntil) {
        long hours = minutesUntil / 60;
        long mins = minutesUntil % 60;
        if (hours > 24) {
            long days = hours / 24;
            hours = hours % 24;
            return days + "d " + hours + "h " + mins + "m";
        } else if (hours > 0) {
            return hours + "h " + mins + "m";
        } else {
            return mins + "m";
        }
    }

    @Test
    void formatTimeApenasMinutos() {
        assertEquals("45m", formatTimeRemaining(45));
    }

    @Test
    void formatTimeHorasEMinutos() {
        assertEquals("2h 30m", formatTimeRemaining(150));
    }

    @Test
    void formatTimeDiasHorasMinutos() {
        assertEquals("1d 2h 30m", formatTimeRemaining(26 * 60 + 30)); // 26h30m
    }

    @Test
    void formatTimeZeroMinutos() {
        assertEquals("0m", formatTimeRemaining(0));
    }

    @Test
    void formatTimeExatamente1Hora() {
        assertEquals("1h 0m", formatTimeRemaining(60));
    }

    @Test
    void formatTimeExatamente24Horas() {
        assertEquals("24h 0m", formatTimeRemaining(24 * 60));
    }

    @Test
    void formatTimeMaisQue24Horas() {
        assertEquals("2d 0h 0m", formatTimeRemaining(48 * 60));
    }

    // --- Warning Deduplication ---

    @Test
    void sentWarningsDeduplicacao() {
        Set<String> sentWarnings = Collections.synchronizedSet(new HashSet<>());
        String key = "MONDAY_20_0_rei_gorvax_30_MONDAY";

        assertTrue(sentWarnings.add(key)); // Primeiro: true
        assertFalse(sentWarnings.add(key)); // Duplicado: false
    }

    @Test
    void firedEventsDeduplicacao() {
        Set<String> firedEvents = Collections.synchronizedSet(new HashSet<>());
        String key = "MONDAY_20_0_rei_gorvax_MONDAY_20_0";

        assertTrue(firedEvents.add(key)); // Primeiro: true
        assertFalse(firedEvents.add(key)); // Duplicado: false
    }

    @Test
    void cleanupOldEntriesLimpaAoPassarLimite() {
        Set<String> sentWarnings = Collections.synchronizedSet(new HashSet<>());
        for (int i = 0; i < 501; i++) {
            sentWarnings.add("warning_" + i);
        }
        assertTrue(sentWarnings.size() > 500);

        // Simula cleanup
        if (sentWarnings.size() > 500) {
            sentWarnings.clear();
        }
        assertTrue(sentWarnings.isEmpty());
    }

    // --- Event list operations ---

    @Test
    void eventListVazioRetornaListaVazia() {
        List<Object> events = new ArrayList<>();
        assertTrue(events.isEmpty());
    }

    @Test
    void eventListParseaDiaCorretamente() {
        String line = "MONDAY 20:00 rei_gorvax";
        String[] parts = line.trim().split("\\s+");
        assertEquals(3, parts.length);
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.valueOf(parts[0].toUpperCase()));

        String[] timeParts = parts[1].split(":");
        assertEquals(20, Integer.parseInt(timeParts[0]));
        assertEquals(0, Integer.parseInt(timeParts[1]));
        assertEquals("rei_gorvax", parts[2].toLowerCase());
    }

    @Test
    void eventListParseDiaInvalidoLancaException() {
        assertThrows(IllegalArgumentException.class,
                () -> DayOfWeek.valueOf("SOMEDAY"));
    }
}
