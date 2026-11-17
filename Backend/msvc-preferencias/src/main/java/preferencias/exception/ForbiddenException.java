package preferencias.exception;

/** El usuario autenticado no puede acceder al recurso (403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
