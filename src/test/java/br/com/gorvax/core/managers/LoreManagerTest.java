package br.com.gorvax.core.managers;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para LoreManager.
 * Testa BookData/TotemData records e lógica de locKey/lookup replicada.
 */
class LoreManagerTest {

    // --- BookData record ---

    @Test
    void bookDataRecord() {
        var book = new LoreManager.BookData("§4Gênesis", "§7Gorvax", List.of("Página 1", "Página 2"));
        assertEquals("§4Gênesis", book.title());
        assertEquals("§7Gorvax", book.author());
        assertEquals(2, book.pages().size());
        assertEquals("Página 1", book.pages().get(0));
    }

    @Test
    void bookDataEquality() {
        var a = new LoreManager.BookData("T", "A", List.of("P1"));
        var b = new LoreManager.BookData("T", "A", List.of("P1"));
        assertEquals(a, b);
    }

    // --- TotemData record ---

    @Test
    void totemDataRecord() {
        var totem = new LoreManager.TotemData("fire_pillar", Material.LODESTONE,
                List.of("§7Pilar de Fogo", "§cAbençoe-se"));
        assertEquals("fire_pillar", totem.id());
        assertEquals(Material.LODESTONE, totem.block());
        assertEquals(2, totem.lore().size());
    }

    // --- locKey replicado ---

    private String locKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    @Test
    void locKeyFormat() {
        assertEquals("world,10,64,20", locKey("world", 10, 64, 20));
    }

    @Test
    void locKeyNegativeCoords() {
        assertEquals("nether,-50,32,-100", locKey("nether", -50, 32, -100));
    }

    // --- Bookshelf/Totem lookup replicado ---

    private Map<String, String> bookshelves;
    private Map<String, LoreManager.TotemData> totems;

    @BeforeEach
    void setUp() {
        bookshelves = new HashMap<>();
        totems = new HashMap<>();
    }

    @Test
    void isLoreBookshelf() {
        bookshelves.put(locKey("world", 10, 64, 20), "genesis");
        assertTrue(bookshelves.containsKey(locKey("world", 10, 64, 20)));
        assertFalse(bookshelves.containsKey(locKey("world", 10, 64, 21)));
    }

    @Test
    void getBookIdAt() {
        bookshelves.put(locKey("world", 10, 64, 20), "genesis");
        assertEquals("genesis", bookshelves.get(locKey("world", 10, 64, 20)));
        assertNull(bookshelves.get(locKey("world", 0, 0, 0)));
    }

    @Test
    void isLoreTotem() {
        var totem = new LoreManager.TotemData("fire", Material.LODESTONE, List.of("lore"));
        totems.put(locKey("world", 50, 80, 100), totem);
        assertTrue(totems.containsKey(locKey("world", 50, 80, 100)));
        assertFalse(totems.containsKey(locKey("world", 0, 0, 0)));
    }

    @Test
    void getTotemAt() {
        var totem = new LoreManager.TotemData("ice", Material.BLUE_ICE, List.of("§bGelo"));
        String key = locKey("world", 50, 80, 100);
        totems.put(key, totem);

        assertEquals(totem, totems.get(key));
        assertNull(totems.get(locKey("world", 0, 0, 0)));
    }

    // --- Color conversion (&→§) replicada ---

    @Test
    void colorConversion() {
        String input = "&4Gênesis do Mundo";
        String expected = "§4Gênesis do Mundo";
        assertEquals(expected, input.replace("&", "§"));
    }

    @Test
    void multipleColorCodes() {
        String input = "&c&lTítulo &7Descrição";
        String expected = "§c§lTítulo §7Descrição";
        assertEquals(expected, input.replace("&", "§"));
    }

    // --- Múltiplas estantes e totems ---

    @Test
    void multipleBookshelvesIndependent() {
        bookshelves.put(locKey("world", 0, 64, 0), "genesis");
        bookshelves.put(locKey("world", 100, 64, 100), "war_of_realms");
        bookshelves.put(locKey("nether", 0, 64, 0), "abissal");

        assertEquals(3, bookshelves.size());
        assertEquals("genesis", bookshelves.get(locKey("world", 0, 64, 0)));
        assertEquals("war_of_realms", bookshelves.get(locKey("world", 100, 64, 100)));
        assertEquals("abissal", bookshelves.get(locKey("nether", 0, 64, 0)));
    }

    @Test
    void multipleTotemsIndependent() {
        totems.put(locKey("world", 0, 64, 0),
                new LoreManager.TotemData("fire", Material.LODESTONE, List.of("fogo")));
        totems.put(locKey("world", 100, 64, 100),
                new LoreManager.TotemData("ice", Material.BLUE_ICE, List.of("gelo")));

        assertEquals(2, totems.size());
        assertEquals("fire", totems.get(locKey("world", 0, 64, 0)).id());
        assertEquals("ice", totems.get(locKey("world", 100, 64, 100)).id());
    }
}
