package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "EventoDetalle", description = "Evento con lugar, sectores con precio y participantes")
public class EventoDetalleResponseDTO {

    @Schema(description = "UUID del evento", example = "e0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Nombre", example = "Festival TicketYa 2026")
    private String nombre;

    @Schema(description = "Descripción", example = "Una noche de rock y electrónica")
    private String descripcion;

    @Schema(description = "Tipo", example = "FESTIVAL")
    private String tipo;

    @Schema(description = "Inicio (ISO-8601)", example = "2026-03-14T20:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fin (ISO-8601)", example = "2026-03-15T02:00:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Fecha de publicación", example = "2026-01-20T10:00:00")
    private LocalDateTime fechaPublicacion;

    @Schema(description = "Lugar con sectores")
    private LugarDetalleDTO lugar;

    @Schema(description = "Imagen (null en E1)", example = "null")
    private String imagenUrl;

    @Schema(description = "Precio mínimo del evento", example = "25000.00")
    private BigDecimal precioDesde;

    @Schema(description = "Destacado (false en E1)", example = "false")
    private boolean destacado;

    @Schema(description = "Estado", example = "PUBLICADO")
    private String estado;

    @Schema(description = "UUID del organizador", example = "c0000000-0000-4000-8000-000000000002")
    private String idOrganizador;

    @Schema(description = "Alta demanda", example = "false")
    private boolean esAltaDemanda;

    @Schema(description = "Sectores del lugar con precio del evento")
    private List<SectorEventoDTO> sectores = new ArrayList<>();

    /** Artistas/bandas asociados al evento (con rol/orden). */
    @Schema(description = "Artistas/bandas asociados con rol y orden")
    private List<ParticipanteResponseDTO> participantes = new ArrayList<>();
}
