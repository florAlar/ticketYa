package eventos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Catálogo / listado — alineado al shape liviano del front (toCatalogoDto).
 * imagenUrl y destacado no existen en schema MySQL actual → null / false.
 */
@Getter
@Setter
public class EventoResponseDTO {

    private String id;
    private String nombre;
    private String tipo;
    private LocalDateTime fechaInicio;
    private LugarResumenDTO lugar;
    private String imagenUrl;
    private BigDecimal precioDesde;
    private boolean destacado;
    private String estado;
    private String idOrganizador;
    private boolean esAltaDemanda;
}
