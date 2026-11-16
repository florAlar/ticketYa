package usuarios.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * E2-NFR-03: bitácora de operaciones críticas (compra, devolución, cancelación, validación de
 * acceso, login…). Es una línea de log con logger {@code bitacora}; el correlation id sale solo en el
 * patrón de log, así que se puede seguir la operación completa entre servicios:
 * {@code docker compose logs | grep <id>}.
 */
public final class Bitacora {

    private static final Logger LOG = LoggerFactory.getLogger("bitacora");

    private Bitacora() {}

    /**
     * @param accion  qué pasó, en MAYÚSCULAS (ej. PAGO_APROBADO)
     * @param usuario quién lo hizo; si es null se toma el X-User-Id del request (MDC) y, sin request, queda en '-'
     * @param recurso id principal afectado (compra, evento, entrada…)
     * @param detalle datos útiles sin información sensible (clave=valor)
     */
    public static void registrar(String accion, String usuario, String recurso, String detalle) {
        String quien = usuario != null && !usuario.isBlank() ? usuario : MDC.get(CorrelationIdFilter.MDC_USER);
        LOG.info("accion={} usuario={} recurso={} detalle={}", accion, nz(quien), nz(recurso), nz(detalle));
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "-" : s.replaceAll("[\\r\\n]", " ");
    }
}
