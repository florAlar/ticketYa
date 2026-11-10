package eventos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SectorLayoutTest {

    @Test
    void rotuloFila_letrasSimples() {
        assertEquals("A", SectorLayout.rotuloFila(1));
        assertEquals("B", SectorLayout.rotuloFila(2));
        assertEquals("Z", SectorLayout.rotuloFila(26));
    }

    @Test
    void rotuloFila_pasaDeZ_comoPlanilla() {
        assertEquals("AA", SectorLayout.rotuloFila(27));
        assertEquals("AB", SectorLayout.rotuloFila(28));
        assertEquals("AZ", SectorLayout.rotuloFila(52));
        assertEquals("BA", SectorLayout.rotuloFila(53));
        assertEquals("ZZ", SectorLayout.rotuloFila(702));
        assertEquals("AAA", SectorLayout.rotuloFila(703));
    }

    @Test
    void rotuloFila_indiceInvalido_throws() {
        assertThrows(IllegalArgumentException.class, () -> SectorLayout.rotuloFila(0));
    }

    @Test
    void capacidad_esFilasPorAsientos() {
        assertEquals(300, SectorLayout.capacidad(20, 15));
    }

    @Test
    void validar_ok_enLosLimites() {
        SectorLayout.validar(1, 1, "X");
        SectorLayout.validar(SectorLayout.MAX_FILAS, SectorLayout.MAX_ASIENTOS_POR_FILA, "X");
    }

    @Test
    void validar_nulosOFueraDeRango_throws() {
        assertThrows(IllegalArgumentException.class, () -> SectorLayout.validar(null, 10, "X"));
        assertThrows(IllegalArgumentException.class, () -> SectorLayout.validar(10, null, "X"));
        assertThrows(IllegalArgumentException.class, () -> SectorLayout.validar(0, 10, "X"));
        assertThrows(IllegalArgumentException.class, () -> SectorLayout.validar(10, 0, "X"));
        assertThrows(IllegalArgumentException.class,
                () -> SectorLayout.validar(SectorLayout.MAX_FILAS + 1, 10, "X"));
        assertThrows(IllegalArgumentException.class,
                () -> SectorLayout.validar(10, SectorLayout.MAX_ASIENTOS_POR_FILA + 1, "X"));
    }

    @Test
    void generar_ordenadoPorFilaYNumero() {
        var asientos = SectorLayout.generar("sec-1", 2, 3);

        assertEquals(6, asientos.size());
        assertEquals("A", asientos.get(0).getFila());
        assertEquals(1, asientos.get(0).getNumero());
        assertEquals("A-3", asientos.get(2).getCodigo());
        assertEquals("B", asientos.get(3).getFila());
        assertEquals("B-3", asientos.get(5).getCodigo());
        assertEquals("sec-1", asientos.get(5).getIdSector());
        assertNull(asientos.get(5).getId());
    }
}
