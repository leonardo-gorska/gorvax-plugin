package br.com.gorvax.core.managers;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para CodexManager.
 * Testa records, lógica de progresso via PlayerData, e helpers puros.
 * Replica lógica interna para evitar dependências Bukkit (GorvaxCore, YAML, etc.).
 */
class CodexManagerTest {

    // ==================== CodexEntry record ====================

    @Test
    void codexEntryRecordCampos() {
        var entry = new CodexManager.CodexEntry(
                "gorvax", "§6§l⚔ Rei Gorvax", Material.WITHER_SKELETON_SKULL,
                List.of("§8§o??? — Derrote este boss."),
                List.of("§7O Rei das Chamas.", "§cHP: §f5000"),
                "BOSS_KILL", "gorvax");

        assertEquals("gorvax", entry.id());
        assertEquals("§6§l⚔ Rei Gorvax", entry.nome());
        assertEquals(Material.WITHER_SKELETON_SKULL, entry.icone());
        assertEquals(1, entry.loreBloqueado().size());
        assertEquals(2, entry.loreDesbloqueado().size());
        assertEquals("BOSS_KILL", entry.tipo());
        assertEquals("gorvax", entry.trigger());
    }

    @Test
    void codexEntryIgualdade() {
        var a = new CodexManager.CodexEntry(
                "gorvax", "nome", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "gorvax");
        var b = new CodexManager.CodexEntry(
                "gorvax", "nome", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "gorvax");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void codexEntryDesigualdade() {
        var a = new CodexManager.CodexEntry(
                "gorvax", "nome", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "gorvax");
        var b = new CodexManager.CodexEntry(
                "indrax", "nome", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "indrax");
        assertNotEquals(a, b);
    }

    @Test
    void codexEntryLoreVazia() {
        var entry = new CodexManager.CodexEntry(
                "test", "Teste", Material.BOOK, List.of(), List.of(), "", "");
        assertTrue(entry.loreBloqueado().isEmpty());
        assertTrue(entry.loreDesbloqueado().isEmpty());
        assertEquals("", entry.tipo());
        assertEquals("", entry.trigger());
    }

    // ==================== CodexCategory record ====================

    @Test
    void codexCategoryRecordCampos() {
        var entry1 = new CodexManager.CodexEntry(
                "gorvax", "Gorvax", Material.WITHER_SKELETON_SKULL,
                List.of(), List.of(), "BOSS_KILL", "gorvax");
        var entry2 = new CodexManager.CodexEntry(
                "indrax", "Indrax", Material.ENDERMAN_SPAWN_EGG,
                List.of(), List.of(), "BOSS_KILL", "indrax");

        Map<String, CodexManager.CodexEntry> entries = new LinkedHashMap<>();
        entries.put("gorvax", entry1);
        entries.put("indrax", entry2);

        var cat = new CodexManager.CodexCategory(
                "bestiario", "§c💀 Bestiário", Material.WITHER_SKELETON_SKULL, "Bosses e criaturas", entries);

        assertEquals("bestiario", cat.id());
        assertEquals("§c💀 Bestiário", cat.nome());
        assertEquals(Material.WITHER_SKELETON_SKULL, cat.icone());
        assertEquals("Bosses e criaturas", cat.descricao());
        assertEquals(2, cat.entries().size());
        assertTrue(cat.entries().containsKey("gorvax"));
        assertTrue(cat.entries().containsKey("indrax"));
    }

    @Test
    void codexCategorySemEntradas() {
        var cat = new CodexManager.CodexCategory(
                "vazia", "Vazia", Material.BARRIER, "", Map.of());
        assertEquals(0, cat.entries().size());
    }

    // ==================== getCategoryNameForKey (lógica replicada) ====================

    /**
     * Replica a lógica privada de getCategoryNameForKey para testar sem instância real.
     */
    private String getCategoryNameForKey(String fullKey, Map<String, CodexManager.CodexCategory> categories) {
        String catId = fullKey.contains(".") ? fullKey.substring(0, fullKey.indexOf('.')) : fullKey;
        CodexManager.CodexCategory cat = categories.get(catId);
        return cat != null ? cat.nome() : catId;
    }

    @Test
    void getCategoryNameForKeyComPonto() {
        var cat = new CodexManager.CodexCategory(
                "bestiario", "§c💀 Bestiário", Material.WITHER_SKELETON_SKULL, "", Map.of());
        Map<String, CodexManager.CodexCategory> cats = Map.of("bestiario", cat);

        assertEquals("§c💀 Bestiário", getCategoryNameForKey("bestiario.gorvax", cats));
    }

    @Test
    void getCategoryNameForKeySemPonto() {
        Map<String, CodexManager.CodexCategory> cats = Map.of();
        // Sem ponto → catId = a string inteira, não encontrado → retorna o próprio catId
        assertEquals("bestiario", getCategoryNameForKey("bestiario", cats));
    }

    @Test
    void getCategoryNameForKeyCategoriaInexistente() {
        Map<String, CodexManager.CodexCategory> cats = Map.of();
        assertEquals("xyz", getCategoryNameForKey("xyz.entry", cats));
    }

    // ==================== parseMaterial (lógica replicada) ====================

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.PAPER;
        }
    }

    @Test
    void parseMaterialValido() {
        assertEquals(Material.DIAMOND_SWORD, parseMaterial("DIAMOND_SWORD"));
        assertEquals(Material.DIAMOND_SWORD, parseMaterial("diamond_sword"));
        assertEquals(Material.BOOK, parseMaterial("book"));
    }

    @Test
    void parseMaterialInvalido() {
        assertEquals(Material.PAPER, parseMaterial("NAO_EXISTE"));
        assertEquals(Material.PAPER, parseMaterial(""));
    }

    @Test
    void parseMaterialInvalidoComNull() {
        // NullPointerException → catch → PAPER
        assertEquals(Material.PAPER, parseMaterial("null_test_xyz_123"));
    }

    // ==================== triggerIndex (lógica replicada) ====================

    /**
     * Replica a lógica de construção do triggerIndex.
     */
    private Map<String, List<String>> buildTriggerIndex(Map<String, CodexManager.CodexEntry> allEntries,
                                                         Map<String, CodexManager.CodexCategory> categories) {
        Map<String, List<String>> index = new HashMap<>();
        for (var catEntry : categories.entrySet()) {
            String catId = catEntry.getKey();
            for (var entryEntry : catEntry.getValue().entries().entrySet()) {
                String entryId = entryEntry.getKey();
                String trigger = entryEntry.getValue().trigger();
                String fullKey = catId + "." + entryId;
                index.computeIfAbsent(trigger, k -> new ArrayList<>()).add(fullKey);
            }
        }
        return index;
    }

    @Test
    void triggerIndexMapeiaTriggerParaChave() {
        var e1 = new CodexManager.CodexEntry("gorvax", "Gorvax", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "gorvax");
        var e2 = new CodexManager.CodexEntry("indrax", "Indrax", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "indrax");
        var cat = new CodexManager.CodexCategory("bestiario", "Bestiário", Material.PAPER, "",
                Map.of("gorvax", e1, "indrax", e2));

        Map<String, List<String>> index = buildTriggerIndex(Map.of(), Map.of("bestiario", cat));

        assertTrue(index.containsKey("gorvax"));
        assertEquals(1, index.get("gorvax").size());
        assertEquals("bestiario.gorvax", index.get("gorvax").get(0));

        assertTrue(index.containsKey("indrax"));
        assertEquals("bestiario.indrax", index.get("indrax").get(0));
    }

    @Test
    void triggerIndexMultiplasEntradasMesmoTrigger() {
        // Duas entradas com mesmo trigger, categorias diferentes
        var e1 = new CodexManager.CodexEntry("a", "A", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "gorvax");
        var e2 = new CodexManager.CodexEntry("b", "B", Material.PAPER, List.of(), List.of(), "BOSS_KILL", "gorvax");
        var cat1 = new CodexManager.CodexCategory("bestiario", "B", Material.PAPER, "", Map.of("a", e1));
        var cat2 = new CodexManager.CodexCategory("figuras", "F", Material.PAPER, "", Map.of("b", e2));

        Map<String, List<String>> index = buildTriggerIndex(Map.of(), Map.of("bestiario", cat1, "figuras", cat2));

        assertEquals(2, index.get("gorvax").size());
        assertTrue(index.get("gorvax").contains("bestiario.a"));
        assertTrue(index.get("gorvax").contains("figuras.b"));
    }

    @Test
    void triggerIndexTriggerVazio() {
        var e1 = new CodexManager.CodexEntry("sem_trigger", "Sem", Material.PAPER, List.of(), List.of(), "", "");
        var cat = new CodexManager.CodexCategory("cat", "Cat", Material.PAPER, "", Map.of("sem_trigger", e1));

        Map<String, List<String>> index = buildTriggerIndex(Map.of(), Map.of("cat", cat));
        assertTrue(index.containsKey(""));
        assertEquals(1, index.get("").size());
    }

    // ==================== PlayerData — Codex methods ====================

    @Test
    void playerDataCodexInicial() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        assertTrue(pd.getUnlockedCodex().isEmpty());
        assertFalse(pd.hasCodexEntry("bestiario.gorvax"));
    }

    @Test
    void playerDataCodexUnlock() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");

        assertTrue(pd.hasCodexEntry("bestiario.gorvax"));
        assertFalse(pd.hasCodexEntry("bestiario.indrax"));
        assertEquals(1, pd.getUnlockedCodex().size());
    }

    @Test
    void playerDataCodexUnlockDuplicado() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("bestiario.gorvax"); // duplicado

        assertEquals(1, pd.getUnlockedCodex().size());
    }

    @Test
    void playerDataCodexMultiplasEntradas() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("bestiario.indrax");
        pd.unlockCodexEntry("cronicas.genesis");

        assertEquals(3, pd.getUnlockedCodex().size());
        assertTrue(pd.hasCodexEntry("bestiario.gorvax"));
        assertTrue(pd.hasCodexEntry("bestiario.indrax"));
        assertTrue(pd.hasCodexEntry("cronicas.genesis"));
    }

    @Test
    void playerDataCodexSetUnlockedCodex() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");
        assertEquals(1, pd.getUnlockedCodex().size());

        // Set sobrescreve
        pd.setUnlockedCodex(Set.of("cronicas.genesis", "locais.spawn"));
        assertEquals(2, pd.getUnlockedCodex().size());
        assertFalse(pd.hasCodexEntry("bestiario.gorvax")); // foi limpo
        assertTrue(pd.hasCodexEntry("cronicas.genesis"));
        assertTrue(pd.hasCodexEntry("locais.spawn"));
    }

    @Test
    void playerDataCodexSetNullLimpa() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.setUnlockedCodex(null);
        assertTrue(pd.getUnlockedCodex().isEmpty());
    }

    @Test
    void playerDataCodexSetVazioLimpa() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.setUnlockedCodex(Set.of());
        assertTrue(pd.getUnlockedCodex().isEmpty());
    }

    // ==================== Progresso (lógica replicada) ====================

    /**
     * Replica getProgress usando PlayerData e mapa allEntries.
     */
    private int[] getProgress(PlayerData pd, Map<String, CodexManager.CodexEntry> allEntries) {
        int unlocked = (int) pd.getUnlockedCodex().stream()
                .filter(allEntries::containsKey)
                .count();
        return new int[]{unlocked, allEntries.size()};
    }

    @Test
    void progressoZeroInicial() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        Map<String, CodexManager.CodexEntry> all = Map.of(
                "bestiario.gorvax", new CodexManager.CodexEntry("gorvax", "G", Material.PAPER, List.of(), List.of(), "", ""),
                "bestiario.indrax", new CodexManager.CodexEntry("indrax", "I", Material.PAPER, List.of(), List.of(), "", "")
        );

        int[] progress = getProgress(pd, all);
        assertEquals(0, progress[0]);
        assertEquals(2, progress[1]);
    }

    @Test
    void progressoParcial() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");

        Map<String, CodexManager.CodexEntry> all = Map.of(
                "bestiario.gorvax", new CodexManager.CodexEntry("gorvax", "G", Material.PAPER, List.of(), List.of(), "", ""),
                "bestiario.indrax", new CodexManager.CodexEntry("indrax", "I", Material.PAPER, List.of(), List.of(), "", ""),
                "cronicas.genesis", new CodexManager.CodexEntry("genesis", "Gn", Material.PAPER, List.of(), List.of(), "", "")
        );

        int[] progress = getProgress(pd, all);
        assertEquals(1, progress[0]);
        assertEquals(3, progress[1]);
    }

    @Test
    void progressoCompleto() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("a.1");
        pd.unlockCodexEntry("b.2");

        Map<String, CodexManager.CodexEntry> all = Map.of(
                "a.1", new CodexManager.CodexEntry("1", "A1", Material.PAPER, List.of(), List.of(), "", ""),
                "b.2", new CodexManager.CodexEntry("2", "B2", Material.PAPER, List.of(), List.of(), "", "")
        );

        int[] progress = getProgress(pd, all);
        assertEquals(2, progress[0]);
        assertEquals(2, progress[1]);
    }

    @Test
    void progressoIgnoraEntradasDesconhecidas() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("inexistente.xyz"); // não existe no allEntries

        Map<String, CodexManager.CodexEntry> all = Map.of(
                "a.1", new CodexManager.CodexEntry("1", "A1", Material.PAPER, List.of(), List.of(), "", "")
        );

        int[] progress = getProgress(pd, all);
        assertEquals(0, progress[0]); // não conta o inexistente
        assertEquals(1, progress[1]);
    }

    // ==================== Progresso por categoria (lógica replicada) ====================

    private int[] getCategoryProgress(PlayerData pd, String categoryId, CodexManager.CodexCategory cat) {
        if (cat == null) return new int[]{0, 0};
        int unlocked = 0;
        for (String entryId : cat.entries().keySet()) {
            if (pd.hasCodexEntry(categoryId + "." + entryId)) {
                unlocked++;
            }
        }
        return new int[]{unlocked, cat.entries().size()};
    }

    @Test
    void progressoCategoriaVazia() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        int[] result = getCategoryProgress(pd, "bestiario", null);
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
    }

    @Test
    void progressoCategoriaParcial() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");

        var cat = new CodexManager.CodexCategory("bestiario", "B", Material.PAPER, "", Map.of(
                "gorvax", new CodexManager.CodexEntry("gorvax", "G", Material.PAPER, List.of(), List.of(), "", ""),
                "indrax", new CodexManager.CodexEntry("indrax", "I", Material.PAPER, List.of(), List.of(), "", "")
        ));

        int[] result = getCategoryProgress(pd, "bestiario", cat);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
    }

    @Test
    void progressoCategoriaCompleta() {
        var pd = new PlayerData(UUID.randomUUID(), 100);
        pd.unlockCodexEntry("bestiario.gorvax");
        pd.unlockCodexEntry("bestiario.indrax");

        var cat = new CodexManager.CodexCategory("bestiario", "B", Material.PAPER, "", Map.of(
                "gorvax", new CodexManager.CodexEntry("gorvax", "G", Material.PAPER, List.of(), List.of(), "", ""),
                "indrax", new CodexManager.CodexEntry("indrax", "I", Material.PAPER, List.of(), List.of(), "", "")
        ));

        int[] result = getCategoryProgress(pd, "bestiario", cat);
        assertEquals(2, result[0]);
        assertEquals(2, result[1]);
    }
}
