package ventas.util;

/**
 * Validación de asientos virtuales contra el layout del sector (filas x asientos por fila).
 * Misma convención de rótulos que msvc-eventos: A..Z, AA..AZ, BA... (base 26 tipo planilla).
 */
public final class AsientoLayout {

    private AsientoLayout() {
    }

    /** Fila en mayúsculas y sin espacios; null si viene vacía. */
    public static String normalizarFila(String fila) {
        if (fila == null) {
            return null;
        }
        String f = fila.trim().toUpperCase();
        return f.isEmpty() ? null : f;
    }

    /** Índice base 1 del rótulo (A=1, Z=26, AA=27); -1 si no es un rótulo válido. */
    public static int indiceFila(String fila) {
        if (fila == null || fila.isEmpty() || fila.length() > 3) {
            return -1;
        }
        int idx = 0;
        for (int i = 0; i < fila.length(); i++) {
            char c = fila.charAt(i);
            if (c < 'A' || c > 'Z') {
                return -1;
            }
            idx = idx * 26 + (c - 'A' + 1);
        }
        return idx;
    }

    /** true si (fila, numero) existe en un layout de {@code filas x asientosPorFila}. */
    public static boolean existe(String fila, Integer numero, int filas, int asientosPorFila) {
        int idx = indiceFila(fila);
        return idx >= 1 && idx <= filas && numero != null && numero >= 1 && numero <= asientosPorFila;
    }

    public static String codigo(String fila, Integer numero) {
        return fila + "-" + numero;
    }
}
