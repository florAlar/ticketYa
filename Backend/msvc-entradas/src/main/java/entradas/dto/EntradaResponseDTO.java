package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Shape cercano al FE domain/entrada (toEntradaListItem). */
@Getter
@Setter
@Schema(name = "Entrada", description = "Entrada emitida con código QR y datos del evento")
public class EntradaResponseDTO {

    @Schema(description = "UUID de la entrada", example = "66666666-6666-4666-8666-666666666666")
    private String id;

    /** Null si la política de QR no permite exhibirlo (evento cancelado, entrada usada, …). */
    @Schema(description = "Código QR (null si no exhibible)", example = "TKT-8f3a1c-0001")
    private String codigoQR;

    @Schema(description = "UUID de la compra", example = "33333333-3333-4333-8333-333333333333")
    private String idCompra;

    @Schema(description = "UUID del cliente", example = "b0000000-0000-4000-8000-000000000001")
    private String idCliente;

    @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
    private String idEvento;

    @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000011")
    private String idSector;

    @Schema(description = "Estado: VENDIDA | USADA | DEVUELTA | A_REINTEGRAR",
            allowableValues = {"VENDIDA", "USADA", "DEVUELTA", "A_REINTEGRAR"},
            example = "VENDIDA")
    private String estado;

    @Schema(description = "Fecha de emisión", example = "2026-03-01T12:10:00")
    private LocalDateTime fechaEmision;

    @Schema(description = "Fecha de ingreso validada (null si no ingresó)", example = "null")
    private LocalDateTime fechaIngreso;

    /** Opcional: no está en schema entrada; se rellena en emisión si viene. */
    @Schema(description = "Precio unitario pagado", example = "95000.00")
    private BigDecimal precio;

    @Schema(description = "Nombre del evento", example = "Metallica — M72 World Tour")
    private String eventoNombre;

    /** Estado del evento (Feign); p.ej. CANCELADO → sin QR. */
    @Schema(description = "Estado del evento (p.ej. PUBLICADO, CANCELADO)", example = "PUBLICADO")
    private String eventoEstado;

    @Schema(description = "Nombre del sector", example = "Campo")
    private String sectorNombre;

    @Schema(description = "Nombre del lugar", example = "Estadio Obras")
    private String lugarNombre;

    @Schema(description = "Ciudad del lugar", example = "Buenos Aires")
    private String lugarCiudad;
}
