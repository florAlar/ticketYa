package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Asiento",
        description = "Asiento virtual de un sector numerado, calculado desde filas y asientosPorFila (no se persiste)")
public class AsientoResponseDTO {

    @Schema(description = "Siempre null: los asientos son virtuales; se identifican por sector + fila + número",
            nullable = true)
    private String id;

    @Schema(description = "UUID del sector", example = "a0000000-0000-4000-8000-000000000001")
    private String idSector;

    @Schema(description = "Fila", example = "A")
    private String fila;

    @Schema(description = "Número", example = "12")
    private Integer numero;

    @Schema(description = "Código legible", example = "A-12")
    private String codigo;
}
