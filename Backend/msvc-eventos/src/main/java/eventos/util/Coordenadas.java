package eventos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Coordenadas geográficas de un lugar (LUG-02): latitud y longitud en grados decimales, opcionales.
 * Si hay una tiene que haber la otra y ambas dentro de rango. Se guardan con 6 decimales (~0,1 m).
 */
public final class Coordenadas {

    public static final BigDecimal LAT_MIN = new BigDecimal("-90");
    public static final BigDecimal LAT_MAX = new BigDecimal("90");
    public static final BigDecimal LNG_MIN = new BigDecimal("-180");
    public static final BigDecimal LNG_MAX = new BigDecimal("180");
    public static final int ESCALA = 6;

    private Coordenadas() {}

    /** Lanza {@link IllegalArgumentException} si el par es inconsistente o está fuera de rango. */
    public static void validar(BigDecimal latitud, BigDecimal longitud) {
        if (latitud == null && longitud == null) {
            return;
        }
        if (latitud == null || longitud == null) {
            throw new IllegalArgumentException("latitud y longitud deben venir juntas");
        }
        if (latitud.compareTo(LAT_MIN) < 0 || latitud.compareTo(LAT_MAX) > 0) {
            throw new IllegalArgumentException("latitud fuera de rango (-90 a 90)");
        }
        if (longitud.compareTo(LNG_MIN) < 0 || longitud.compareTo(LNG_MAX) > 0) {
            throw new IllegalArgumentException("longitud fuera de rango (-180 a 180)");
        }
    }

    /** Redondea al máximo de decimales que guarda la base (columna DECIMAL(9,6)). */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
