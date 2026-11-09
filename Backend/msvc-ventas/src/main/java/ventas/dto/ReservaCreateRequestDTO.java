package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(name = "ReservaCreateRequest", description = "Reserva temporal de cupos por sector")
public record ReservaCreateRequestDTO(
        @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
        @NotBlank String idEvento,
        @Schema(description = "Sectores y cantidades a reservar")
        @NotEmpty List<@Valid Item> items) {

    @Schema(name = "ReservaItem", description = "Cantidad a reservar en un sector")
    public record Item(
            @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000011")
            @NotBlank String idSector,
            @Schema(description = "Cantidad de entradas (mín. 1; máx. por cliente aplica en servidor)",
                    example = "2")
            @NotNull @Min(1) Integer cantidad) {
    }
}
