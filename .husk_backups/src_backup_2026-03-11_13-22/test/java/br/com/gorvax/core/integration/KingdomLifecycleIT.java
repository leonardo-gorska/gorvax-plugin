package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ReputationManager;
import br.com.gorvax.core.towns.managers.KingdomManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Ciclo de vida completo de um Reino.
 * Managers envolvidos: KingdomManager, ClaimManager (lógica), WarManager
 * (lógica).
 *
 * Fluxo testado:
 * 1. Criar reino com poucos membros → rank "Acampamento"
 * 2. Crescer membros → rank sobe para "Vila", "Reino", "Império"
 * 3. XP bonus aumenta proporcionalmente
 * 4. Diplomacia: alianças (invite records)
 * 5. Guerra: estado pós-surrender
 */
@Tag("integration")
class KingdomLifecycleIT extends GorvaxIntegrationTest {

    // ===== Fluxo 1: Progressão de Rank do Reino =====

    @Test
    void reinoProgressaDeAcampamentoAImperio() {
        // Inicia como acampamento (0 súditos)
        assertEquals("§7Acampamento", getKingdomRank(0));

        // Cresce para vila (10 súditos)
        int membros = 10;
        assertEquals("§b§lVila", getKingdomRank(membros));
        assertEquals(0.04, getPassiveXpBonus(membros), 0.001);

        // Cresce para reino (20 súditos)
        membros = 20;
        assertEquals("§e§lReino", getKingdomRank(membros));
        assertEquals(0.08, getPassiveXpBonus(membros), 0.001);

        // Cresce para império (50 súditos)
        membros = 50;
        assertEquals("§6§lImpério", getKingdomRank(membros));
        assertEquals(0.20, getPassiveXpBonus(membros), 0.001);
    }

    // ===== Fluxo 2: XP Bonus escala com membros =====

    @Test
    void xpBonusProporcionalAosMembros() {
        // Simula progressão de 0 a 60 membros, verificando XP bonus em cada checkpoint
        int[] checkpoints = { 0, 4, 5, 10, 15, 20, 30, 50, 60 };
        double[] expectedBonus = { 0.0, 0.0, 0.02, 0.04, 0.06, 0.08, 0.12, 0.20, 0.24 };

        for (int i = 0; i < checkpoints.length; i++) {
            assertEquals(expectedBonus[i], getPassiveXpBonus(checkpoints[i]), 0.001,
                    "XP bonus incorreto para " + checkpoints[i] + " membros");
        }
    }

    // ===== Fluxo 3: Invite records — diplomacia =====

    @Test
    void inviteRecordsCriadosEValidados() {
        UUID inviter = UUID.randomUUID();
        long now = System.currentTimeMillis();

        // Criar e validar invite
        var invite = new KingdomManager.Invite("reino_alpha", inviter, now);
        assertEquals("reino_alpha", invite.kingdomId());
        assertEquals(inviter, invite.inviter());

        // Criar outro invite para outro reino — devem ser diferentes
        var invite2 = new KingdomManager.Invite("reino_beta", inviter, now);
        assertNotEquals(invite, invite2);
    }

    // ===== Fluxo 4: Alliance proposals =====

    @Test
    void allianceProposalsCicloDiplomatico() {
        long now = System.currentTimeMillis();

        // Proposta de aliança A → B
        var proposal1 = new KingdomManager.AllianceProposal("reino_alpha", "reino_beta", now);
        assertEquals("reino_alpha", proposal1.fromKingdomId());
        assertEquals("reino_beta", proposal1.toKingdomId());

        // Proposta reversa B → A — devem ser diferentes
        var proposal2 = new KingdomManager.AllianceProposal("reino_beta", "reino_alpha", now);
        assertNotEquals(proposal1, proposal2);

        // Mesma proposta = iguais (idempotência)
        var proposal3 = new KingdomManager.AllianceProposal("reino_alpha", "reino_beta", now);
        assertEquals(proposal1, proposal3);
    }

    // ===== Fluxo 5: Rank + XP: fronteiras exatas =====

    @Test
    void fronteiraExataEntreRanksDeReino() {
        // 9 súditos = Acampamento, 10 = Vila
        assertEquals("§7Acampamento", getKingdomRank(9));
        assertEquals("§b§lVila", getKingdomRank(10));

        // 19 = Vila, 20 = Reino
        assertEquals("§b§lVila", getKingdomRank(19));
        assertEquals("§e§lReino", getKingdomRank(20));

        // 49 = Reino, 50 = Império
        assertEquals("§e§lReino", getKingdomRank(49));
        assertEquals("§6§lImpério", getKingdomRank(50));
    }

    // ===== Fluxo 6: Crescimento com karma dos membros =====

    @Test
    void crescimentoDoReinoAfetaKarmaEEconomia() {
        UUID owner = UUID.randomUUID();
        PlayerData pd = createPlayerData(owner);

        // Rei herói: karma alto → desconto
        pd.addKarma(120);
        assertEquals(ReputationManager.KarmaRank.HEROI, getKarmaRank(pd.getKarma()));
        assertEquals(5.0, getMarketDiscount(pd.getKarma(), 5.0), 0.001);

        // Rei com império: verifica que rank e karma são independentes
        assertEquals("§6§lImpério", getKingdomRank(50));
        assertEquals(1.0, getPriceMultiplier(pd.getKarma()), 0.001);
    }
}
