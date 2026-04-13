package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.PriceHistoryManager.PriceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para PriceHistoryManager.
 * Testa lógica de cálculo de variação e tendência.
 * (construtor requer GorvaxCore, então replicamos a lógica pura.)
 */
class PriceHistoryManagerTest {

    private Map<String, List<PriceSnapshot>> history;

    @BeforeEach
    void setUp() {
        history = new ConcurrentHashMap<>();
    }

    // --- PriceSnapshot ---

    @Test
    void snapshotCriacao() {
        long now = System.currentTimeMillis();
        PriceSnapshot snap = new PriceSnapshot(now, 100.0, 50.0);

        assertEquals(now, snap.timestamp);
        assertEquals(100.0, snap.buyPrice, 0.001);
        assertEquals(50.0, snap.sellPrice, 0.001);
    }

    // --- Variação manual (replica lógica de getVariation) ---

    /**
     * Replica a lógica de getVariation para testar sem instanciar o manager.
     */
    private double calculateVariation(List<PriceSnapshot> snapshots, int hours, boolean isBuy) {
        if (snapshots == null || snapshots.size() < 2) return Double.NaN;

        long cutoff = System.currentTimeMillis() - (hours * 3600_000L);

        PriceSnapshot oldest = null;
        for (PriceSnapshot s : snapshots) {
            if (s.timestamp >= cutoff) {
                oldest = s;
                break;
            }
        }

        if (oldest == null) return Double.NaN;

        PriceSnapshot newest = snapshots.get(snapshots.size() - 1);

        double oldPrice = isBuy ? oldest.buyPrice : oldest.sellPrice;
        double newPrice = isBuy ? newest.buyPrice : newest.sellPrice;

        if (oldPrice == 0) return Double.NaN;

        return ((newPrice - oldPrice) / oldPrice) * 100.0;
    }

    @Test
    void variacaoPositivaBuy() {
        long now = System.currentTimeMillis();
        List<PriceSnapshot> snaps = new ArrayList<>();
        snaps.add(new PriceSnapshot(now - 3600_000L, 100.0, 50.0)); // 1h atrás
        snaps.add(new PriceSnapshot(now, 150.0, 75.0)); // agora

        double var = calculateVariation(snaps, 6, true);
        assertEquals(50.0, var, 0.1); // +50%
    }

    @Test
    void variacaoNegativaSell() {
        long now = System.currentTimeMillis();
        List<PriceSnapshot> snaps = new ArrayList<>();
        snaps.add(new PriceSnapshot(now - 3600_000L, 100.0, 50.0));
        snaps.add(new PriceSnapshot(now, 80.0, 30.0));

        double var = calculateVariation(snaps, 6, false);
        assertEquals(-40.0, var, 0.1); // -40%
    }

    @Test
    void variacaoSemDados() {
        assertTrue(Double.isNaN(calculateVariation(null, 6, true)));
        assertTrue(Double.isNaN(calculateVariation(new ArrayList<>(), 6, true)));

        List<PriceSnapshot> oneSnap = new ArrayList<>();
        oneSnap.add(new PriceSnapshot(System.currentTimeMillis(), 100, 50));
        assertTrue(Double.isNaN(calculateVariation(oneSnap, 6, true)));
    }

    @Test
    void variacaoPrecoAntigoZero() {
        long now = System.currentTimeMillis();
        List<PriceSnapshot> snaps = new ArrayList<>();
        snaps.add(new PriceSnapshot(now - 3600_000L, 0.0, 0.0));
        snaps.add(new PriceSnapshot(now, 100.0, 50.0));

        assertTrue(Double.isNaN(calculateVariation(snaps, 6, true)));
    }

    @Test
    void variacaoComSnapshotForaDoIntervalo() {
        long now = System.currentTimeMillis();
        List<PriceSnapshot> snaps = new ArrayList<>();
        snaps.add(new PriceSnapshot(now - 10 * 3600_000L, 200.0, 100.0)); // 10h atrás
        snaps.add(new PriceSnapshot(now - 4 * 3600_000L, 100.0, 50.0)); // 4h atrás
        snaps.add(new PriceSnapshot(now, 120.0, 60.0));

        // Variação de 6h: oldest dentro do intervalo é 4h atrás (100 → 120 = +20%)
        double var = calculateVariation(snaps, 6, true);
        assertEquals(20.0, var, 0.1);
    }

    @Test
    void variacaoEstavel() {
        long now = System.currentTimeMillis();
        List<PriceSnapshot> snaps = new ArrayList<>();
        snaps.add(new PriceSnapshot(now - 3600_000L, 100.0, 50.0));
        snaps.add(new PriceSnapshot(now, 100.0, 50.0));

        double var = calculateVariation(snaps, 6, true);
        assertEquals(0.0, var, 0.001);
    }

    // --- Trend string ---

    @Test
    void trendStringPositiva() {
        double variation = 15.3;
        String trend;
        if (Double.isNaN(variation)) {
            trend = "§7— Sem dados";
        } else if (variation > 0.5) {
            trend = String.format(Locale.US, "§a↑ +%.1f%%", variation);
        } else if (variation < -0.5) {
            trend = String.format(Locale.US, "§c↓ %.1f%%", variation);
        } else {
            trend = String.format(Locale.US, "§7— %.1f%%", variation);
        }
        assertEquals("§a↑ +15.3%", trend);
    }

    @Test
    void trendStringNegativa() {
        double variation = -8.1;
        String trend;
        if (variation > 0.5) {
            trend = String.format(Locale.US, "§a↑ +%.1f%%", variation);
        } else if (variation < -0.5) {
            trend = String.format(Locale.US, "§c↓ %.1f%%", variation);
        } else {
            trend = String.format(Locale.US, "§7— %.1f%%", variation);
        }
        assertEquals("§c↓ -8.1%", trend);
    }

    @Test
    void trendStringEstavel() {
        double variation = 0.3;
        String trend;
        if (variation > 0.5) {
            trend = String.format(Locale.US, "§a↑ +%.1f%%", variation);
        } else if (variation < -0.5) {
            trend = String.format(Locale.US, "§c↓ %.1f%%", variation);
        } else {
            trend = String.format(Locale.US, "§7— %.1f%%", variation);
        }
        assertEquals("§7— 0.3%", trend);
    }

    @Test
    void trendStringSemDados() {
        double variation = Double.NaN;
        String trend;
        if (Double.isNaN(variation)) {
            trend = "§7— Sem dados";
        } else {
            trend = "outro";
        }
        assertEquals("§7— Sem dados", trend);
    }
}
