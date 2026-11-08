package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Pago", description = "Pago registrado para una compra")
public class PagoResponseDTO {

    @Schema(description = "UUID del pago", example = "44444444-4444-4444-8444-444444444444")
    private String id;

    @Schema(description = "UUID de la compra", example = "33333333-3333-4333-8333-333333333333")
    private String idCompra;

    @Schema(description = "Proveedor de pago", example = "MOCK_PASARELA")
    private String proveedor;

    @Schema(description = "Identificador de la transacción en el proveedor", example = "MOCK-8f3a1c")
    private String idTransaccionExterna;

    @Schema(description = "Importe pagado", example = "190000.00")
    private BigDecimal monto;

    @Schema(description = "Estado: APROBADO | RECHAZADO", example = "APROBADO")
    private String estado;

    @Schema(description = "Fecha del pago", example = "2026-03-01T12:06:00")
    private LocalDateTime fechaPago;
}
