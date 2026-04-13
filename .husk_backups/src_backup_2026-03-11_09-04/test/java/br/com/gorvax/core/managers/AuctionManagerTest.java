package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.AuctionManager.Auction;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AuctionManager.
 * Testa lógica pura de formatação e modelo Auction.
 * Replica lógica de formatTime e formatMaterialName.
 */
class AuctionManagerTest {

    // --- formatTime (lógica replicada) ---

    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        } else if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    @Test
    void formatTimeSegundos() {
        assertEquals("30s", formatTime(30));
    }

    @Test
    void formatTimeMinutos() {
        assertEquals("5m 0s", formatTime(300));
    }

    @Test
    void formatTimeMinutosESegundos() {
        assertEquals("2m 30s", formatTime(150));
    }

    @Test
    void formatTimeHoras() {
        assertEquals("1h 0m", formatTime(3600));
    }

    @Test
    void formatTimeHorasEMinutos() {
        assertEquals("1h 30m", formatTime(5400));
    }

    @Test
    void formatTimeZero() {
        assertEquals("0s", formatTime(0));
    }

    @Test
    void formatTime24Horas() {
        assertEquals("24h 0m", formatTime(86400));
    }

    @Test
    void formatTime1Segundo() {
        assertEquals("1s", formatTime(1));
    }

    @Test
    void formatTime59Segundos() {
        assertEquals("59s", formatTime(59));
    }

    @Test
    void formatTime61Segundos() {
        assertEquals("1m 1s", formatTime(61));
    }

    // --- formatMaterialName (lógica replicada) ---

    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty())
                sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return sb.toString();
    }

    @Test
    void formatMaterialNameSimples() {
        assertEquals("Diamond", formatMaterialName("DIAMOND"));
    }

    @Test
    void formatMaterialNameComposto() {
        assertEquals("Diamond Sword", formatMaterialName("DIAMOND_SWORD"));
    }

    @Test
    void formatMaterialNameTresPalavras() {
        assertEquals("Golden Horse Armor", formatMaterialName("GOLDEN_HORSE_ARMOR"));
    }

    @Test
    void formatMaterialNameStone() {
        assertEquals("Stone", formatMaterialName("STONE"));
    }

    @Test
    void formatMaterialNameOakPlanks() {
        assertEquals("Oak Planks", formatMaterialName("OAK_PLANKS"));
    }

    // --- Auction model ---

    @Test
    void auctionModelCriacao() {
        UUID seller = UUID.randomUUID();
        ItemStack item = mock(ItemStack.class);
        long endTime = System.currentTimeMillis() + 3600000L;

        Auction auction = new Auction("auction-1", seller, "Gorska", item, 100.0, endTime);

        assertEquals("auction-1", auction.id);
        assertEquals(seller, auction.sellerUUID);
        assertEquals("Gorska", auction.sellerName);
        assertEquals(item, auction.item);
        assertEquals(100.0, auction.minPrice, 0.001);
        assertEquals(0, auction.currentBid, 0.001);
        assertNull(auction.currentBidder);
        assertNull(auction.currentBidderName);
        assertEquals(0, auction.bidCount);
    }

    @Test
    void auctionModelNaoExpirado() {
        ItemStack item = mock(ItemStack.class);
        long endTime = System.currentTimeMillis() + 3600000L;
        Auction auction = new Auction("a-1", UUID.randomUUID(), "Test", item, 10, endTime);

        assertFalse(auction.isExpired());
        assertTrue(auction.getRemainingSeconds() > 0);
    }

    @Test
    void auctionModelExpirado() {
        ItemStack item = mock(ItemStack.class);
        long endTime = System.currentTimeMillis() - 1000L;
        Auction auction = new Auction("a-2", UUID.randomUUID(), "Test", item, 10, endTime);

        assertTrue(auction.isExpired());
        assertEquals(0, auction.getRemainingSeconds());
    }

    @Test
    void auctionBidUpdate() {
        ItemStack item = mock(ItemStack.class);
        Auction auction = new Auction("a-3", UUID.randomUUID(), "Seller", item, 100,
                System.currentTimeMillis() + 3600000L);

        UUID bidder = UUID.randomUUID();
        auction.currentBid = 150.0;
        auction.currentBidder = bidder;
        auction.currentBidderName = "Bidder";
        auction.bidCount++;

        assertEquals(150.0, auction.currentBid, 0.001);
        assertEquals(bidder, auction.currentBidder);
        assertEquals("Bidder", auction.currentBidderName);
        assertEquals(1, auction.bidCount);
    }

    @Test
    void auctionRemainingSecondsConversion() {
        ItemStack item = mock(ItemStack.class);
        long endTime = System.currentTimeMillis() + 7200000L; // +2h
        Auction auction = new Auction("a-4", UUID.randomUUID(), "Test", item, 10, endTime);

        long remaining = auction.getRemainingSeconds();
        assertTrue(remaining >= 7190); // Aproximadamente 2h
        assertTrue(remaining <= 7200);
    }
}
