package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(name = "ReservaCreateRequest", description = "Reserva temporal de cupos por sector")
public record ReservaCreateRequestDTO(
        @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
        @NotBlank String idEvento,
        @Schema(description = "Sectores y cantidades a reservar")
        @NotEmpty List<@Valid Item> items) {

    @Schema(name = "ReservaItem",
            description = "Sector a reservar: sector general = cantidad; sector numerado = asientos concretos")
    public record Item(
            @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000011")
            @NotBlank String idSector,
            @Schema(description = "Cantidad de entradas. Obligatoria en sectores generales (mín. 1; máx. por "
                    + "cliente aplica en servidor). En sectores numerados se ignora (vale la cantidad de asientos); "
                    + "si se envía debe coincidir.", example = "2")
            @Min(1) Integer cantidad,
            @Schema(description = "Asientos pedidos (fila + número). Obligatorio en sectores numerados y no "
                    + "admitido en sectores generales.", nullable = true)
            List<@Valid AsientoRefDTO> asientos) {

        /** Sector general: solo cantidad. */
        public Item(String idSector, Integer cantidad) {
            this(idSector, cantidad, null);
        }
    }
}
