package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Asiento", description = "Asiento tomado de un sector numerado")
public class AsientoDTO {

    @Schema(description = "Fila", example = "C")
    private String fila;

    @Schema(description = "Número dentro de la fila", example = "14")
    private Integer numero;

    @Schema(description = "RESERVADO (con TTL) o VENDIDO", allowableValues = {"RESERVADO", "VENDIDO"},
            example = "RESERVADO")
    private String estado;

    public AsientoDTO() {
    }

    public AsientoDTO(String fila, Integer numero, String estado) {
        this.fila = fila;
        this.numero = numero;
        this.estado = estado;
    }
}
