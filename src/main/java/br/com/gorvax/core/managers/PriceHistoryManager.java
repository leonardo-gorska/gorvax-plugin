package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * B14.2 — Histórico de Preços do Mercado Global.
 * Registra snapshots periódicos de preços para análise de tendências.
 */
public class PriceHistoryManager {

    private final GorvaxCore plugin;
    private final Map<String, List<PriceSnapshot>> history = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    // Configurações
    private boolean enabled;
    private int retentionHours;
    private int trendHours;

    public PriceHistoryManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        startSnapshotTask();
        startSaveTask();
    }

    private void loadConfig() {
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("price_history.enabled", true);
        this.retentionHours = config.getInt("price_history.retention_hours", 24);
        this.trendHours = config.getInt("price_history.trend_hours", 6);
    }

    // =============================
    // SNAPSHOT
    // =============================

    /**
     * Tira snapshot dos preços atuais de todos os itens do mercado global.
     */
    public void takeSnapshot() {
        if (!enabled) return;

        MarketManager mm = plugin.getMarketManager();
        if (mm == null) return;

        Map<String, MarketManager.MarketCategory> categories = mm.getCategories();
        long now = System.currentTimeMillis();

        for (MarketManager.MarketCategory cat : categories.values()) {
            for (MarketManager.MarketItem item : cat.items) {
                double buyPrice = mm.getPrice(item, true);
                double sellPrice = mm.getPrice(item, false);

                PriceSnapshot snap = new PriceSnapshot(now, buyPrice, sellPrice);
                history.computeIfAbsent(item.id, k -> new ArrayList<>()).add(snap);
            }
        }

        // Limpar snapshots antigos
        long cutoff = now - (retentionHours * 3600_000L);
        for (List<PriceSnapshot> list : history.values()) {
            list.removeIf(s -> s.timestamp < cutoff);
        }

        dirty.set(true);
    }

    // =============================
    // CONSULTAS
    // =============================

    /**
     * Retorna a variação percentual do preço de compra de um item nas últimas 'hours' horas.
     * Retorna Double.NaN se não há dados suficientes.
     */
    public double getBuyVariation(String itemId, int hours) {
        return getVariation(itemId, hours, true);
    }

    /**
     * Retorna a variação percentual do preço de venda de um item nas últimas 'hours' horas.
     */
    public double getSellVariation(String itemId, int hours) {
        return getVariation(itemId, hours, false);
    }

    private double getVariation(String itemId, int hours, boolean isBuy) {
        List<PriceSnapshot> snapshots = history.get(itemId);
        if (snapshots == null || snapshots.size() < 2) return Double.NaN;

        long cutoff = System.currentTimeMillis() - (hours * 3600_000L);

        // Encontrar o snapshot mais antigo dentro do intervalo
        PriceSnapshot oldest = null;
        for (PriceSnapshot s : snapshots) {
            if (s.timestamp >= cutoff) {
                oldest = s;
                break; // Lista é ordenada cronologicamente
            }
        }

        if (oldest == null) return Double.NaN;

        // Snapshot mais recente
        PriceSnapshot newest = snapshots.get(snapshots.size() - 1);

        double oldPrice = isBuy ? oldest.buyPrice : oldest.sellPrice;
        double newPrice = isBuy ? newest.buyPrice : newest.sellPrice;

        if (oldPrice == 0) return Double.NaN;

        return ((newPrice - oldPrice) / oldPrice) * 100.0;
    }

    /**
     * Retorna a string de tendência formatada para uso na lore do item.
     * Exemplo: "§a↑ +15.3%" ou "§c↓ -8.1%" ou "§7— 0.0%"
     */
    public String getTrendString(String itemId) {
        double variation = getBuyVariation(itemId, trendHours);
        if (Double.isNaN(variation)) return "§7— Sem dados";

        if (variation > 0.5) {
            return String.format("§a↑ +%.1f%%", variation);
        } else if (variation < -0.5) {
            return String.format("§c↓ %.1f%%", variation);
        } else {
            return String.format("§7— %.1f%%", variation);
        }
    }

    /**
     * Retorna o histórico textual de preços para exibição ao jogador.
     */
    public List<String> getPriceHistory(String itemId) {
        List<PriceSnapshot> snapshots = history.get(itemId);
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.singletonList("§7Sem histórico de preços para este item.");
        }

        List<String> lines = new ArrayList<>();
        // Mostrar últimos 12 snapshots (1 por hora se snapshot a cada 5min = últimas ~1h)
        int startIdx = Math.max(0, snapshots.size() - 12);
        for (int i = startIdx; i < snapshots.size(); i++) {
            PriceSnapshot s = snapshots.get(i);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(s.timestamp);
            String time = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
            lines.add(String.format("  §7%s §8| §aCompra: §f$%.2f §8| §eVenda: §f$%.2f",
                    time, s.buyPrice, s.sellPrice));
        }

        // Adicionar resumo
        double buyVar = getBuyVariation(itemId, trendHours);
        double sellVar = getSellVariation(itemId, trendHours);
        lines.add("");
        if (!Double.isNaN(buyVar)) {
            String buyTrend = buyVar > 0 ? "§a↑ +" : buyVar < 0 ? "§c↓ " : "§7— ";
            lines.add("  §7Tendência compra (" + trendHours + "h): " + buyTrend + String.format("%.1f%%", buyVar));
        }
        if (!Double.isNaN(sellVar)) {
            String sellTrend = sellVar > 0 ? "§a↑ +" : sellVar < 0 ? "§c↓ " : "§7— ";
            lines.add("  §7Tendência venda (" + trendHours + "h): " + sellTrend + String.format("%.1f%%", sellVar));
        }

        return lines;
    }

    /**
     * Procura um MarketItem pelo ID ou nome parcial.
     */
    public MarketManager.MarketItem findItem(String query) {
        MarketManager mm = plugin.getMarketManager();
        if (mm == null) return null;

        String lowerQuery = query.toLowerCase();
        for (MarketManager.MarketCategory cat : mm.getCategories().values()) {
            for (MarketManager.MarketItem item : cat.items) {
                if (item.id.equalsIgnoreCase(query) || item.name.toLowerCase().contains(lowerQuery)) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos os IDs de itens conhecidos no histórico.
     */
    public Set<String> getTrackedItems() {
        return Collections.unmodifiableSet(history.keySet());
    }

    // =============================
    // TASKS PERIÓDICAS
    // =============================

    private void startSnapshotTask() {
        int interval = plugin.getConfig().getInt("price_history.snapshot_interval", 6000);
        new BukkitRunnable() {
            @Override
            public void run() {
                takeSnapshot();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void startSaveTask() {
        // Save a cada 5 minutos
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAsync();
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
    }

    // =============================
    // PERSISTÊNCIA
    // =============================

    private File getDataFile() {
        return new File(plugin.getDataFolder(), "price_history.yml");
    }

    public void loadData() {
        File file = getDataFile();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("items")) return;

        for (String itemId : yaml.getConfigurationSection("items").getKeys(false)) {
            List<Map<?, ?>> snapList = yaml.getMapList("items." + itemId);
            List<PriceSnapshot> snapshots = new ArrayList<>();
            for (Map<?, ?> map : snapList) {
                try {
                    long timestamp = ((Number) map.get("t")).longValue();
                    double buyP = ((Number) map.get("b")).doubleValue();
                    double sellP = ((Number) map.get("s")).doubleValue();
                    snapshots.add(new PriceSnapshot(timestamp, buyP, sellP));
                } catch (Exception ignored) {
                    plugin.getLogger().fine("Snapshot inválido em price_history: " + ignored.getMessage());
                }
            }
            if (!snapshots.isEmpty()) {
                history.put(itemId, snapshots);
            }
        }

        plugin.getLogger().info("[Histórico de Preços] Carregados dados de " + history.size() + " itens.");
    }

    public void saveSync() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<String, List<PriceSnapshot>> entry : history.entrySet()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (PriceSnapshot s : entry.getValue()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("t", s.timestamp);
                map.put("b", Math.round(s.buyPrice * 100.0) / 100.0);
                map.put("s", Math.round(s.sellPrice * 100.0) / 100.0);
                serialized.add(map);
            }
            yaml.set("items." + entry.getKey(), serialized);
        }

        try {
            yaml.save(getDataFile());
            dirty.set(false);
        } catch (IOException e) {
            plugin.getLogger().severe("[Histórico de Preços] Erro ao salvar: " + e.getMessage());
        }
    }

    public void saveAsync() {
        if (!dirty.getAndSet(false)) return;

        // Snapshot na main thread
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, List<PriceSnapshot>> entry : history.entrySet()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (PriceSnapshot s : entry.getValue()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("t", s.timestamp);
                map.put("b", Math.round(s.buyPrice * 100.0) / 100.0);
                map.put("s", Math.round(s.sellPrice * 100.0) / 100.0);
                serialized.add(map);
            }
            yaml.set("items." + entry.getKey(), serialized);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                yaml.save(getDataFile());
            } catch (IOException e) {
                plugin.getLogger().severe("[Histórico de Preços] Erro ao salvar (async): " + e.getMessage());
            }
        });
    }

    public void reload() {
        loadConfig();
        history.clear();
        loadData();
        plugin.getLogger().info("[Histórico de Preços] Recarregado com sucesso!");
    }

    // =============================
    // CLASSE INTERNA
    // =============================

    public static class PriceSnapshot {
        public final long timestamp;
        public final double buyPrice;
        public final double sellPrice;

        public PriceSnapshot(long timestamp, double buyPrice, double sellPrice) {
            this.timestamp = timestamp;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }
}
