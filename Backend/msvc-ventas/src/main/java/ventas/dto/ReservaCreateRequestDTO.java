package ventas.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(name = "ReservaCreateRequest", description = "Reserva temporal de cupos por sector")
public class ReservaCreateRequestDTO {

    @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
    @NotBlank
    private final String idEvento;

    @Schema(description = "Sectores y cantidades a reservar")
    @NotEmpty
    private final List<@Valid Item> items;

    @Schema(description = "Token de la fila (FIL-03). Obligatorio en eventos de alta demanda; "
            + "se ignora en eventos normales.", nullable = true)
    private final String tokenFila;

    /** Constructor de 2 argumentos: mantiene las llamadas existentes sin token de fila. */
    public ReservaCreateRequestDTO(String idEvento, List<Item> items) {
        this(idEvento, items, null);
    }

    @JsonCreator
    public ReservaCreateRequestDTO(
            @JsonProperty("idEvento") String idEvento,
            @JsonProperty("items") List<Item> items,
            @JsonProperty("tokenFila") String tokenFila) {
        this.idEvento = idEvento;
        this.items = items;
        this.tokenFila = tokenFila;
    }

    public String idEvento() {
        return idEvento;
    }

    public List<Item> items() {
        return items;
    }

    public String tokenFila() {
        return tokenFila;
    }

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
