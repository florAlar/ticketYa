package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Reserva", description = "Reserva temporal con TTL y detalle por sector")
public class ReservaResponseDTO {

    @Schema(description = "UUID de la reserva", example = "11111111-1111-4111-8111-111111111111")
    private String id;

    @Schema(description = "UUID del cliente", example = "b0000000-0000-4000-8000-000000000001")
    private String idCliente;

    @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
    private String idEvento;

    @Schema(description = "Fecha de creación", example = "2026-03-01T12:00:00")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Expiración del TTL", example = "2026-03-01T12:10:00")
    private LocalDateTime fechaExpiracion;

    @Schema(description = "Estado: ACTIVA | CONFIRMADA | CANCELADA | EXPIRADA", example = "ACTIVA")
    private String estado;

    @Schema(description = "Total de la reserva", example = "190000.00")
    private BigDecimal total;

    @Schema(description = "Detalle por sector")
    private List<DetalleReservaDTO> detalles = new ArrayList<>();
}
