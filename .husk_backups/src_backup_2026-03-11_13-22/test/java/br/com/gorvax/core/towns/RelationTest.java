package br.com.gorvax.core.towns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de sanidade para o enum Relation.
 */
class RelationTest {

    @Test
    void todosOsValoresExistem() {
        assertNotNull(Relation.NEUTRAL);
        assertNotNull(Relation.ALLY);
        assertNotNull(Relation.ENEMY);
        assertNotNull(Relation.TRUCE);
        assertNotNull(Relation.WAR);
    }

    @Test
    void valueOfFuncionaParaCada() {
        assertEquals(Relation.NEUTRAL, Relation.valueOf("NEUTRAL"));
        assertEquals(Relation.ALLY, Relation.valueOf("ALLY"));
        assertEquals(Relation.ENEMY, Relation.valueOf("ENEMY"));
        assertEquals(Relation.TRUCE, Relation.valueOf("TRUCE"));
        assertEquals(Relation.WAR, Relation.valueOf("WAR"));
    }

    @Test
    void contagemTotal() {
        assertEquals(5, Relation.values().length);
    }
}
