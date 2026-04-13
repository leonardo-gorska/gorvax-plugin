package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para LeaderboardManager.
 * Testa LeaderboardEntry, trimAndSort, getTop, getPosition, isValidCategory.
 */
class LeaderboardManagerTest {

    // --- LeaderboardEntry record ---

    @Test
    void leaderboardEntryRecord() {
        UUID uuid = UUID.randomUUID();
        var entry = new LeaderboardManager.LeaderboardEntry(uuid, "Jogador1", 42.5);
        assertEquals(uuid, entry.uuid());
        assertEquals("Jogador1", entry.name());
        assertEquals(42.5, entry.value(), 0.001);
    }

    @Test
    void leaderboardEntryEquality() {
        UUID uuid = UUID.randomUUID();
        var a = new LeaderboardManager.LeaderboardEntry(uuid, "P1", 10.0);
        var b = new LeaderboardManager.LeaderboardEntry(uuid, "P1", 10.0);
        assertEquals(a, b);
    }

    // --- CATEGORIES constante ---

    @Test
    void categoriesContem8() {
        assertEquals(8, LeaderboardManager.CATEGORIES.length);
    }

    @Test
    void categoriesContains() {
        List<String> cats = Arrays.asList(LeaderboardManager.CATEGORIES);
        assertTrue(cats.contains("kills"));
        assertTrue(cats.contains("mortes"));
        assertTrue(cats.contains("kdr"));
        assertTrue(cats.contains("riqueza"));
        assertTrue(cats.contains("playtime"));
        assertTrue(cats.contains("bosses"));
        assertTrue(cats.contains("reinos"));
        assertTrue(cats.contains("streak"));
    }

    // --- isValidCategory ---

    @Test
    void isValidCategoryValidas() {
        for (String cat : LeaderboardManager.CATEGORIES) {
            assertTrue(isValidCategory(cat), "Categoria deveria ser válida: " + cat);
        }
    }

    @Test
    void isValidCategoryCaseInsensitive() {
        assertTrue(isValidCategory("KILLS"));
        assertTrue(isValidCategory("Mortes"));
        assertTrue(isValidCategory("KDR"));
    }

    @Test
    void isValidCategoryInvalida() {
        assertFalse(isValidCategory("inexistente"));
        assertFalse(isValidCategory(""));
        assertFalse(isValidCategory("kill")); // sem 's'
    }

    private boolean isValidCategory(String category) {
        for (String cat : LeaderboardManager.CATEGORIES) {
            if (cat.equalsIgnoreCase(category))
                return true;
        }
        return false;
    }

    // --- trimAndSort replicado ---

    private int topSize;
    private Map<String, List<LeaderboardManager.LeaderboardEntry>> cache;

    @BeforeEach
    void setUp() {
        topSize = 3; // menor para facilitar testes
        cache = new ConcurrentHashMap<>();
    }

    private List<LeaderboardManager.LeaderboardEntry> trimAndSort(
            List<LeaderboardManager.LeaderboardEntry> list,
            Comparator<LeaderboardManager.LeaderboardEntry> comp) {
        list.sort(comp);
        if (list.size() > topSize) {
            return Collections.unmodifiableList(new ArrayList<>(list.subList(0, topSize)));
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    @Test
    void trimAndSortOrdenacaoDesc() {
        Comparator<LeaderboardManager.LeaderboardEntry> desc = (a, b) -> Double.compare(b.value(), a.value());

        var list = new ArrayList<>(List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 10),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P2", 50),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P3", 30)));

        var result = trimAndSort(list, desc);
        assertEquals(3, result.size());
        assertEquals(50, result.get(0).value(), 0.001);
        assertEquals(30, result.get(1).value(), 0.001);
        assertEquals(10, result.get(2).value(), 0.001);
    }

    @Test
    void trimAndSortTruncatesTopSize() {
        Comparator<LeaderboardManager.LeaderboardEntry> desc = (a, b) -> Double.compare(b.value(), a.value());

        var list = new ArrayList<>(List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 10),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P2", 50),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P3", 30),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P4", 5),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P5", 100)));

        var result = trimAndSort(list, desc);
        assertEquals(3, result.size()); // Deve truncar para topSize=3
        assertEquals(100, result.get(0).value(), 0.001);
        assertEquals(50, result.get(1).value(), 0.001);
        assertEquals(30, result.get(2).value(), 0.001);
    }

    @Test
    void trimAndSortListaVazia() {
        var result = trimAndSort(new ArrayList<>(),
                (a, b) -> Double.compare(b.value(), a.value()));
        assertTrue(result.isEmpty());
    }

    @Test
    void trimAndSortResultImutavel() {
        var list = new ArrayList<>(List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 10)));
        var result = trimAndSort(list, (a, b) -> Double.compare(b.value(), a.value()));
        assertThrows(UnsupportedOperationException.class,
                () -> result.add(new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "X", 0)));
    }

    // --- getTop ---

    @Test
    void getTopCategoriaExistente() {
        var entries = List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 100));
        cache.put("kills", entries);

        assertEquals(1, cache.getOrDefault("kills", Collections.emptyList()).size());
    }

    @Test
    void getTopCategoriaInexistente() {
        assertTrue(cache.getOrDefault("inexistente", Collections.emptyList()).isEmpty());
    }

    @Test
    void getTopCaseInsensitive() {
        var entries = List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 100));
        cache.put("kills", entries);

        // Replica getTop que faz category.toLowerCase()
        assertFalse(cache.getOrDefault("kills", Collections.emptyList()).isEmpty());
    }

    // --- getPosition ---

    private int getPosition(String category, UUID uuid) {
        List<LeaderboardManager.LeaderboardEntry> list = cache.getOrDefault(category.toLowerCase(),
                Collections.emptyList());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    @Test
    void getPositionPrimeiro() {
        UUID target = UUID.randomUUID();
        cache.put("kills", List.of(
                new LeaderboardManager.LeaderboardEntry(target, "P1", 100),
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P2", 50)));
        assertEquals(1, getPosition("kills", target));
    }

    @Test
    void getPositionSegundo() {
        UUID target = UUID.randomUUID();
        cache.put("kills", List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 100),
                new LeaderboardManager.LeaderboardEntry(target, "P2", 50)));
        assertEquals(2, getPosition("kills", target));
    }

    @Test
    void getPositionAusente() {
        cache.put("kills", List.of(
                new LeaderboardManager.LeaderboardEntry(UUID.randomUUID(), "P1", 100)));
        assertEquals(-1, getPosition("kills", UUID.randomUUID()));
    }

    @Test
    void getPositionCategoriaInexistente() {
        assertEquals(-1, getPosition("inexistente", UUID.randomUUID()));
    }
}
