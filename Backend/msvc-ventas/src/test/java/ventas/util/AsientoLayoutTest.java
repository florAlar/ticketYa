package ventas.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AsientoLayoutTest {

    @Test
    void indiceFila_rotulosValidos() {
        assertEquals(1, AsientoLayout.indiceFila("A"));
        assertEquals(26, AsientoLayout.indiceFila("Z"));
        assertEquals(27, AsientoLayout.indiceFila("AA"));
        assertEquals(52, AsientoLayout.indiceFila("AZ"));
        assertEquals(53, AsientoLayout.indiceFila("BA"));
        assertEquals(702, AsientoLayout.indiceFila("ZZ"));
        assertEquals(703, AsientoLayout.indiceFila("AAA"));
    }

    @Test
    void indiceFila_rotulosInvalidos() {
        assertEquals(-1, AsientoLayout.indiceFila(null));
        assertEquals(-1, AsientoLayout.indiceFila(""));
        assertEquals(-1, AsientoLayout.indiceFila("a")); // se espera ya normalizado
        assertEquals(-1, AsientoLayout.indiceFila("A1"));
        assertEquals(-1, AsientoLayout.indiceFila("AAAA"));
    }

    @Test
    void normalizarFila_trimYMayuscula() {
        assertEquals("AB", AsientoLayout.normalizarFila("  ab "));
        assertNull(AsientoLayout.normalizarFila("   "));
        assertNull(AsientoLayout.normalizarFila(null));
    }

    @Test
    void existe_respetaFilasYAsientosPorFila() {
        assertTrue(AsientoLayout.existe("A", 1, 10, 20));
        assertTrue(AsientoLayout.existe("J", 20, 10, 20));
        assertFalse(AsientoLayout.existe("K", 1, 10, 20));
        assertFalse(AsientoLayout.existe("A", 21, 10, 20));
        assertFalse(AsientoLayout.existe("A", 0, 10, 20));
        assertFalse(AsientoLayout.existe("A", null, 10, 20));
        assertFalse(AsientoLayout.existe(null, 1, 10, 20));
        assertTrue(AsientoLayout.existe("AA", 1, 30, 5));
    }

    @Test
    void codigo_filaGuionNumero() {
        assertEquals("C-14", AsientoLayout.codigo("C", 14));
    }
}
