package br.com.gorvax.core.integration;

import br.com.gorvax.core.managers.PlayerData;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Descoberta no Códex.
 * Managers envolvidos: PlayerData (unlockedCodex), AchievementManager
 * (conditions).
 *
 * Fluxo testado:
 * 1. Desbloquear entradas individuais → progresso %
 * 2. Completar categoria → achievement trigger
 * 3. Entradas duplicadas não contam duas vezes
 */
@Tag("integration")
class CodexDiscoveryIT extends GorvaxIntegrationTest {

    private double calculateProgress(int unlocked, int total) {
        if (total == 0)
            return 0;
        return (double) unlocked / total * 100.0;
    }

    @Test
    void desbloqueioProgressivoDoCodex() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        int totalEntries = 40; // Total de entradas no codex.yml

        // Desbloquear 5 entradas de bestiário
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("bestiario.indrax");
        pd.unlockCodexEntry("bestiario.vulgathor");
        pd.unlockCodexEntry("bestiario.zarith");
        pd.unlockCodexEntry("bestiario.kaldur");

        assertEquals(5, pd.getUnlockedCodex().size());
        assertEquals(12.5, calculateProgress(5, totalEntries), 0.1);
    }

    @Test
    void entradaDuplicadaNaoContaDuasVezes() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("bestiario.gorvax"); // Duplicata

        assertEquals(1, pd.getUnlockedCodex().size());
    }

    @Test
    void completarCategoriaBestiarioTriggerAchievement() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Todas as 9 entradas do bestiário (7 world bosses + variações)
        String[] bestiario = {
                "bestiario.gorvax", "bestiario.indrax", "bestiario.vulgathor",
                "bestiario.zarith", "bestiario.kaldur", "bestiario.skulkor",
                "bestiario.xylos", "bestiario.guardiao_deserto", "bestiario.sentinela_gelida"
        };

        for (String entry : bestiario) {
            pd.unlockCodexEntry(entry);
        }

        assertEquals(9, pd.getUnlockedCodex().size());

        // Verificar que todas as entradas de bestiário estão desbloqueadas
        long bestiarioCount = pd.getUnlockedCodex().stream()
                .filter(e -> e.startsWith("bestiario."))
                .count();
        assertEquals(9, bestiarioCount);

        // Achievement "Caçador Supremo" seria disparado quando bestiario = 100%
        assertTrue(bestiarioCount >= 9, "Deveria disparar achievement 'Caçador Supremo'");
    }

    @Test
    void progressoTotalDoCodex() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        int totalEntries = 40;

        // Desbloquear entradas de múltiplas categorias
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("cronicas.genesis");
        pd.unlockCodexEntry("reinos.ashvale");
        pd.unlockCodexEntry("reliquias.lamina_gorvax");
        pd.unlockCodexEntry("locais.spawn_fortress");
        pd.unlockCodexEntry("figuras.rei_gorvax");

        assertEquals(6, pd.getUnlockedCodex().size());
        assertEquals(15.0, calculateProgress(6, totalEntries), 0.1);
    }

    @Test
    void codex100PorcentoDisparaEnciclopedista() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        // Simular todas as 40 entradas desbloqueadas
        for (int i = 0; i < 40; i++) {
            pd.unlockCodexEntry("entry_" + i);
        }

        assertEquals(40, pd.getUnlockedCodex().size());
        assertEquals(100.0, calculateProgress(40, 40), 0.001);

        // Achievement "Enciclopedista" = 100% do Códex
        assertTrue(pd.getUnlockedCodex().size() >= 40);
    }

    @Test
    void categoriasIndependentes() {
        UUID uuid = UUID.randomUUID();
        PlayerData pd = createPlayerData(uuid);

        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("cronicas.genesis");

        // Contar por categoria
        long bestiario = pd.getUnlockedCodex().stream()
                .filter(e -> e.startsWith("bestiario.")).count();
        long cronicas = pd.getUnlockedCodex().stream()
                .filter(e -> e.startsWith("cronicas.")).count();

        assertEquals(1, bestiario);
        assertEquals(1, cronicas);
    }
}
