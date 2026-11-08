package entradas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** Emisión post-compra (llamado por msvc-ventas o FE). */
@Schema(name = "EmitirEntradasRequest", description = "Entradas a emitir por compra")
public record EmitirEntradasRequestDTO(

        @Schema(description = "UUID de la compra origen", example = "33333333-3333-4333-8333-333333333333")
        @NotBlank String idCompra,

        @Schema(description = "UUID del cliente", example = "b0000000-0000-4000-8000-000000000001")
        @NotBlank String idCliente,

        @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
        @NotBlank String idEvento,

        @Schema(description = "Líneas de entrada por sector")
        @NotEmpty List<@Valid Item> items) {

    @Schema(name = "EmitirEntradaItem", description = "Entradas a emitir en un sector")
    public record Item(
            @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000011")
            @NotBlank String idSector,

            @Schema(description = "Cantidad de entradas (mín. 1)", example = "2")
            @NotNull @Min(1) Integer cantidad,

            @Schema(description = "Precio unitario pagado", example = "95000.00")
            BigDecimal precioUnitario) {
    }
}
