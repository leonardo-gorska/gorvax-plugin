package br.com.gorvax.core.towns.managers;

import br.com.gorvax.core.towns.managers.WarManager.War;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para WarManager.
 * Testa lógica pura do modelo War, WarState, e formatação de duração.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class WarManagerTest {

    // --- War model ---

    private War createWar(String id, String attacker, String defender) {
        return new War(id, attacker, defender,
                System.currentTimeMillis(),
                System.currentTimeMillis() + 3600000L,
                System.currentTimeMillis() + 86400000L,
                0, 0,
                WarManager.WarState.PREPARATION);
    }

    @Test
    void warGetOpponentRetornaDefensor() {
        War war = createWar("w1", "attacker", "defender");
        assertEquals("defender", war.getOpponent("attacker"));
    }

    @Test
    void warGetOpponentRetornaAtacante() {
        War war = createWar("w1", "attacker", "defender");
        assertEquals("attacker", war.getOpponent("defender"));
    }

    @Test
    void warGetOpponentReinoDesconhecido() {
        War war = createWar("w1", "attacker", "defender");
        // Se o reino não participa, retorna attackerId (fallback)
        assertEquals("attacker", war.getOpponent("outsider"));
    }

    @Test
    void warInvolvesAtacante() {
        War war = createWar("w1", "attacker", "defender");
        assertTrue(war.involves("attacker"));
    }

    @Test
    void warInvolvesDefensor() {
        War war = createWar("w1", "attacker", "defender");
        assertTrue(war.involves("defender"));
    }

    @Test
    void warNaoInvolveOutsider() {
        War war = createWar("w1", "attacker", "defender");
        assertFalse(war.involves("outsider"));
    }

    @Test
    void warAddAttackerPoints() {
        War war = createWar("w1", "a", "d");
        assertEquals(0, war.getAttackerPoints());
        war.addAttackerPoints(5);
        assertEquals(5, war.getAttackerPoints());
        war.addAttackerPoints(3);
        assertEquals(8, war.getAttackerPoints());
    }

    @Test
    void warAddDefenderPoints() {
        War war = createWar("w1", "a", "d");
        assertEquals(0, war.getDefenderPoints());
        war.addDefenderPoints(10);
        assertEquals(10, war.getDefenderPoints());
    }

    @Test
    void warStateTransition() {
        War war = createWar("w1", "a", "d");
        assertEquals(WarManager.WarState.PREPARATION, war.getState());
        war.setState(WarManager.WarState.ACTIVE);
        assertEquals(WarManager.WarState.ACTIVE, war.getState());
        war.setState(WarManager.WarState.ENDED);
        assertEquals(WarManager.WarState.ENDED, war.getState());
    }

    @Test
    void warGetters() {
        long declared = 1000L;
        long starts = 2000L;
        long ends = 3000L;
        War war = new War("w1", "att", "def", declared, starts, ends, 5, 3,
                WarManager.WarState.ACTIVE);
        assertEquals("w1", war.getId());
        assertEquals("att", war.getAttackerId());
        assertEquals("def", war.getDefenderId());
        assertEquals(declared, war.getDeclaredAt());
        assertEquals(starts, war.getStartsAt());
        assertEquals(ends, war.getEndsAt());
        assertEquals(5, war.getAttackerPoints());
        assertEquals(3, war.getDefenderPoints());
        assertEquals(WarManager.WarState.ACTIVE, war.getState());
    }

    // --- WarState enum ---

    @Test
    void warStateEnumValues() {
        assertEquals(3, WarManager.WarState.values().length);
    }

    @Test
    void warStateContainsExpected() {
        assertNotNull(WarManager.WarState.valueOf("PREPARATION"));
        assertNotNull(WarManager.WarState.valueOf("ACTIVE"));
        assertNotNull(WarManager.WarState.valueOf("ENDED"));
    }

    // --- formatDuration (static, testável diretamente) ---

    @Test
    void formatDurationZero() {
        assertEquals("0m", WarManager.formatDuration(0));
    }

    @Test
    void formatDurationNegativo() {
        assertEquals("0m", WarManager.formatDuration(-5000));
    }

    @Test
    void formatDurationApenasMinutos() {
        // 30 minutos = 1.800.000 ms
        assertEquals("30m", WarManager.formatDuration(1_800_000));
    }

    @Test
    void formatDurationUmaHora() {
        // 60 minutos = 3.600.000 ms
        assertEquals("1h", WarManager.formatDuration(3_600_000));
    }

    @Test
    void formatDurationHorasEMinutos() {
        // 3h 20m = 12.000.000 ms
        assertEquals("3h 20m", WarManager.formatDuration(12_000_000));
    }

    @Test
    void formatDurationDias() {
        // 2d 5h = (2*24 + 5)*60*60*1000 = 190.800.000
        assertEquals("2d 5h", WarManager.formatDuration(190_800_000));
    }

    @Test
    void formatDurationDiasEMinutos() {
        // 1d 0h 30m = (24*60 + 30)*60*1000 = 88.200.000
        assertEquals("1d 30m", WarManager.formatDuration(88_200_000));
    }

    @Test
    void formatDurationDiasHorasMinutos() {
        // 1d 2h 15m = (24*60 + 2*60 + 15)*60*1000 = 94.500.000
        assertEquals("1d 2h 15m", WarManager.formatDuration(94_500_000));
    }
}
