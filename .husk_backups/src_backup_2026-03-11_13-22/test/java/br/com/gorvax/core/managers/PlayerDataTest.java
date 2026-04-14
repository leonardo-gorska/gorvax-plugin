package br.com.gorvax.core.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para PlayerData.
 * Lógica pura — sem mocks necessários.
 */
class PlayerDataTest {

    private PlayerData data;
    private UUID playerUUID;

    @BeforeEach
    void setUp() {
        playerUUID = UUID.randomUUID();
        data = new PlayerData(playerUUID, 100);
    }

    // --- Criação ---

    @Test
    void criacaoComBlocosIniciais() {
        assertEquals(playerUUID, data.getUuid());
        assertEquals(100, data.getClaimBlocks());
        assertFalse(data.hasKingRank());
        assertEquals("", data.getActiveTitle());
    }

    // --- Claim Blocks ---

    @Test
    void addClaimBlocksIncrementaCorretamente() {
        data.addClaimBlocks(50);
        assertEquals(150, data.getClaimBlocks());
    }

    @Test
    void removeClaimBlocksSucesso() {
        assertTrue(data.removeClaimBlocks(30));
        assertEquals(70, data.getClaimBlocks());
    }

    @Test
    void removeClaimBlocksInsuficiente() {
        assertFalse(data.removeClaimBlocks(200));
        assertEquals(100, data.getClaimBlocks()); // Não alterou
    }

    @Test
    void removeClaimBlocksExatamenteIgual() {
        assertTrue(data.removeClaimBlocks(100));
        assertEquals(0, data.getClaimBlocks());
    }

    // --- Estatísticas ---

    @Test
    void incrementoDeKills() {
        assertEquals(0, data.getTotalKills());
        data.incrementKills();
        data.incrementKills();
        assertEquals(2, data.getTotalKills());
    }

    @Test
    void incrementoDeDeaths() {
        data.incrementDeaths();
        assertEquals(1, data.getTotalDeaths());
    }

    @Test
    void incrementoBlocos() {
        data.incrementBlocksBroken();
        data.incrementBlocksPlaced();
        data.incrementBlocksPlaced();
        assertEquals(1, data.getTotalBlocksBroken());
        assertEquals(2, data.getTotalBlocksPlaced());
    }

    @Test
    void incrementoBosses() {
        data.incrementBossesKilled();
        data.incrementBossTopDamage();
        assertEquals(1, data.getBossesKilled());
        assertEquals(1, data.getBossTopDamage());
    }

    // --- PlayTime ---

    @Test
    void addPlayTimeAcumulativo() {
        data.addPlayTime(60000L);  // 1 min
        data.addPlayTime(120000L); // 2 min
        assertEquals(180000L, data.getTotalPlayTime());
    }

    // --- Dinheiro ---

    @Test
    void addMoneyEarnedESpent() {
        data.addMoneyEarned(1500.50);
        data.addMoneySpent(300.25);
        assertEquals(1500.50, data.getTotalMoneyEarned(), 0.001);
        assertEquals(300.25, data.getTotalMoneySpent(), 0.001);
    }

    // --- Títulos ---

    @Test
    void unlockTitulosSemDuplicata() {
        data.addUnlockedTitle("Guerreiro");
        data.addUnlockedTitle("Guerreiro"); // Duplicata
        data.addUnlockedTitle("Lendário");
        assertEquals(2, data.getUnlockedTitles().size());
        assertTrue(data.getUnlockedTitles().contains("Guerreiro"));
        assertTrue(data.getUnlockedTitles().contains("Lendário"));
    }

    @Test
    void activeTitleNullViraVazio() {
        data.setActiveTitle("Rei");
        assertEquals("Rei", data.getActiveTitle());
        data.setActiveTitle(null);
        assertEquals("", data.getActiveTitle());
    }

    // --- Conquistas ---

    @Test
    void unlockAchievementSemDuplicata() {
        data.unlockAchievement("first_kill");
        data.unlockAchievement("first_kill"); // Duplicata — não deve substituir timestamp
        assertTrue(data.hasAchievement("first_kill"));
        assertFalse(data.hasAchievement("outro"));
        assertEquals(1, data.getAchievements().size());
    }

    // --- BorderSound ---

    @Test
    void toggleBorderSound() {
        assertTrue(data.isBorderSound()); // Padrão = true
        data.setBorderSound(false);
        assertFalse(data.isBorderSound());
    }

    // --- Outpost (transient) ---

    @Test
    void outpostFlags() {
        assertFalse(data.isNextClaimIsOutpost());
        assertNull(data.getOutpostParentKingdomId());

        data.setNextClaimIsOutpost(true);
        data.setOutpostParentKingdomId("reino-1");

        assertTrue(data.isNextClaimIsOutpost());
        assertEquals("reino-1", data.getOutpostParentKingdomId());
    }

    // --- B4: Tutorial Interativo + Welcome Kit ---

    @Test
    void tutorialStepDefaultZero() {
        assertEquals(0, data.getTutorialStep());
    }

    @Test
    void tutorialStepSetEGet() {
        data.setTutorialStep(3);
        assertEquals(3, data.getTutorialStep());
        data.setTutorialStep(7);
        assertEquals(7, data.getTutorialStep());
    }

    @Test
    void hasReceivedKitDefaultFalse() {
        assertFalse(data.hasReceivedKit());
    }

    @Test
    void hasReceivedKitToggle() {
        data.setHasReceivedKit(true);
        assertTrue(data.hasReceivedKit());
        data.setHasReceivedKit(false);
        assertFalse(data.hasReceivedKit());
    }

    @Test
    void tutorialCompletedDefaultFalse() {
        assertFalse(data.isTutorialCompleted());
    }

    @Test
    void tutorialCompletedToggle() {
        data.setTutorialCompleted(true);
        assertTrue(data.isTutorialCompleted());
    }
}
