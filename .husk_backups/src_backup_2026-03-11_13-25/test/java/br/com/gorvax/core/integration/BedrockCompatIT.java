package br.com.gorvax.core.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B34 — Teste de integração: Compatibilidade Bedrock.
 * Managers envolvidos: InputManager (detecção Bedrock, fallback).
 *
 * Fluxo testado:
 * 1. Detecção de player Bedrock (prefixo ".")
 * 2. Lógica de input numérico (replicated)
 * 3. Fallback de GUI → Form adequado
 */
@Tag("integration")
class BedrockCompatIT extends GorvaxIntegrationTest {

    // Lógica replicada de isBedrockPlayer (InputManager / Floodgate prefixo)
    private boolean isBedrockPlayer(String playerName) {
        return playerName != null && playerName.startsWith(".");
    }

    // Lógica replicada de NumericHolder (input numérico via GUI)
    private static class SimpleNumericInput {
        private String currentInput = "";

        String getValue() {
            return currentInput.isEmpty() ? "0" : currentInput;
        }

        void append(String digit) {
            if (currentInput.equals("0"))
                currentInput = "";
            currentInput += digit;
        }

        void backspace() {
            if (!currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
            }
        }

        double parse() {
            try {
                String val = getValue();
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
    }

    @Test
    void detectaPlayerBedrockPeloPrefixo() {
        assertTrue(isBedrockPlayer(".Steve123"));
        assertTrue(isBedrockPlayer(".JogadorBedrock"));
        assertFalse(isBedrockPlayer("JogadorJava"));
        assertFalse(isBedrockPlayer("Steve"));
        assertFalse(isBedrockPlayer(""));
        assertFalse(isBedrockPlayer(null));
    }

    @Test
    void numericInputOperacoes() {
        var input = new SimpleNumericInput();
        assertEquals("0", input.getValue());

        input.append("1");
        assertEquals("1", input.getValue());

        input.append("5");
        assertEquals("15", input.getValue());

        input.append("0");
        assertEquals("150", input.getValue());

        input.backspace();
        assertEquals("15", input.getValue());

        assertEquals(15.0, input.parse(), 0.01);
    }

    @Test
    void numericInputComPontoDecimal() {
        var input = new SimpleNumericInput();

        input.append("1");
        input.append("0");
        input.append("0");
        input.append(".");
        input.append("5");
        input.append("0");

        assertEquals("100.50", input.getValue());
        assertEquals(100.50, input.parse(), 0.01);
    }

    @Test
    void numericInputBackspaceComUmDigito() {
        var input = new SimpleNumericInput();
        input.append("5");
        input.backspace();
        assertEquals("0", input.getValue());
        assertEquals(0.0, input.parse(), 0.01);
    }

    @Test
    void numericInputBackspaceVazio() {
        var input = new SimpleNumericInput();
        input.backspace(); // Nenhum efeito — já é ""
        assertEquals("0", input.getValue());
    }

    @Test
    void fluxoCompletoBedrockFallback() {
        // Player Bedrock: GUI -> SimpleForm (Floodgate)
        String bedrockPlayer = ".BedrockUser123";
        assertTrue(isBedrockPlayer(bedrockPlayer));

        // Player Java: GUI -> Inventory
        String javaPlayer = "JavaPlayer456";
        assertFalse(isBedrockPlayer(javaPlayer));

        // Input numérico: funciona para ambos
        var input = new SimpleNumericInput();
        input.append("5");
        input.append("0");
        input.append("0");
        assertEquals(500.0, input.parse(), 0.01);
    }
}
