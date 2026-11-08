package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "EventoResumen", description = "Evento en catálogo/listado")
public class EventoResponseDTO {

    @Schema(description = "UUID del evento", example = "e0000000-0000-4000-8000-000000000001")
    private String id;

    @Schema(description = "Nombre", example = "Festival TicketYa 2026")
    private String nombre;

    @Schema(description = "Tipo", example = "FESTIVAL")
    private String tipo;

    @Schema(description = "Inicio (ISO-8601)", example = "2026-03-14T20:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Lugar resumido")
    private LugarResumenDTO lugar;

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
}
