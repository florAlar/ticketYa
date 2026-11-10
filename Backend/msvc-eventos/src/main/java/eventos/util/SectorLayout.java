package eventos.util;

import eventos.dto.AsientoResponseDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * Layout de un sector numerado: {@code filas x asientosPorFila}.
 *
 * <p>Los asientos son virtuales: no se persisten filas vacías. Un asiento se identifica por
 * (sector, fila, número), con fila {@code A..Z, AA..AZ, BA...} y número {@code 1..asientosPorFila}.
 * La misma convención de rótulos se usa en msvc-ventas para validar las reservas.
 */
public final class SectorLayout {

    public static final int MAX_FILAS = 200;
    public static final int MAX_ASIENTOS_POR_FILA = 500;

    private SectorLayout() {
    }

    /** Valida que filas y asientosPorFila estén presentes y dentro de rango. */
    public static void validar(Integer filas, Integer asientosPorFila, String nombreSector) {
        if (filas == null || asientosPorFila == null
                || filas < 1 || filas > MAX_FILAS
                || asientosPorFila < 1 || asientosPorFila > MAX_ASIENTOS_POR_FILA) {
            throw new IllegalArgumentException(
                    "Sector numerado «" + nombreSector + "» requiere filas (1.." + MAX_FILAS
                            + ") y asientosPorFila (1.." + MAX_ASIENTOS_POR_FILA + ")");
        }
    }

    /** Capacidad total del layout. */
    public static int capacidad(int filas, int asientosPorFila) {
        return filas * asientosPorFila;
    }

    /** Rótulo de fila (base 1): 1 → A, 26 → Z, 27 → AA, 28 → AB, ... */
    public static String rotuloFila(int indice) {
        if (indice < 1) {
            throw new IllegalArgumentException("indice de fila debe ser >= 1");
        }
        StringBuilder sb = new StringBuilder();
        int n = indice;
        while (n > 0) {
            n--;
            sb.append((char) ('A' + (n % 26)));
            n /= 26;
        }
        return sb.reverse().toString();
    }

    /** Asientos virtuales del sector, ordenados por fila y número. */
    public static List<AsientoResponseDTO> generar(String idSector, int filas, int asientosPorFila) {
        List<AsientoResponseDTO> out = new ArrayList<>(capacidad(filas, asientosPorFila));
        for (int f = 1; f <= filas; f++) {
            String fila = rotuloFila(f);
            for (int n = 1; n <= asientosPorFila; n++) {
                AsientoResponseDTO dto = new AsientoResponseDTO();
                dto.setIdSector(idSector);
                dto.setFila(fila);
                dto.setNumero(n);
                dto.setCodigo(fila + "-" + n);
                out.add(dto);
            }
        }
        return out;
    }
}
