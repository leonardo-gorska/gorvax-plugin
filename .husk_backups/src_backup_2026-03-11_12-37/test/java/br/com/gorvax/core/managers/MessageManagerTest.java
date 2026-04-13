package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para MessageManager.
 * Testa a lógica de substituição de placeholders posicionais {0}, {1}, etc.
 * Isolado do plugin — replica a lógica pura de formatação.
 */
class MessageManagerTest {

    /**
     * Replica a lógica de substituição de placeholders de MessageManager.get().
     */
    private String formatMessage(String template, Object... args) {
        if (template == null) return "§c[Mensagem ausente]";

        String msg = template;
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return msg;
    }

    // --- Substituição simples ---

    @Test
    void semPlaceholders() {
        assertEquals("Olá mundo!", formatMessage("Olá mundo!"));
    }

    @Test
    void umPlaceholder() {
        assertEquals("Bem-vindo, Gorska!", formatMessage("Bem-vindo, {0}!", "Gorska"));
    }

    @Test
    void doisPlaceholders() {
        assertEquals("Gorska comprou diamond por $100",
                formatMessage("{0} comprou {1} por ${2}", "Gorska", "diamond", 100));
    }

    @Test
    void placeholderNumerico() {
        assertEquals("Você tem 42 blocos.", formatMessage("Você tem {0} blocos.", 42));
    }

    @Test
    void placeholderDouble() {
        assertEquals("Saldo: 1500.5", formatMessage("Saldo: {0}", 1500.5));
    }

    // --- Casos especiais ---

    @Test
    void placeholderNull() {
        assertEquals("Valor: null", formatMessage("Valor: {0}", (Object) null));
    }

    @Test
    void mensagemNull() {
        assertEquals("§c[Mensagem ausente]", formatMessage(null));
    }

    @Test
    void placeholderNaoEncontrado() {
        // Se o template tem {0} mas nenhum arg é passado, ele permanece
        assertEquals("Olá {0}!", formatMessage("Olá {0}!"));
    }

    @Test
    void argsExcedentes() {
        // Se mais args do que placeholders, ignora os extras
        assertEquals("Olá Gorska!", formatMessage("Olá {0}!", "Gorska", "ExtraArg"));
    }

    @Test
    void placeholderRepetido() {
        assertEquals("AAA", formatMessage("{0}{0}{0}", "A"));
    }

    @Test
    void stringVazia() {
        assertEquals("", formatMessage(""));
    }

    @Test
    void placeholderComCodigoCor() {
        assertEquals("§aGorska §bé §ccool",
                formatMessage("§a{0} §b{1} §c{2}", "Gorska", "é", "cool"));
    }
}
