package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.MarketManager.MarketItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para GlobalMarketManager.
 * Lógica pura de preço dinâmico — construtor aceita null para GorvaxCore.
 */
class GlobalMarketManagerTest {

    private GlobalMarketManager market;

    // Helper para criar MarketItem de teste
    private MarketItem createItem(String id, double buyPrice, double sellPrice) {
        return new MarketItem(id, id, null, buyPrice, sellPrice);
    }

    @BeforeEach
    void setUp() {
        market = new GlobalMarketManager(null); // GorvaxCore não é usado no construtor
    }

    // --- getPrice com demanda neutra ---

    @Test
    void precoBaseSemDemanda() {
        MarketItem item = createItem("diamond", 100.0, 50.0);

        assertEquals(100.0, market.getPrice(item, true), 0.001);  // Buy
        assertEquals(50.0, market.getPrice(item, false), 0.001);  // Sell
    }

    // --- getPrice com demanda positiva ---

    @Test
    void precoComDemandaPositiva() {
        MarketItem item = createItem("diamond", 100.0, 50.0);
        market.updateDemand("diamond", 100); // score = 100 → multiplier = 1.5

        assertEquals(150.0, market.getPrice(item, true), 0.001);
        assertEquals(75.0, market.getPrice(item, false), 0.001);
    }

    // --- getPrice com demanda negativa ---

    @Test
    void precoComDemandaNegativa() {
        MarketItem item = createItem("diamond", 100.0, 50.0);
        market.updateDemand("diamond", -100); // score = -100 → multiplier = 0.5

        assertEquals(50.0, market.getPrice(item, true), 0.001);
        assertEquals(25.0, market.getPrice(item, false), 0.001);
    }

    // --- Clamp no MAX_MULTIPLIER (3.0) ---

    @Test
    void precoClampMaximo() {
        MarketItem item = createItem("diamond", 100.0, 50.0);
        market.updateDemand("diamond", 9999); // Muito alto → clamp em 3.0x

        assertEquals(300.0, market.getPrice(item, true), 0.001);
        assertEquals(150.0, market.getPrice(item, false), 0.001);
    }

    // --- Clamp no MIN_MULTIPLIER (0.2) ---

    @Test
    void precoClampMinimo() {
        MarketItem item = createItem("diamond", 100.0, 50.0);
        market.updateDemand("diamond", -9999); // Muito baixo → clamp em 0.2x

        assertEquals(20.0, market.getPrice(item, true), 0.001);
        assertEquals(10.0, market.getPrice(item, false), 0.001);
    }

    // --- updateDemand acumula ---

    @Test
    void updateDemandAcumulaCorretamente() {
        market.updateDemand("iron", 50);
        market.updateDemand("iron", 30);
        market.updateDemand("iron", -10);

        assertEquals(70, market.getDemand("iron"));
    }

    // --- getDemand de item inexistente ---

    @Test
    void getDemandItemInexistenteRetornaZero() {
        assertEquals(0, market.getDemand("inexistente"));
    }
}
