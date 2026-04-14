package br.com.gorvax.core.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para SelectionManager.
 * Testa Selection inner class (area, width, length, isComplete).
 */
class SelectionManagerTest {

    // --- Selection.isComplete ---

    @Test
    void selectionNaoCompleta() {
        var sel = new SelectionManager.Selection();
        assertFalse(sel.isComplete());
    }

    @Test
    void selectionComPonto1() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(0, 64, 0);
        assertFalse(sel.isComplete());
    }

    @Test
    void selectionCompleta() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(0, 64, 0);
        sel.point2 = mockLocation(10, 64, 10);
        assertTrue(sel.isComplete());
    }

    // --- Selection.getArea ---

    @Test
    void selectionAreaComplexa() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(0, 64, 0);
        sel.point2 = mockLocation(9, 64, 9);

        int area = sel.getArea();
        // width = |9-0|+1 = 10, length = |9-0|+1 = 10 → area = 100
        assertEquals(100, area);
    }

    @Test
    void selectionAreaUnicoBloco() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(5, 64, 5);
        sel.point2 = mockLocation(5, 64, 5);

        assertEquals(1, sel.getArea());
    }

    @Test
    void selectionAreaNegativeCoords() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(-5, 64, -5);
        sel.point2 = mockLocation(4, 64, 4);

        // width = |4-(-5)|+1 = 10, length = |4-(-5)|+1 = 10 → 100
        assertEquals(100, sel.getArea());
    }

    // --- Selection.getWidth ---

    @Test
    void selectionWidth() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(0, 64, 0);
        sel.point2 = mockLocation(15, 64, 5);

        assertEquals(16, sel.getWidth()); // |15-0|+1 = 16
    }

    // --- Selection.getLength ---

    @Test
    void selectionLength() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(0, 64, 0);
        sel.point2 = mockLocation(15, 64, 5);

        assertEquals(6, sel.getLength()); // |5-0|+1 = 6
    }

    // --- Selection.isResize defaults ---

    @Test
    void selectionDefaults() {
        var sel = new SelectionManager.Selection();
        assertFalse(sel.isResize);
        assertNull(sel.resizeClaimId);
        assertEquals(0, sel.originalArea);
        assertNotNull(sel.activeGhostBlocks);
        assertTrue(sel.activeGhostBlocks.isEmpty());
    }

    // --- Selection com resize ---

    @Test
    void selectionResizeMode() {
        var sel = new SelectionManager.Selection();
        sel.isResize = true;
        sel.resizeClaimId = "claim-123";
        sel.originalArea = 500;

        assertTrue(sel.isResize);
        assertEquals("claim-123", sel.resizeClaimId);
        assertEquals(500, sel.originalArea);
    }

    // --- Selection area com pontos invertidos ---

    @Test
    void selectionAreaPontosInvertidos() {
        var sel = new SelectionManager.Selection();
        sel.point1 = mockLocation(10, 64, 10);
        sel.point2 = mockLocation(0, 64, 0);

        // Deve funcionar independente da ordem dos pontos
        // width = |0-10|+1 = 11, length = |0-10|+1 = 11 → 121
        assertEquals(121, sel.getArea());
    }

    // --- Helper ---

    private Location mockLocation(int x, int y, int z) {
        Location loc = mock(Location.class);
        when(loc.getBlockX()).thenReturn(x);
        when(loc.getBlockY()).thenReturn(y);
        when(loc.getBlockZ()).thenReturn(z);
        return loc;
    }
}
