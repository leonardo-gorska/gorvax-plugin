package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.MarketManager.MarketCategory;
import br.com.gorvax.core.managers.MarketManager.MarketItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica do mercado global: categorias, preços dinâmicos e demanda.
 */
public class GlobalMarketManager {

    private final Map<String, MarketCategory> categories = new LinkedHashMap<>();
    private final Map<String, Integer> itemDemand = new ConcurrentHashMap<>();

    // Constantes de preço dinâmico
    private static final double SENSITIVITY = 0.005; // 0.5% de variação por item comprado/vendido
    private static final double MAX_MULTIPLIER = 3.0;
    private static final double MIN_MULTIPLIER = 0.2;

    public GlobalMarketManager(GorvaxCore plugin) {
        // Plugin reservado para futuras expansões
    }

    /**
     * Carrega configuração do mercado global via MarketData.
     */
    public void loadConfig(MarketData data) {
        data.loadConfig(categories);
    }

    /**
     * Calcula o preço atual de um item baseado na demanda.
     */
    public double getPrice(MarketItem item, boolean isBuy) {
        int score = itemDemand.getOrDefault(item.id, 0);

        // Multiplier: 1 + (score * 0.005)
        // Score 100 -> 1 + 0.5 = 1.5x preço
        // Score -100 -> 1 - 0.5 = 0.5x preço
        double multiplier = 1.0 + (score * SENSITIVITY);

        // Clamp
        if (multiplier > MAX_MULTIPLIER)
            multiplier = MAX_MULTIPLIER;
        if (multiplier < MIN_MULTIPLIER)
            multiplier = MIN_MULTIPLIER;

        double base = isBuy ? item.buyPrice : item.sellPrice;
        return base * multiplier;
    }

    /**
     * Atualiza o score de demanda de um item.
     */
    public void updateDemand(String itemId, int amount) {
        itemDemand.merge(itemId, amount, Integer::sum);
    }

    /**
     * Retorna o score de demanda de um item.
     */
    public int getDemand(String itemId) {
        return itemDemand.getOrDefault(itemId, 0);
    }

    public Map<String, MarketCategory> getCategories() {
        return categories;
    }

    public Map<String, Integer> getItemDemand() {
        return itemDemand;
    }
}
