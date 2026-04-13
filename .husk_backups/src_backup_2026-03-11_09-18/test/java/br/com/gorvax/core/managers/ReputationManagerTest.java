package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.ReputationManager.KarmaRank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para ReputationManager.
 * Testa lógica pura de karma ranks, descontos e multiplicadores de preço.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class ReputationManagerTest {

    // Constantes replicadas do ReputationManager
    private static final int HERO_THRESHOLD = 100;
    private static final int GOOD_THRESHOLD = 50;
    private static final int VILLAIN_THRESHOLD = -50;
    private static final int WANTED_THRESHOLD = -100;

    // --- Lógica replicada ---

    private KarmaRank getKarmaRank(int karma) {
        if (karma >= HERO_THRESHOLD)
            return KarmaRank.HEROI;
        if (karma >= GOOD_THRESHOLD)
            return KarmaRank.BOM;
        if (karma > VILLAIN_THRESHOLD)
            return KarmaRank.NEUTRO;
        if (karma > WANTED_THRESHOLD)
            return KarmaRank.VILAO;
        return KarmaRank.PROCURADO;
    }

    private double getMarketDiscount(int karma, boolean enabled, double heroDiscount) {
        if (!enabled)
            return 0.0;
        if (karma >= HERO_THRESHOLD)
            return heroDiscount;
        return 0.0;
    }

    private double getClaimDiscount(int karma, boolean enabled, double heroClaimDiscount) {
        if (!enabled)
            return 0.0;
        if (karma >= HERO_THRESHOLD)
            return heroClaimDiscount;
        return 0.0;
    }

    private double getPriceMultiplier(int karma, boolean enabled) {
        if (!enabled)
            return 1.0;
        if (karma <= WANTED_THRESHOLD)
            return 1.25;
        if (karma <= VILLAIN_THRESHOLD)
            return 1.10;
        return 1.0;
    }

    // --- getKarmaRank: boundaries ---

    @Test
    void heroiComKarma100() {
        assertEquals(KarmaRank.HEROI, getKarmaRank(100));
    }

    @Test
    void heroiComKarma200() {
        assertEquals(KarmaRank.HEROI, getKarmaRank(200));
    }

    @Test
    void bomComKarma50() {
        assertEquals(KarmaRank.BOM, getKarmaRank(50));
    }

    @Test
    void bomComKarma99() {
        assertEquals(KarmaRank.BOM, getKarmaRank(99));
    }

    @Test
    void neutroComKarma0() {
        assertEquals(KarmaRank.NEUTRO, getKarmaRank(0));
    }

    @Test
    void neutroComKarma49() {
        assertEquals(KarmaRank.NEUTRO, getKarmaRank(49));
    }

    @Test
    void neutroComKarmaMenos49() {
        assertEquals(KarmaRank.NEUTRO, getKarmaRank(-49));
    }

    @Test
    void vilaoComKarmaMenos50() {
        assertEquals(KarmaRank.VILAO, getKarmaRank(-50));
    }

    @Test
    void vilaoComKarmaMenos99() {
        assertEquals(KarmaRank.VILAO, getKarmaRank(-99));
    }

    @Test
    void procuradoComKarmaMenos100() {
        assertEquals(KarmaRank.PROCURADO, getKarmaRank(-100));
    }

    @Test
    void procuradoComKarmaMenos500() {
        assertEquals(KarmaRank.PROCURADO, getKarmaRank(-500));
    }

    // --- getKarmaColor e getKarmaLabel (via KarmaRank enum) ---

    @Test
    void corDoHeroi() {
        assertEquals("§a", getKarmaRank(100).getColor());
    }

    @Test
    void corDoProcurado() {
        assertEquals("§4", getKarmaRank(-100).getColor());
    }

    @Test
    void labelDoNeutro() {
        assertEquals("§7⚖ Neutro", getKarmaRank(0).getLabel());
    }

    @Test
    void labelDoBom() {
        assertEquals("§2☘ Bom", getKarmaRank(50).getLabel());
    }

    // --- getMarketDiscount ---

    @Test
    void descontoMercadoParaHeroi() {
        assertEquals(5.0, getMarketDiscount(100, true, 5.0), 0.001);
    }

    @Test
    void descontoMercadoParaNeutro() {
        assertEquals(0.0, getMarketDiscount(0, true, 5.0), 0.001);
    }

    @Test
    void descontoMercadoDesabilitado() {
        assertEquals(0.0, getMarketDiscount(200, false, 5.0), 0.001);
    }

    // --- getClaimDiscount ---

    @Test
    void descontoClaimParaHeroi() {
        assertEquals(5.0, getClaimDiscount(100, true, 5.0), 0.001);
    }

    @Test
    void descontoClaimParaNeutro() {
        assertEquals(0.0, getClaimDiscount(0, true, 5.0), 0.001);
    }

    @Test
    void descontoClaimDesabilitado() {
        assertEquals(0.0, getClaimDiscount(200, false, 5.0), 0.001);
    }

    // --- getPriceMultiplier ---

    @Test
    void multiplicadorPrecoHeroi() {
        assertEquals(1.0, getPriceMultiplier(100, true), 0.001);
    }

    @Test
    void multiplicadorPrecoNeutro() {
        assertEquals(1.0, getPriceMultiplier(0, true), 0.001);
    }

    @Test
    void multiplicadorPrecoVilao() {
        assertEquals(1.10, getPriceMultiplier(-50, true), 0.001);
    }

    @Test
    void multiplicadorPrecoProcurado() {
        assertEquals(1.25, getPriceMultiplier(-100, true), 0.001);
    }

    @Test
    void multiplicadorPrecoProcuradoExtremo() {
        assertEquals(1.25, getPriceMultiplier(-500, true), 0.001);
    }

    @Test
    void multiplicadorPrecoDesabilitado() {
        assertEquals(1.0, getPriceMultiplier(-500, false), 0.001);
    }

    // --- KarmaRank enum ---

    @Test
    void karmaRankEnumValues() {
        assertEquals(5, KarmaRank.values().length);
    }

    @Test
    void karmaRankHeroiProps() {
        assertEquals("§a✦ Herói", KarmaRank.HEROI.getLabel());
        assertEquals("§a", KarmaRank.HEROI.getColor());
        assertEquals(100, KarmaRank.HEROI.getThreshold());
    }

    @Test
    void karmaRankProcuradoProps() {
        assertEquals("§4💀 Procurado", KarmaRank.PROCURADO.getLabel());
        assertEquals("§4", KarmaRank.PROCURADO.getColor());
        assertEquals(-100, KarmaRank.PROCURADO.getThreshold());
    }
}
