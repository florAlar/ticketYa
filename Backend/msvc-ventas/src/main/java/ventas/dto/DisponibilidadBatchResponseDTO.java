package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Respuesta batch de disponibilidad comercial. */
@Getter
@Setter
@Schema(name = "DisponibilidadBatch", description = "Disponibilidad comercial de varios eventos (máx. 50)")
public class DisponibilidadBatchResponseDTO {

    @Schema(description = "Disponibilidad por evento")
    private List<DisponibilidadResponseDTO> items = new ArrayList<>();
}
