package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ReputationManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Fluxo econômico.
 * Managers envolvidos: PlayerData (stats), ReputationManager
 * (descontos/penalidades).
 *
 * Fluxo testado:
 * 1. Jogador ganha/gasta dinheiro → stats atualizados
 * 2. Karma afeta descontos e multiplicadores de preço
 * 3. Diferentes ranks de karma resultam em preços diferentes
 */
@Tag("integration")
class EconomyFlowIT extends GorvaxIntegrationTest {

    @Test
    void fluxoEconomicoComKarmaHeroi() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Jogador ganha dinheiro via mercado e bosses
        pd.addMoneyEarned(5000.0);
        pd.addKarma(120); // Herói

        // Stats de economia acumulados
        assertEquals(5000.0, pd.getTotalMoneyEarned(), 0.01);
        assertEquals(0.0, pd.getTotalMoneySpent(), 0.01);

        // Herói: 5% desconto
        double discount = getMarketDiscount(pd.getKarma(), 5.0);
        assertEquals(5.0, discount, 0.001);

        // Preço para herói: sem multiplicador
        double multiplier = getPriceMultiplier(pd.getKarma());
        assertEquals(1.0, multiplier, 0.001);

        // Compra item de R$1000 com desconto
        double basePrice = 1000.0;
        double finalPrice = basePrice * (1 - discount / 100) * multiplier;
        assertEquals(950.0, finalPrice, 0.01);

        // Registra gasto
        pd.addMoneySpent(finalPrice);
        assertEquals(950.0, pd.getTotalMoneySpent(), 0.01);
    }

    @Test
    void fluxoEconomicoComKarmaVilao() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        pd.addKarma(-60); // Vilão
        assertEquals(ReputationManager.KarmaRank.VILAO, getKarmaRank(pd.getKarma()));

        // Vilão: sem desconto, +10% no preço
        double discount = getMarketDiscount(pd.getKarma(), 5.0);
        assertEquals(0.0, discount, 0.001);

        double multiplier = getPriceMultiplier(pd.getKarma());
        assertEquals(1.10, multiplier, 0.001);

        // Compra item de R$1000 com penalidade
        double basePrice = 1000.0;
        double finalPrice = basePrice * (1 - discount / 100) * multiplier;
        assertEquals(1100.0, finalPrice, 0.01);
    }

    @Test
    void fluxoEconomicoComKarmaProcurado() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        pd.addKarma(-120); // Procurado
        assertEquals(ReputationManager.KarmaRank.PROCURADO, getKarmaRank(pd.getKarma()));

        // Procurado: +25% no preço
        double multiplier = getPriceMultiplier(pd.getKarma());
        assertEquals(1.25, multiplier, 0.001);

        double basePrice = 1000.0;
        double finalPrice = basePrice * multiplier;
        assertEquals(1250.0, finalPrice, 0.01);
    }

    @Test
    void transicaoDeKarmaAfetaPrecos() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Começa neutro
        assertEquals(1.0, getPriceMultiplier(pd.getKarma()), 0.001);

        // Mata jogadores → karma cai
        pd.addKarma(-55); // Vilão
        assertEquals(1.10, getPriceMultiplier(pd.getKarma()), 0.001);

        // Continua matando → karma cai mais
        pd.addKarma(-50); // Total: -105, Procurado
        assertEquals(1.25, getPriceMultiplier(pd.getKarma()), 0.001);

        // Faz boas ações → karma sobe
        pd.addKarma(110); // Total: 5, Neutro
        assertEquals(1.0, getPriceMultiplier(pd.getKarma()), 0.001);

        // Continua ajudando → Herói
        pd.addKarma(100); // Total: 105, Herói
        assertEquals(5.0, getMarketDiscount(pd.getKarma(), 5.0), 0.001);
    }

    @Test
    void balanceTrackerAcumulaCorretamente() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Múltiplas transações de ganho
        pd.addMoneyEarned(1000.0);
        pd.addMoneyEarned(2500.0);
        pd.addMoneyEarned(750.0);
        assertEquals(4250.0, pd.getTotalMoneyEarned(), 0.01);

        // Múltiplas transações de gasto
        pd.addMoneySpent(500.0);
        pd.addMoneySpent(1200.0);
        assertEquals(1700.0, pd.getTotalMoneySpent(), 0.01);

        // Lucro líquido
        double profit = pd.getTotalMoneyEarned() - pd.getTotalMoneySpent();
        assertEquals(2550.0, profit, 0.01);
    }
}
