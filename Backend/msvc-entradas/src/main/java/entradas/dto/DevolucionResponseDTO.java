package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Devolución de una entrada (estado PENDIENTE | APROBADA | RECHAZADA | EN_CURSO). */
@Getter
@Setter
@Schema(name = "Devolucion", description = "Devolución de entrada con estado y monto reembolsado")
public class DevolucionResponseDTO {

    @Schema(description = "UUID de la devolución", example = "55555555-5555-4555-8555-555555555555")
    private String id;

    @Schema(description = "UUID de la entrada", example = "66666666-6666-4666-8666-666666666666")
    private String idEntrada;

    @Schema(description = "UUID del cliente", example = "b0000000-0000-4000-8000-000000000001")
    private String idCliente;

    @Schema(description = "Motivo de la devolución", example = "No puedo asistir al evento")
    private String motivo;

    @Schema(description = "Monto a reembolsar", example = "95000.00")
    private BigDecimal monto;

    @Schema(description = "Fecha de solicitud", example = "2026-03-01T12:30:00")
    private LocalDateTime fechaSolicitud;

    @Schema(description = "Estado: PENDIENTE | APROBADA | RECHAZADA | EN_CURSO",
            allowableValues = {"PENDIENTE", "APROBADA", "RECHAZADA", "EN_CURSO"},
            example = "PENDIENTE")
    private String estado;
}
