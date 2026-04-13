package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.MarketManager.MarketCategory;
import br.com.gorvax.core.managers.MarketManager.MarketItem;
import br.com.gorvax.core.managers.MarketManager.MarketListing;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Persistência YAML e tasks periódicas do sistema de mercado.
 * Gerencia: market_global.yml, market_local.yml, market_data.yml
 */
public class MarketData {

    private final GorvaxCore plugin;

    // --- Configuração global ---
    private FileConfiguration marketConfig;

    // --- Demanda dinâmica ---
    private File demandFile;
    private FileConfiguration demandConfig;
    private final Map<String, Integer> itemDemand;

    // --- Listings locais ---
    private final Map<String, List<MarketListing>> localListings;
    private final AtomicBoolean needsLocalSave = new AtomicBoolean(false);
    private final ReentrantLock localSaving = new ReentrantLock();

    // --- Constantes de normalização ---
    private static final double DECAY_RATE = 0.90; // 10% decay por ciclo

    public MarketData(GorvaxCore plugin, Map<String, Integer> itemDemand,
                      Map<String, List<MarketListing>> localListings) {
        this.plugin = plugin;
        this.itemDemand = itemDemand;
        this.localListings = localListings;
    }

    // =============================
    // CONFIGURAÇÃO GLOBAL (market_global.yml)
    // =============================

    public FileConfiguration loadConfig(Map<String, MarketCategory> categories) {
        File file = new File(plugin.getDataFolder(), "market_global.yml");
        if (!file.exists()) {
            plugin.saveResource("market_global.yml", false);
        }

        marketConfig = YamlConfiguration.loadConfiguration(file);
        categories.clear();

        ConfigurationSection catsSection = marketConfig.getConfigurationSection("market.categories");
        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                ConfigurationSection catSection = catsSection.getConfigurationSection(key);
                if (catSection != null) {
                    String name = catSection.getString("name", key);
                    String iconStr = catSection.getString("icon", "STONE");
                    Material icon = Material.matchMaterial(iconStr);
                    if (icon == null)
                        icon = Material.STONE;
                    int slot = catSection.getInt("slot", 0);

                    MarketCategory category = new MarketCategory(key, name, icon, slot);

                    // Carregar itens desta categoria
                    ConfigurationSection itemsSection = catSection.getConfigurationSection("items");
                    if (itemsSection != null) {
                        for (String itemKey : itemsSection.getKeys(false)) {
                            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                            if (itemSection != null) {
                                String matStr = itemSection.getString("material", "STONE");
                                Material mat = Material.matchMaterial(matStr);
                                if (mat != null) {
                                    String itemName = itemSection.getString("name", "§b" + formatName(mat));
                                    double buy = itemSection.getDouble("buy", 0);
                                    double sell = itemSection.getDouble("sell", 0);
                                    category.items.add(new MarketItem(itemKey, itemName, mat, buy, sell));
                                }
                            }
                        }
                    }
                    categories.put(key, category);
                }
            }
        }

        return marketConfig;
    }

    public FileConfiguration getMarketConfig() {
        return marketConfig;
    }

    // =============================
    // DEMANDA DINÂMICA (market_data.yml)
    // =============================

    public void loadMarketData() {
        demandFile = new File(plugin.getDataFolder(), "market_data.yml");
        if (!demandFile.exists()) {
            try {
                demandFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao criar market_data.yml: " + e.getMessage());
            }
        }
        demandConfig = YamlConfiguration.loadConfiguration(demandFile);
        itemDemand.clear();

        if (demandConfig.contains("demand")) {
            ConfigurationSection demandSection = demandConfig.getConfigurationSection("demand");
            if (demandSection != null) {
                for (String key : demandSection.getKeys(false)) {
                    itemDemand.put(key, demandConfig.getInt("demand." + key));
                }
            }
        }
    }

    public synchronized void saveMarketData() {
        if (demandConfig == null || demandFile == null)
            return;

        for (Map.Entry<String, Integer> entry : itemDemand.entrySet()) {
            demandConfig.set("demand." + entry.getKey(), entry.getValue());
        }

        try {
            demandConfig.save(demandFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar market_data.yml: " + e.getMessage());
        }
    }

    // =============================
    // MERCADO LOCAL (market_local.yml)
    // =============================

    public void loadLocalMarket() {
        File file = new File(plugin.getDataFolder(), "market_local.yml");
        if (!file.exists())
            return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        localListings.clear();

        if (config.contains("cities")) {
            for (String cityId : config.getConfigurationSection("cities").getKeys(false)) {
                List<MarketListing> listings = new ArrayList<>();
                if (config.contains("cities." + cityId + ".items")) {
                    for (String itemId : config.getConfigurationSection("cities." + cityId + ".items").getKeys(false)) {
                        String path = "cities." + cityId + ".items." + itemId;
                        try {
                            UUID seller = UUID.fromString(config.getString(path + ".seller"));
                            ItemStack item = config.getItemStack(path + ".item");
                            double price = config.getDouble(path + ".price");
                            long time = config.getLong(path + ".date");
                            String sellerName = config.getString(path + ".sellerName", "Desconhecido");

                            listings.add(new MarketListing(itemId, seller, sellerName, item, price, time));
                        } catch (Exception e) {
                            plugin.getLogger().warning(
                                    "Erro ao carregar item " + itemId + " do mercado local: " + e.getMessage());
                        }
                    }
                }
                localListings.put(cityId, listings);
            }
        }
    }

    public void markLocalDirty() {
        needsLocalSave.set(true);
    }

    public boolean saveLocalMarketSync() {
        localSaving.lock();
        try {
            File file = new File(plugin.getDataFolder(), "market_local.yml");
            File tmpFile = new File(plugin.getDataFolder(), "market_local.yml.tmp");
            File backup = new File(plugin.getDataFolder(), "market_local.yml.bak");

            // Backup de segurança antes de sobrescrever
            if (file.exists()) {
                try {
                    Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception backupEx) {
                    plugin.getLogger().warning("Falha ao criar backup de market_local.yml: " + backupEx.getMessage());
                }
            }

            FileConfiguration config = new YamlConfiguration();

            synchronized (localListings) {
                for (Map.Entry<String, List<MarketListing>> entry : localListings.entrySet()) {
                    String cityId = entry.getKey();
                    int i = 0;
                    for (MarketListing listing : entry.getValue()) {
                        String path = "cities." + cityId + ".items." + (listing.id != null ? listing.id : i++);
                        config.set(path + ".seller", listing.seller.toString());
                        config.set(path + ".sellerName", listing.sellerName);
                        config.set(path + ".item", listing.item);
                        config.set(path + ".price", listing.price);
                        config.set(path + ".date", listing.timestamp);
                    }
                }
            }

            config.save(tmpFile);

            // Atomic Move
            try {
                Files.move(tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("CRITICAL: Erro ao salvar market_local.yml: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            localSaving.unlock();
        }
    }

    // =============================
    // TASKS PERIÓDICAS
    // =============================

    public void startNormalizationTask() {
        // Normalização de demanda e salvamento periódico (5 min)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            boolean changed = false;
            for (Map.Entry<String, Integer> entry : itemDemand.entrySet()) {
                int current = entry.getValue();
                if (current == 0)
                    continue;

                int next = (int) (current * DECAY_RATE);
                if (Math.abs(next) < 5)
                    next = 0;

                if (next != current) {
                    itemDemand.put(entry.getKey(), next);
                    changed = true;
                }
            }
            if (changed) {
                saveMarketData();
            }
        }, 6000L, 6000L);

        // Tarefa de salvamento atrasado para Market Local (Evitar IO excessivo)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (needsLocalSave.get()) {
                needsLocalSave.set(false);
                saveLocalMarketSync();
            }
        }, 200L, 200L); // Checa a cada 10 segundos
    }

    // =============================
    // UTILITÁRIOS
    // =============================

    private String formatName(Material mat) {
        String[] words = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
