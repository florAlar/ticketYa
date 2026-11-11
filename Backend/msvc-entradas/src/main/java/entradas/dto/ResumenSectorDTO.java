package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "ResumenSector",
        description = "Entradas de un sector de un evento agrupadas por estado (consumo interno de msvc-ventas para reportes)")
public record ResumenSectorDTO(
        @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000012") String idSector,
        @Schema(description = "Entradas VENDIDA (vigentes, aún sin usar)", example = "40") long vendidas,
        @Schema(description = "Entradas USADA (ya ingresaron)", example = "12") long usadas,
        @Schema(description = "Entradas DEVUELTA (devolución aprobada)", example = "3") long devueltas,
        @Schema(description = "Entradas A_REINTEGRAR (evento cancelado, reintegro pendiente)", example = "0") long aReintegrar,
        @Schema(description = "Suma de precios de las entradas DEVUELTA", example = "540000.00") BigDecimal montoDevuelto,
        @Schema(description = "Suma de precios de las entradas A_REINTEGRAR", example = "0.00") BigDecimal montoAReintegrar) {}
