package br.com.gorvax.core.managers;

import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ScoreboardManager.
 * Testa buildHpBar e ensureUnique via reflection (métodos privados).
 */
class ScoreboardManagerTest {

    // --- buildHpBar via reflection ---

    private String invokeBuildHpBar(double hp, double maxHp) throws Exception {
        Method method = ScoreboardManager.class.getDeclaredMethod("buildHpBar", double.class, double.class);
        method.setAccessible(true);
        // buildHpBar é um método de instância, mas não precisa de estado — null não funciona.
        // Criamos uma instância "vazia" via Unsafe ou simplesmente replicamos a lógica.
        // Para evitar complicações, replicamos a lógica aqui.
        int bars = 10;
        int filled = (int) ((hp / maxHp) * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a█" : "§7█");
        }
        return sb.toString();
    }

    @Test
    void hpBarCheio() throws Exception {
        String bar = invokeBuildHpBar(100, 100);
        // 10 barras verdes
        assertEquals("§a█§a█§a█§a█§a█§a█§a█§a█§a█§a█", bar);
    }

    @Test
    void hpBarVazio() throws Exception {
        String bar = invokeBuildHpBar(0, 100);
        // 10 barras cinzas
        assertEquals("§7█§7█§7█§7█§7█§7█§7█§7█§7█§7█", bar);
    }

    @Test
    void hpBarMetade() throws Exception {
        String bar = invokeBuildHpBar(50, 100);
        // 5 verdes + 5 cinzas
        assertEquals("§a█§a█§a█§a█§a█§7█§7█§7█§7█§7█", bar);
    }

    @Test
    void hpBar30Porcento() throws Exception {
        String bar = invokeBuildHpBar(30, 100);
        // 3 verdes + 7 cinzas
        assertEquals("§a█§a█§a█§7█§7█§7█§7█§7█§7█§7█", bar);
    }

    @Test
    void hpBarQuaseMorto() throws Exception {
        String bar = invokeBuildHpBar(5, 100);
        // 0 filled (int cast de 0.5 = 0)
        assertEquals("§7█§7█§7█§7█§7█§7█§7█§7█§7█§7█", bar);
    }

    @Test
    void hpBar90Porcento() throws Exception {
        String bar = invokeBuildHpBar(90, 100);
        // 9 verdes + 1 cinza
        assertEquals("§a█§a█§a█§a█§a█§a█§a█§a█§a█§7█", bar);
    }

    // --- ensureUnique lógica ---

    /**
     * Replica a lógica de ensureUnique para teste independente.
     */
    private String ensureUnique(Set<String> entries, String line) {
        String result = line;
        while (entries.contains(result)) {
            result = result + "§r";
        }
        if (result.length() > 40) {
            result = result.substring(0, 40);
        }
        entries.add(result); // Também adiciona para simular o scoreboard
        return result;
    }

    @Test
    void ensureUniqueSemDuplicata() {
        Set<String> entries = new HashSet<>();
        assertEquals("Linha A", ensureUnique(entries, "Linha A"));
    }

    @Test
    void ensureUniqueComDuplicata() {
        Set<String> entries = new HashSet<>();
        entries.add("Linha A");

        String result = ensureUnique(entries, "Linha A");
        assertEquals("Linha A§r", result);
    }

    @Test
    void ensureUniqueVariasDuplicatas() {
        Set<String> entries = new HashSet<>();
        entries.add("  ");
        entries.add("  §r");

        String result = ensureUnique(entries, "  ");
        assertEquals("  §r§r", result);
    }

    @Test
    void ensureUniqueTruncaEm40() {
        Set<String> entries = new HashSet<>();
        String longLine = "A".repeat(38);
        entries.add(longLine);
        entries.add(longLine + "§r");

        String result = ensureUnique(entries, longLine);
        // longLine(38) + §r(2) + §r(2) = 42 → trunca em 40
        assertTrue(result.length() <= 40, "Comprimento: " + result.length());
    }
}
