package ventas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Dependencia externa caída al armar una respuesta (p. ej. Feign a eventos o entradas desde los
 * reportes). {@link GlobalExceptionHandler} la serializa con el mismo cuerpo de error que el
 * resto del microservicio, a diferencia de {@code ResponseStatusException}.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
