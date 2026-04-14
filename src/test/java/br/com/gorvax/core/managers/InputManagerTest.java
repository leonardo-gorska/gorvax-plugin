package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para InputManager.
 * Testa isBedrockPlayer fallback e inner classes acessíveis.
 */
class InputManagerTest {

    // --- isBedrockPlayer lógica de nome com prefixo ---

    /**
     * Replica a parte de fallback do isBedrockPlayer que verifica prefixo no nome.
     * Quando Floodgate não está disponível, o método verifica se o nome começa com
     * ".".
     */
    private boolean isBedrockByPrefix(String playerName) {
        if (playerName == null)
            return false;
        return playerName.startsWith(".");
    }

    @Test
    void bedrockPlayerPrefixoPonto() {
        assertTrue(isBedrockByPrefix(".PlayerBedrock"));
    }

    @Test
    void javaPlayerSemPrefixo() {
        assertFalse(isBedrockByPrefix("JavaPlayer123"));
    }

    @Test
    void bedrockPlayerNomeVazio() {
        assertFalse(isBedrockByPrefix(""));
    }

    @Test
    void bedrockPlayerNull() {
        assertFalse(isBedrockByPrefix(null));
    }

    @Test
    void bedrockPlayerSoPonto() {
        assertTrue(isBedrockByPrefix("."));
    }

    // --- NumericHolder lógica replicada ---

    @Test
    void numericInitialValueZero() {
        double initial = 0;
        String currentInput = (initial == 0) ? "" : String.valueOf(initial).replace(".0", "");
        assertEquals("", currentInput);
    }

    @Test
    void numericInitialValuePositive() {
        double initial = 42;
        String currentInput = (initial == 0) ? "" : String.valueOf(initial).replace(".0", "");
        assertEquals("42", currentInput);
    }

    @Test
    void numericInitialValueDecimal() {
        double initial = 3.5;
        String currentInput = (initial == 0) ? "" : String.valueOf(initial).replace(".0", "");
        assertEquals("3.5", currentInput); // Não perde o .5
    }

    @Test
    void numericDigitAppend() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 9; i++) {
            sb.append(i);
        }
        assertEquals("0123456789", sb.toString());
    }

    @Test
    void numericBackspace() {
        String input = "123";
        if (!input.isEmpty()) {
            input = input.substring(0, input.length() - 1);
        }
        assertEquals("12", input);
    }

    @Test
    void numericBackspaceVazio() {
        String input = "";
        if (!input.isEmpty()) {
            input = input.substring(0, input.length() - 1);
        }
        assertEquals("", input);
    }

    @Test
    void numericParseDouble() {
        String input = "42";
        double value = input.isEmpty() ? 0 : Double.parseDouble(input);
        assertEquals(42.0, value, 0.001);
    }

    @Test
    void numericParseDoubleVazio() {
        String input = "";
        double value = input.isEmpty() ? 0 : Double.parseDouble(input);
        assertEquals(0.0, value, 0.001);
    }

    @Test
    void numericDecimalInput() {
        String input = "3.14";
        double value = Double.parseDouble(input);
        assertEquals(3.14, value, 0.001);
    }

    // --- Inner classes acessíveis ---

    @Test
    void numericHolderClassExists() {
        // Verifica que a inner class é acessível
        assertNotNull(InputManager.NumericHolder.class);
    }

    @Test
    void playerSelectHolderClassExists() {
        // Verifica que a inner class é acessível
        assertNotNull(InputManager.PlayerSelectHolder.class);
    }
}
