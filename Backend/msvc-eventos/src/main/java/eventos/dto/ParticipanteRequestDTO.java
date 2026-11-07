package eventos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipanteRequestDTO {
    private String nombre;
    /** ARTISTA | BANDA | ELENCO | … */
    private String tipo;
    private String descripcion;
}
