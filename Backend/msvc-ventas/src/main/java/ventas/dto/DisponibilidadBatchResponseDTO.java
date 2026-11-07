package ventas.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Respuesta batch de disponibilidad comercial. */
@Getter
@Setter
public class DisponibilidadBatchResponseDTO {

    private List<DisponibilidadResponseDTO> items = new ArrayList<>();
}
