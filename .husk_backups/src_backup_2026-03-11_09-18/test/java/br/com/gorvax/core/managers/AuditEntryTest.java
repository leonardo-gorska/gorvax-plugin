package br.com.gorvax.core.managers;

import br.com.gorvax.core.managers.AuditManager.AuditAction;
import br.com.gorvax.core.managers.AuditManager.AuditEntry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para AuditManager.AuditEntry (inner class).
 * Lógica pura — sem mocks necessários.
 */
class AuditEntryTest {

    private AuditEntry criarEntry(AuditAction action) {
        return new AuditEntry(
                System.currentTimeMillis(), action,
                UUID.randomUUID(), "TestPlayer", "Detalhes do teste", 100.0
        );
    }

    // --- getActionIcon —- retorna ícone não-nulo para todas as ações

    @Test
    void todosIconesNaoNulos() {
        for (AuditAction action : AuditAction.values()) {
            AuditEntry entry = criarEntry(action);
            assertNotNull(entry.getActionIcon(), "Ícone nulo para: " + action);
            assertFalse(entry.getActionIcon().isEmpty(), "Ícone vazio para: " + action);
        }
    }

    // --- getActionName --- retorna nome PT-BR para todas as ações

    @Test
    void todosNomesNaoNulos() {
        for (AuditAction action : AuditAction.values()) {
            AuditEntry entry = criarEntry(action);
            assertNotNull(entry.getActionName(), "Nome nulo para: " + action);
            assertFalse(entry.getActionName().isEmpty(), "Nome vazio para: " + action);
        }
    }

    // --- isMarketAction ---

    @Test
    void isMarketActionCorreto() {
        assertTrue(criarEntry(AuditAction.MARKET_BUY).isMarketAction());
        assertTrue(criarEntry(AuditAction.MARKET_SELL).isMarketAction());
        assertTrue(criarEntry(AuditAction.MARKET_CANCEL).isMarketAction());

        assertFalse(criarEntry(AuditAction.CLAIM_CREATE).isMarketAction());
        assertFalse(criarEntry(AuditAction.KINGDOM_CREATE).isMarketAction());
        assertFalse(criarEntry(AuditAction.TRUST_ADD).isMarketAction());
    }

    // --- getFormattedDate ---

    @Test
    void formattedDateFormatoCorreto() {
        AuditEntry entry = criarEntry(AuditAction.CLAIM_CREATE);
        String date = entry.getFormattedDate();
        // Formato: dd/MM HH:mm
        assertNotNull(date);
        assertTrue(date.matches("\\d{2}/\\d{2} \\d{2}:\\d{2}"),
                "Formato esperado dd/MM HH:mm, got: " + date);
    }
}
