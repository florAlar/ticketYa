package ventas.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Errores de la fila virtual (E2-FILA-01). Un único tipo con código de negocio y status HTTP
 * para no dispersar ocho clases de excepción; {@link GlobalExceptionHandler} serializa el
 * {@code code} y los {@code extras} en el cuerpo de la respuesta.
 */
public class FilaException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> extras;

    public FilaException(String code, HttpStatus status, String message) {
        this(code, status, message, Map.of(), null);
    }

    public FilaException(String code, HttpStatus status, String message, Throwable cause) {
        this(code, status, message, Map.of(), cause);
    }

    private FilaException(String code, HttpStatus status, String message,
            Map<String, Object> extras, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.extras = new LinkedHashMap<>(extras);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getExtras() {
        return Map.copyOf(extras);
    }

    public static FilaException noDisponible() {
        return noDisponible(null);
    }

    public static FilaException noDisponible(Throwable cause) {
        return new FilaException("FILA_NO_DISPONIBLE", HttpStatus.SERVICE_UNAVAILABLE,
                "La fila virtual no está disponible", cause);
    }

    public static FilaException eventoNoElegible() {
        return new FilaException("FILA_EVENTO_NO_ELEGIBLE", HttpStatus.CONFLICT,
                "El evento no está PUBLICADO o no es de alta demanda");
    }

    public static FilaException noIngresado() {
        return new FilaException("FILA_NO_INGRESADO", HttpStatus.NOT_FOUND,
                "El cliente no ingresó a la fila de este evento");
    }

    public static FilaException turnoUsado() {
        return new FilaException("FILA_TURNO_USADO", HttpStatus.CONFLICT,
                "El cliente ya consumió su turno en este evento");
    }

    public static FilaException tandaNoReducible() {
        return new FilaException("FILA_TANDA_NO_REDUCIBLE", HttpStatus.CONFLICT,
                "La tanda no puede retroceder");
    }

    public static FilaException fueraDeTanda(long posicion, long tanda) {
        return new FilaException("FILA_FUERA_DE_TANDA", HttpStatus.CONFLICT,
                "Aún no es tu turno", Map.of(
                        "posicion", posicion,
                        "posicionHabilitadaHasta", tanda,
                        "delante", Math.max(0, posicion - tanda)), null);
    }

    public static FilaException tokenRequerido() {
        return new FilaException("FILA_TOKEN_REQUERIDO", HttpStatus.CONFLICT,
                "Este evento requiere token de la fila para reservar");
    }

    public static FilaException tokenInvalido() {
        return new FilaException("FILA_TOKEN_INVALIDO", HttpStatus.CONFLICT,
                "El token de la fila es inválido, vencido o ya fue usado");
    }
}
