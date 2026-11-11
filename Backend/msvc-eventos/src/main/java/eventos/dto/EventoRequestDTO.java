package eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición alineada al form FE gestión ({@code createEvento} body).
 * Puede traer lugar embebido (nombre/ciudad) o {@code idLugar} existente + sectores con precio.
 */
@Getter
@Setter
@Schema(name = "EventoRequest", description = "Alta o edición de evento; lugar embebido o existente + sectores")
public class EventoRequestDTO {

    @Schema(description = "Nombre del evento", example = "Festival TicketYa 2026")
    private String nombre;

    @Schema(description = "Descripción", example = "Una noche de rock y electrónica")
    private String descripcion;

    @Schema(description = "Tipo de evento", example = "FESTIVAL")
    private String tipo;

    @Schema(description = "Inicio (ISO-8601)", example = "2026-03-14T20:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fin (ISO-8601)", example = "2026-03-15T02:00:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Si se omite en alta → BORRADOR. PUBLICADO publica al final del alta",
            example = "BORRADOR")
    private String estado;

    @Schema(description = "Marca de alta demanda", example = "false")
    private Boolean esAltaDemanda;

    @Schema(description = "UUID de un lugar existente (los lugares los crea el ADMIN). Obligatorio para publicar; "
            + "un borrador puede quedar sin lugar (queda en 'Por definir')",
            example = "f0000000-0000-4000-8000-000000000001")
    private String idLugar;

    @Schema(description = "Sectores del lugar que se venden en este evento, con su precio y cupo. Si viene, es la lista "
            + "completa del evento (en borrador reemplaza a la anterior; ausente = no se tocan). Todos deben ser "
            + "sectores del lugar elegido")
    private List<SectorInputDTO> sectores;

    @Getter
    @Setter
    @Schema(name = "EventoSectorInput", description = "Sector del evento con precio y capacidad")
    public static class SectorInputDTO {

        @Schema(description = "UUID del sector del lugar (obligatorio: los sectores los define el lugar)", example = "a0000000-0000-4000-8000-000000000001")
        private String id;

        @Schema(description = "Solo informativo: si viene debe coincidir con el del sector (lo define el lugar)", example = "Campo")
        private String nombre;

        @Schema(description = "Precio unitario en ARS", example = "25000.00")
        private BigDecimal precio;

        @Schema(description = "Capacidad total del sector", example = "500")
        private Integer capacidadTotal;

        @Schema(description = "Capacidad (alias de capacidadTotal)", example = "500")
        private Integer capacidad;

        @Schema(description = "Sector sin cupo limitado", example = "false")
        private Boolean sinCupo;

        @Schema(description = "Sector numerado", example = "false")
        private Boolean esNumerado;

        @Schema(description = "Filas del sector numerado (obligatorio al publicar si esNumerado; 1..200)", example = "20")
        private Integer filas;

        @Schema(description = "Asientos por fila del sector numerado (obligatorio al publicar si esNumerado; 1..500). "
                + "Si hay layout, la capacidad se calcula como filas * asientosPorFila", example = "15")
        private Integer asientosPorFila;

        @Schema(description = "Tipo de sector", example = "CAMPO")
        private String tipo;
    }
}
