package eventos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Detalle de evento (GET /eventos/{id}).
 * Shape cercano al front toEventoDetalleDto: lugar completo + sectores con precio.
 */
@Getter
@Setter
public class EventoDetalleResponseDTO {

    private String id;
    private String nombre;
    private String descripcion;
    private String tipo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime fechaPublicacion;
    private LugarDetalleDTO lugar;
    private String imagenUrl;
    private BigDecimal precioDesde;
    private boolean destacado;
    private String estado;
    private String idOrganizador;
    private boolean esAltaDemanda;
    private List<SectorEventoDTO> sectores = new ArrayList<>();
}
