package ventas.dto;

/**
 * Clasificación comercial de cupo libre (número real → nivel).
 * Umbrales: 0 → AGOTADO; &lt;20 → ULTIMOS_LUGARES; &lt;50 → MENOS_DE_50; &lt;100 → MENOS_DE_100; else OK.
 */
public final class NivelCupo {

    public static final String AGOTADO = "AGOTADO";
    public static final String ULTIMOS_LUGARES = "ULTIMOS_LUGARES";
    public static final String MENOS_DE_50 = "MENOS_DE_50";
    public static final String MENOS_DE_100 = "MENOS_DE_100";
    public static final String OK = "OK";

    public static final int UMBRAL_ULTIMOS = 20;
    public static final int UMBRAL_50 = 50;
    public static final int UMBRAL_100 = 100;

    private NivelCupo() {}

    public static String fromDisponible(int disponibleTotal) {
        if (disponibleTotal < 1) {
            return AGOTADO;
        }
        if (disponibleTotal < UMBRAL_ULTIMOS) {
            return ULTIMOS_LUGARES;
        }
        if (disponibleTotal < UMBRAL_50) {
            return MENOS_DE_50;
        }
        if (disponibleTotal < UMBRAL_100) {
            return MENOS_DE_100;
        }
        return OK;
    }

    public static String label(String nivel) {
        if (nivel == null) {
            return null;
        }
        return switch (nivel) {
            case AGOTADO -> "Agotado";
            case ULTIMOS_LUGARES -> "Últimos lugares";
            case MENOS_DE_50 -> "Menos de 50";
            case MENOS_DE_100 -> "Menos de 100";
            case OK -> null;
            default -> null;
        };
    }

    public static boolean isAgotado(String nivel) {
        return AGOTADO.equals(nivel);
    }
}
