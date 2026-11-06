package entradas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** Emisión post-compra (llamado por msvc-ventas o FE). */
public record EmitirEntradasRequestDTO(
        @NotBlank String idCompra,
        @NotBlank String idCliente,
        @NotBlank String idEvento,
        @NotEmpty List<@Valid Item> items) {

    public record Item(
            @NotBlank String idSector,
            @NotNull @Min(1) Integer cantidad,
            BigDecimal precioUnitario) {
    }
}
