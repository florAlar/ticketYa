package entradas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Dependencia externa caída o emisión en curso: la operación puede reintentarse. Se responde 503
 * con el cuerpo de error estándar de {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
