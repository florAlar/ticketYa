package eventos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipanteResponseDTO {
    private String id;
    private String nombre;
    private String tipo;
    private String descripcion;
    /** Solo en contexto de evento: PRINCIPAL | TELONERO | … */
    private String rol;
    /** Solo en contexto de evento. */
    private Integer orden;
}
