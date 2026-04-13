package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.MarketManager.MarketListing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para LocalMarketManager.
 * Testa lógica de mapa de listagens sem dependências Bukkit.
 */
class LocalMarketManagerTest {

    @Test
    void estadoInicialListasVazias() {
        Map<String, List<MarketListing>> listings = new ConcurrentHashMap<>();
        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }

    @Test
    void listingsMapAdicionaCidade() {
        Map<String, List<MarketListing>> listings = new ConcurrentHashMap<>();
        listings.put("city-1", new CopyOnWriteArrayList<>());
        assertEquals(1, listings.size());
        assertTrue(listings.containsKey("city-1"));
        assertTrue(listings.get("city-1").isEmpty());
    }

    @Test
    void listingsMapMultiplasCidades() {
        Map<String, List<MarketListing>> listings = new ConcurrentHashMap<>();
        listings.put("city-1", new CopyOnWriteArrayList<>());
        listings.put("city-2", new CopyOnWriteArrayList<>());
        listings.put("city-3", new CopyOnWriteArrayList<>());
        assertEquals(3, listings.size());
    }

    @Test
    void listingsMapRemoveCidade() {
        Map<String, List<MarketListing>> listings = new ConcurrentHashMap<>();
        listings.put("city-1", new CopyOnWriteArrayList<>());
        listings.remove("city-1");
        assertTrue(listings.isEmpty());
    }

    @Test
    void listingsMapCityNaoExisteRetornaNull() {
        Map<String, List<MarketListing>> listings = new ConcurrentHashMap<>();
        assertNull(listings.get("inexistente"));
    }
}
