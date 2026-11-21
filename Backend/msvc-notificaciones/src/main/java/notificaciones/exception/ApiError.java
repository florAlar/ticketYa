package notificaciones.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Payload uniforme de error de la API de notificaciones. */
@Schema(name = "ApiError",
        description = "Error de la API de notificaciones (incluye errores de validación del body).")
public record ApiError(
        @Schema(description = "Momento del error", format = "date-time",
                example = "2026-11-21T12:00:00Z")
        String timestamp,
        @Schema(description = "Código HTTP", example = "404")
        int status,
        @Schema(description = "Motivo HTTP", example = "Not Found")
        String error,
        @Schema(description = "Detalle del error", nullable = true)
        String message,
        @Schema(description = "Path solicitado", nullable = true)
        String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().toString(), status, error, message, path);
    }
}
