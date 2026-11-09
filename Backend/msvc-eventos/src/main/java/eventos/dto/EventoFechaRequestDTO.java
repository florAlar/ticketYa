package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "EventoFechaRequest", description = "Reprogramación de un evento")
public class EventoFechaRequestDTO {

    @Schema(description = "Nueva fecha de inicio (ISO-8601)", example = "2026-04-10T21:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Nueva fecha de fin (ISO-8601)", example = "2026-04-11T02:00:00")
    private LocalDateTime fechaFin;
}
