package ventas.exception;

import org.springframework.http.HttpStatus;

/**
 * Errores de la fila virtual (E2-FILA-01). Un único tipo con código de negocio y/status HTTP
 * para no dispersar ocho clases de excepción; {@link GlobalExceptionHandler} serializa el
 * {@code code} en el cuerpo de la respuesta.
 */
public class FilaException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public FilaException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public FilaException(String code, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
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
}
