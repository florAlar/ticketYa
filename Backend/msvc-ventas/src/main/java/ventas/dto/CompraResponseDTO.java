package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Compra", description = "Compra creada a partir de una reserva")
public class CompraResponseDTO {

    @Schema(description = "UUID de la compra", example = "33333333-3333-4333-8333-333333333333")
    private String id;

    @Schema(description = "UUID de la reserva origen", example = "11111111-1111-4111-8111-111111111111")
    private String idReserva;

    @Schema(description = "UUID del cliente", example = "b0000000-0000-4000-8000-000000000001")
    private String idCliente;

    @Schema(description = "Fecha de la compra", example = "2026-03-01T12:05:00")
    private LocalDateTime fechaCompra;

    @Schema(description = "Importe total", example = "190000.00")
    private BigDecimal total;

    @Schema(description = "Moneda ISO-4217", example = "ARS")
    private String moneda;

    @Schema(description = "Estado: PENDIENTE_PAGO | APROBADA | RECHAZADA", example = "PENDIENTE_PAGO")
    private String estado;

    @Schema(description = "Estado de emisión de QR en msvc-entradas: NO_REQUIERE | PENDIENTE | EMITIDA | FALLIDA",
            example = "PENDIENTE")
    private String emisionEstado;

    @Schema(description = "Intentos de emisión de entradas", example = "0")
    private Integer emisionIntentos;
}
