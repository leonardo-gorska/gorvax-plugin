package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.File;
import java.util.*;

/**
 * Gerenciador do sistema de Lore do GorvaxCore.
 * Carrega livros, estantes e totems de lore_books.yml.
 */
public class LoreManager {

    private final GorvaxCore plugin;
    private FileConfiguration loreConfig;
    private File loreFile;

    /** bookId → dados do livro (título, autor, páginas) */
    private final Map<String, BookData> books = new HashMap<>();
    /** Location serializada → bookId */
    private final Map<String, String> bookshelves = new HashMap<>();
    /** Location serializada → TotemData */
    private final Map<String, TotemData> totems = new HashMap<>();

    public LoreManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** Carrega ou recarrega o lore_books.yml */
    public void loadConfig() {
        loreFile = new File(plugin.getDataFolder(), "lore_books.yml");
        if (!loreFile.exists()) {
            plugin.saveResource("lore_books.yml", false);
        }
        loreConfig = YamlConfiguration.loadConfiguration(loreFile);

        books.clear();
        bookshelves.clear();
        totems.clear();

        loadBooks();
        loadBookshelves();
        loadTotems();

        plugin.getLogger().info("§a[LoreManager] Carregados " + books.size() + " livros, "
                + bookshelves.size() + " estantes e " + totems.size() + " totems de lore.");
    }

    private void loadBooks() {
        ConfigurationSection booksSection = loreConfig.getConfigurationSection("books");
        if (booksSection == null)
            return;

        for (String bookId : booksSection.getKeys(false)) {
            ConfigurationSection sec = booksSection.getConfigurationSection(bookId);
            if (sec == null)
                continue;

            String title = sec.getString("title", "Livro Misterioso").replace("&", "§");
            String author = sec.getString("author", "Desconhecido").replace("&", "§");
            List<String> pages = sec.getStringList("pages");
            List<String> coloredPages = new ArrayList<>();
            for (String page : pages) {
                coloredPages.add(page.replace("&", "§"));
            }

            books.put(bookId, new BookData(title, author, coloredPages));
        }
    }

    private void loadBookshelves() {
        List<?> list = loreConfig.getList("bookshelves");
        if (list == null)
            return;

        for (Object obj : list) {
            if (obj instanceof Map<?, ?> map) {
                String bookId = String.valueOf(map.get("book"));
                String worldName = String.valueOf(map.get("world"));
                int x = ((Number) map.get("x")).intValue();
                int y = ((Number) map.get("y")).intValue();
                int z = ((Number) map.get("z")).intValue();

                String key = locKey(worldName, x, y, z);
                bookshelves.put(key, bookId);
            }
        }
    }

    private void loadTotems() {
        ConfigurationSection totemsSection = loreConfig.getConfigurationSection("totems");
        if (totemsSection == null)
            return;

        for (String totemId : totemsSection.getKeys(false)) {
            ConfigurationSection sec = totemsSection.getConfigurationSection(totemId);
            if (sec == null)
                continue;

            String worldName = sec.getString("world", "world");
            int x = sec.getInt("x");
            int y = sec.getInt("y");
            int z = sec.getInt("z");
            String blockStr = sec.getString("block", "LODESTONE");
            Material block;
            try {
                block = Material.valueOf(blockStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                block = Material.LODESTONE;
            }

            List<String> loreLines = sec.getStringList("lore");
            List<String> coloredLore = new ArrayList<>();
            for (String line : loreLines) {
                coloredLore.add(line.replace("&", "§"));
            }

            String key = locKey(worldName, x, y, z);
            totems.put(key, new TotemData(totemId, block, coloredLore));
        }
    }

    // ==================== PUBLIC API ====================

    /**
     * Verifica se a localização é uma estante de lore.
     */
    public boolean isLoreBookshelf(Location loc) {
        return bookshelves.containsKey(locKey(loc));
    }

    /**
     * Retorna o bookId para a estante nessa localização, ou null.
     */
    public String getBookIdAt(Location loc) {
        return bookshelves.get(locKey(loc));
    }

    /**
     * Verifica se a localização é um totem de lore.
     */
    public boolean isLoreTotem(Location loc) {
        return totems.containsKey(locKey(loc));
    }

    /**
     * Retorna os dados do totem nessa localização, ou null.
     */
    public TotemData getTotemAt(Location loc) {
        return totems.get(locKey(loc));
    }

    /**
     * Cria um ItemStack de livro escrito a partir do bookId.
     */
    public ItemStack createBook(String bookId) {
        BookData data = books.get(bookId);
        if (data == null)
            return null;

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null)
            return null;

        meta.title(LegacyComponentSerializer.legacySection().deserialize(data.title));
        meta.author(LegacyComponentSerializer.legacySection().deserialize(data.author));
        meta.setGeneration(BookMeta.Generation.ORIGINAL);

        for (String page : data.pages) {
            meta.addPages(LegacyComponentSerializer.legacySection().deserialize(page));
        }

        book.setItemMeta(meta);
        return book;
    }

    /**
     * Verifica se o jogador já possui este livro (pelo título).
     */
    public boolean playerHasBook(Player player, String bookId) {
        BookData data = books.get(bookId);
        if (data == null)
            return false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.WRITTEN_BOOK)
                continue;
            BookMeta meta = (BookMeta) item.getItemMeta();
            if (meta != null && data.title.equals(PlainTextComponentSerializer.plainText().serialize(meta.title()))) {
                return true;
            }
        }
        return false;
    }

    // ==================== HELPERS ====================

    /**
     * Retorna todas as localizações de estantes de lore como objetos Location.
     */
    public List<Location> getBookshelfLocations() {
        List<Location> locations = new ArrayList<>();
        for (String key : bookshelves.keySet()) {
            Location loc = parseLocKey(key);
            if (loc != null)
                locations.add(loc);
        }
        return locations;
    }

    /**
     * Retorna todas as localizações de totems de lore como objetos Location.
     */
    public List<Location> getTotemLocations() {
        List<Location> locations = new ArrayList<>();
        for (String key : totems.keySet()) {
            Location loc = parseLocKey(key);
            if (loc != null)
                locations.add(loc);
        }
        return locations;
    }

    private Location parseLocKey(String key) {
        String[] parts = key.split(",");
        if (parts.length != 4)
            return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null)
            return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String locKey(Location loc) {
        return locKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String locKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    // ==================== DATA CLASSES ====================

    public record BookData(String title, String author, List<String> pages) {
    }

    public record TotemData(String id, Material block, List<String> lore) {
    }
}
