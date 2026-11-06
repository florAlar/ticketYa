package usuarios.exception;

import java.time.Instant;

public record ApiError(String timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().toString(), status, error, message, path);
    }
}
