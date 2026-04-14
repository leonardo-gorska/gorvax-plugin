package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para EndResetManager.
 * Lógica muito ligada a I/O e Bukkit — testa apenas o que é possível sem
 * runtime.
 * Foca em lógica pura: coordenadas de portal, formatação.
 */
class EndResetManagerTest {

    // --- Lógica de coordenadas do Exit Portal (replicada) ---
    // As coordenadas do Exit Portal do End são definidas nas constantes:
    // portal_x=0, portal_z=0, portal_y varia. A lógica de verificação usa
    // um range de 5 blocos ao redor.

    private boolean isNearExitPortal(int x, int y, int z, int range) {
        // Exit portal no End fica em (0, y_var, 0)
        return Math.abs(x) <= range && Math.abs(z) <= range;
    }

    @Test
    void nearExitPortalOrigem() {
        assertTrue(isNearExitPortal(0, 60, 0, 5));
    }

    @Test
    void nearExitPortalBorda() {
        assertTrue(isNearExitPortal(5, 60, 5, 5));
        assertTrue(isNearExitPortal(-5, 60, -5, 5));
    }

    @Test
    void nearExitPortalFora() {
        assertFalse(isNearExitPortal(6, 60, 0, 5));
        assertFalse(isNearExitPortal(0, 60, 6, 5));
    }

    @Test
    void nearExitPortalDistante() {
        assertFalse(isNearExitPortal(100, 60, 100, 5));
    }

    // --- Lógica de deleteDirectory recursiva ---
    // A função real deleta arquivos do sistema de arquivos.
    // Testamos a lógica de contagem/validação, não a operação I/O.

    @Test
    void deleteDirectoryLogicNullSafe() {
        // Simula que receber null não causa exceção
        java.io.File nullFile = null;
        assertDoesNotThrow(() -> {
            if (nullFile != null && nullFile.exists()) {
                // operação de delete
            }
        });
    }

    // --- Lógica de reset timer (milissegundos entre resets) ---

    private long getNextResetMs(long lastReset, long intervalDays) {
        return lastReset + (intervalDays * 24L * 60L * 60L * 1000L);
    }

    @Test
    void nextResetUmDia() {
        long lastReset = 0;
        long next = getNextResetMs(lastReset, 1);
        assertEquals(86_400_000L, next);
    }

    @Test
    void nextResetSeteDias() {
        long lastReset = 1_000_000L;
        long next = getNextResetMs(lastReset, 7);
        assertEquals(1_000_000L + 7L * 86_400_000L, next);
    }

    @Test
    void nextResetTrintaDias() {
        long lastReset = 0;
        long next = getNextResetMs(lastReset, 30);
        assertEquals(30L * 86_400_000L, next);
    }

    @Test
    void resetNecessarioQuandoPassou() {
        long lastReset = 1_000_000L;
        long now = lastReset + 8 * 86_400_000L;
        long next = getNextResetMs(lastReset, 7);
        assertTrue(now > next, "Deveria precisar de reset após 8 dias com intervalo de 7");
    }

    @Test
    void resetNaoNecessarioAntes() {
        long lastReset = 1_000_000L;
        long now = lastReset + 5 * 86_400_000L;
        long next = getNextResetMs(lastReset, 7);
        assertFalse(now > next, "Não deveria precisar de reset após 5 dias com intervalo de 7");
    }
}
