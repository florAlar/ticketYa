package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pago mock. {@code resultado}: APROBAR (default) | RECHAZAR.
 * Opcional {@code proveedor} (default MOCK_PASARELA).
 */
@Schema(name = "PagoCreateRequest", description = "Datos del pago mock; el body completo es opcional")
public record PagoCreateRequestDTO(
        @Schema(description = "Resultado simulado: APROBAR (default) | RECHAZAR", example = "APROBAR")
        String resultado,
        @Schema(description = "Proveedor de pago (default MOCK_PASARELA)", example = "MOCK_PASARELA")
        String proveedor) {
}
