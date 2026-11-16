package preferencias.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Request interno de msvc-notificaciones: artistas del evento + ciudad donde se publica. */
@Getter
@Setter
public class MatchingClientesRequestDTO {

    private String ciudad;

    private List<String> artistas;
}
