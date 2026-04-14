package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para VipManager.
 * Testa lógica pura de VipTier enum, TierBenefits record, e parseTier.
 * Replica a lógica interna para evitar dependências Bukkit.
 */
class VipManagerTest {

    // --- VipTier enum ---

    @Test
    void vipTierValues() {
        assertEquals(5, VipManager.VipTier.values().length);
    }

    @Test
    void vipTierPriorities() {
        assertEquals(0, VipManager.VipTier.NONE.getPriority());
        assertEquals(1, VipManager.VipTier.VIP.getPriority());
        assertEquals(2, VipManager.VipTier.VIP_PLUS.getPriority());
        assertEquals(3, VipManager.VipTier.ELITE.getPriority());
        assertEquals(4, VipManager.VipTier.LENDARIO.getPriority());
    }

    @Test
    void vipTierLabels() {
        assertEquals("Nenhum", VipManager.VipTier.NONE.getLabel());
        assertEquals("VIP", VipManager.VipTier.VIP.getLabel());
        assertEquals("VIP+", VipManager.VipTier.VIP_PLUS.getLabel());
        assertEquals("ELITE", VipManager.VipTier.ELITE.getLabel());
        assertEquals("LENDÁRIO", VipManager.VipTier.LENDARIO.getLabel());
    }

    @Test
    void vipTierDisplayNames() {
        assertNotNull(VipManager.VipTier.VIP.getDisplayName());
        assertNotNull(VipManager.VipTier.NONE.getDisplayName());
    }

    @Test
    void vipTierOrdenacao() {
        assertTrue(VipManager.VipTier.NONE.getPriority() < VipManager.VipTier.VIP.getPriority());
        assertTrue(VipManager.VipTier.VIP.getPriority() < VipManager.VipTier.VIP_PLUS.getPriority());
        assertTrue(VipManager.VipTier.VIP_PLUS.getPriority() < VipManager.VipTier.ELITE.getPriority());
        assertTrue(VipManager.VipTier.ELITE.getPriority() < VipManager.VipTier.LENDARIO.getPriority());
    }

    // --- TierBenefits record ---

    @Test
    void tierBenefitsRecord() {
        var benefits = new VipManager.TierBenefits(500, 2, Map.of("raro", 1), 0.0);
        assertEquals(500, benefits.extraClaimBlocks());
        assertEquals(2, benefits.extraHomes());
        assertEquals(1, benefits.monthlyKeys().get("raro"));
        assertEquals(0.0, benefits.marketDiscountPercent(), 0.001);
    }

    @Test
    void tierBenefitsEquality() {
        var a = new VipManager.TierBenefits(500, 2, Map.of("raro", 1), 5.0);
        var b = new VipManager.TierBenefits(500, 2, Map.of("raro", 1), 5.0);
        assertEquals(a, b);
    }

    @Test
    void tierBenefitsInequality() {
        var a = new VipManager.TierBenefits(500, 2, Map.of("raro", 1), 5.0);
        var b = new VipManager.TierBenefits(1000, 5, Map.of("raro", 2), 10.0);
        assertNotEquals(a, b);
    }

    @Test
    void tierBenefitsPadroesElite() {
        var benefits = new VipManager.TierBenefits(3000, 10, Map.of("raro", 3, "lendario", 1), 10.0);
        assertEquals(3000, benefits.extraClaimBlocks());
        assertEquals(10, benefits.extraHomes());
        assertEquals(3, benefits.monthlyKeys().get("raro"));
        assertEquals(1, benefits.monthlyKeys().get("lendario"));
        assertEquals(10.0, benefits.marketDiscountPercent(), 0.001);
    }

    @Test
    void tierBenefitsNone() {
        var benefits = new VipManager.TierBenefits(0, 0, Map.of(), 0.0);
        assertEquals(0, benefits.extraClaimBlocks());
        assertEquals(0, benefits.extraHomes());
        assertTrue(benefits.monthlyKeys().isEmpty());
        assertEquals(0.0, benefits.marketDiscountPercent(), 0.001);
    }

    // --- parseTier (lógica replicada) ---

    private VipManager.VipTier parseTier(String name) {
        if (name == null)
            return null;
        return switch (name.toLowerCase().replace("-", "_").replace("+", "_plus")) {
            case "vip" -> VipManager.VipTier.VIP;
            case "vip_plus", "vipplus", "vip+" -> VipManager.VipTier.VIP_PLUS;
            case "elite" -> VipManager.VipTier.ELITE;
            case "lendario", "lendário" -> VipManager.VipTier.LENDARIO;
            case "none", "nenhum" -> VipManager.VipTier.NONE;
            default -> null;
        };
    }

    @Test
    void parseTierVip() {
        assertEquals(VipManager.VipTier.VIP, parseTier("vip"));
        assertEquals(VipManager.VipTier.VIP, parseTier("VIP"));
    }

    @Test
    void parseTierVipPlus() {
        assertEquals(VipManager.VipTier.VIP_PLUS, parseTier("vip_plus"));
        assertEquals(VipManager.VipTier.VIP_PLUS, parseTier("vipplus"));
        assertEquals(VipManager.VipTier.VIP_PLUS, parseTier("VIP-Plus"));
    }

    @Test
    void parseTierElite() {
        assertEquals(VipManager.VipTier.ELITE, parseTier("elite"));
        assertEquals(VipManager.VipTier.ELITE, parseTier("ELITE"));
    }

    @Test
    void parseTierLendario() {
        assertEquals(VipManager.VipTier.LENDARIO, parseTier("lendario"));
        assertEquals(VipManager.VipTier.LENDARIO, parseTier("LENDARIO"));
    }

    @Test
    void parseTierNone() {
        assertEquals(VipManager.VipTier.NONE, parseTier("none"));
        assertEquals(VipManager.VipTier.NONE, parseTier("nenhum"));
    }

    @Test
    void parseTierNull() {
        assertNull(parseTier(null));
    }

    @Test
    void parseTierInvalido() {
        assertNull(parseTier("premium_ultra"));
    }

    // --- Lógica de benefícios referenciada (getExtraClaimBlocks,
    // getMarketDiscount) ---

    @Test
    void extraClaimBlocksViaRecord() {
        var benefits = new VipManager.TierBenefits(1500, 5, Map.of(), 5.0);
        assertEquals(1500, benefits.extraClaimBlocks());
    }

    @Test
    void marketDiscountViaRecord() {
        var benefits = new VipManager.TierBenefits(0, 0, Map.of(), 15.0);
        assertEquals(15.0, benefits.marketDiscountPercent(), 0.001);
    }
}
