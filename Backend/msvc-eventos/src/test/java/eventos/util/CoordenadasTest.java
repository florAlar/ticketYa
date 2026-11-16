package eventos.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** LUG-02: validación de latitud/longitud (opcionales, juntas y dentro de rango). */
class CoordenadasTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void sinCoordenadas_esValido() {
        assertDoesNotThrow(() -> Coordenadas.validar(null, null));
    }

    @Test
    void parValidoYExtremos_sonValidos() {
        assertDoesNotThrow(() -> Coordenadas.validar(bd("-34.5453"), bd("-58.4497")));
        assertDoesNotThrow(() -> Coordenadas.validar(bd("90"), bd("180")));
        assertDoesNotThrow(() -> Coordenadas.validar(bd("-90"), bd("-180")));
        assertDoesNotThrow(() -> Coordenadas.validar(bd("0"), bd("0")));
    }

    @Test
    void unaSolaDeLasDos_esInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Coordenadas.validar(bd("10"), null));
        assertThrows(IllegalArgumentException.class, () -> Coordenadas.validar(null, bd("10")));
    }

    @Test
    void fueraDeRango_esInvalido() {
        IllegalArgumentException lat = assertThrows(IllegalArgumentException.class,
                () -> Coordenadas.validar(bd("90.000001"), bd("0")));
        assertEquals("latitud fuera de rango (-90 a 90)", lat.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Coordenadas.validar(bd("-90.1"), bd("0")));
        IllegalArgumentException lng = assertThrows(IllegalArgumentException.class,
                () -> Coordenadas.validar(bd("0"), bd("180.000001")));
        assertEquals("longitud fuera de rango (-180 a 180)", lng.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Coordenadas.validar(bd("0"), bd("-181")));
    }

    @Test
    void normalizar_dejaSeisDecimales() {
        assertEquals(0, bd("-34.545300").compareTo(Coordenadas.normalizar(bd("-34.5453"))));
        assertEquals(6, Coordenadas.normalizar(bd("-34.5453")).scale());
        assertEquals(bd("-58.449700"), Coordenadas.normalizar(bd("-58.44969999")));
        assertNull(Coordenadas.normalizar(null));
    }
}
