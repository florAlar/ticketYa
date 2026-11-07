package eventos.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoFechaRequestDTO {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
}
