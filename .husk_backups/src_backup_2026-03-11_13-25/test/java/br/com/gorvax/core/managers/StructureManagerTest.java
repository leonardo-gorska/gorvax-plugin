package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para StructureManager.
 * Testa StructureData record (contains, distanceTo) e lógica CRUD replicada.
 */
class StructureManagerTest {

    // --- StructureData record ---

    @Test
    void structureDataFields() {
        var data = new StructureManager.StructureData(
                "ashvale", "§6Ashvale", "medieval", "world",
                100.0, 64.0, 200.0, 90f, 0f, 50, "Admin", "2026-03-10");

        assertEquals("ashvale", data.id());
        assertEquals("§6Ashvale", data.nome());
        assertEquals("medieval", data.tema());
        assertEquals("world", data.mundo());
        assertEquals(100.0, data.x(), 0.001);
        assertEquals(64.0, data.y(), 0.001);
        assertEquals(200.0, data.z(), 0.001);
        assertEquals(90f, data.yaw(), 0.001);
        assertEquals(0f, data.pitch(), 0.001);
        assertEquals(50, data.raio());
        assertEquals("Admin", data.criadoPor());
        assertEquals("2026-03-10", data.criadoEm());
    }

    // --- contains (distância euclidiana 2D) ---

    /**
     * Replica a lógica de contains sem Location/World do Bukkit.
     */
    private boolean contains(StructureManager.StructureData s, String worldName, double lx, double lz) {
        if (!worldName.equals(s.mundo()))
            return false;
        double distSq = Math.pow(lx - s.x(), 2) + Math.pow(lz - s.z(), 2);
        return distSq <= (double) s.raio() * s.raio();
    }

    @Test
    void containsDentroDoRaio() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        assertTrue(contains(data, "world", 50, 50));
    }

    @Test
    void containsNoLimite() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        // Distância = 100 (exatamente no limite)
        assertTrue(contains(data, "world", 100, 0));
    }

    @Test
    void containsForaDoRaio() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        assertFalse(contains(data, "world", 101, 0));
    }

    @Test
    void containsMundoDiferente() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        assertFalse(contains(data, "nether", 0, 0));
    }

    @Test
    void containsDiagonal() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        // Diagonal: sqrt(70² + 70²) = ~98.99, dentro do raio
        assertTrue(contains(data, "world", 70, 70));
        // Diagonal: sqrt(71² + 71²) = ~100.41, fora do raio
        assertFalse(contains(data, "world", 71, 71));
    }

    // --- distanceTo ---

    private double distanceTo(StructureManager.StructureData s, String worldName, double lx, double lz) {
        if (!worldName.equals(s.mundo()))
            return Double.MAX_VALUE;
        return Math.sqrt(Math.pow(lx - s.x(), 2) + Math.pow(lz - s.z(), 2));
    }

    @Test
    void distanceToSameWorld() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        assertEquals(0.0, distanceTo(data, "world", 0, 0), 0.001);
        assertEquals(100.0, distanceTo(data, "world", 100, 0), 0.001);
        assertEquals(5.0, distanceTo(data, "world", 3, 4), 0.001);
    }

    @Test
    void distanceToDifferentWorld() {
        var data = new StructureManager.StructureData(
                "id", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        assertEquals(Double.MAX_VALUE, distanceTo(data, "nether", 0, 0));
    }

    // --- CRUD replicado ---

    private Map<String, StructureManager.StructureData> structures;

    @BeforeEach
    void setUp() {
        structures = new ConcurrentHashMap<>();
    }

    @Test
    void getExistente() {
        var data = new StructureManager.StructureData(
                "ashvale", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D");
        structures.put("ashvale", data);
        assertEquals(data, structures.get("ashvale"));
    }

    @Test
    void getInexistente() {
        assertNull(structures.get("nao_existe"));
    }

    @Test
    void deleteExistente() {
        structures.put("id1", new StructureManager.StructureData(
                "id1", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D"));
        assertNotNull(structures.remove("id1"));
        assertNull(structures.get("id1"));
    }

    @Test
    void deleteInexistente() {
        assertNull(structures.remove("nao_existe"));
    }

    @Test
    void getAllIds() {
        structures.put("a", new StructureManager.StructureData(
                "a", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D"));
        structures.put("b", new StructureManager.StructureData(
                "b", "n", "t", "world", 0, 64, 0, 0, 0, 100, "A", "D"));
        List<String> ids = new ArrayList<>(structures.keySet());
        assertEquals(2, ids.size());
        assertTrue(ids.contains("a"));
        assertTrue(ids.contains("b"));
    }

    @Test
    void getStructureAtEncontra() {
        var s1 = new StructureManager.StructureData(
                "s1", "n", "t", "world", 0, 64, 0, 0, 0, 50, "A", "D");
        var s2 = new StructureManager.StructureData(
                "s2", "n", "t", "world", 200, 64, 200, 0, 0, 50, "A", "D");
        structures.put("s1", s1);
        structures.put("s2", s2);

        // Procurar na posição (10, 10) — deve encontrar s1
        StructureManager.StructureData found = null;
        for (var s : structures.values()) {
            if (contains(s, "world", 10, 10)) {
                found = s;
                break;
            }
        }
        assertNotNull(found);
        assertEquals("s1", found.id());
    }

    @Test
    void getStructureAtNaoEncontra() {
        var s1 = new StructureManager.StructureData(
                "s1", "n", "t", "world", 0, 64, 0, 0, 0, 50, "A", "D");
        structures.put("s1", s1);

        StructureManager.StructureData found = null;
        for (var s : structures.values()) {
            if (contains(s, "world", 500, 500)) {
                found = s;
                break;
            }
        }
        assertNull(found);
    }

    @Test
    void getNearestMultiplas() {
        var s1 = new StructureManager.StructureData(
                "s1", "n", "t", "world", 0, 64, 0, 0, 0, 50, "A", "D");
        var s2 = new StructureManager.StructureData(
                "s2", "n", "t", "world", 100, 64, 100, 0, 0, 50, "A", "D");
        structures.put("s1", s1);
        structures.put("s2", s2);

        // Ponto (80, 80) — mais próximo de s2
        StructureManager.StructureData nearest = null;
        double minDist = Double.MAX_VALUE;
        for (var s : structures.values()) {
            double dist = distanceTo(s, "world", 80, 80);
            if (dist < minDist) {
                minDist = dist;
                nearest = s;
            }
        }
        assertNotNull(nearest);
        assertEquals("s2", nearest.id());
    }
}
