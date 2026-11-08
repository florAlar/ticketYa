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

    @Schema(description = "UUID de un lugar existente (alternativo a los campos lugar*)",
            example = "f0000000-0000-4000-8000-000000000001")
    private String idLugar;

    @Schema(description = "Nombre del lugar embebido", example = "Estadio Obras")
    private String lugarNombre;

    @Schema(description = "Ciudad del lugar embebido", example = "Buenos Aires")
    private String lugarCiudad;

    @Schema(description = "Dirección del lugar embebido", example = "Av. del Libertador 7395")
    private String lugarDireccion;

    @Schema(description = "Sectores/tipos de entrada con precio")
    private List<SectorInputDTO> sectores = new ArrayList<>();

    @Getter
    @Setter
    @Schema(name = "EventoSectorInput", description = "Sector del evento con precio y capacidad")
    public static class SectorInputDTO {

        @Schema(description = "UUID del sector si ya existe", example = "a0000000-0000-4000-8000-000000000001")
        private String id;

        @Schema(description = "Nombre del tipo de entrada", example = "Campo")
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

        @Schema(description = "Tipo de sector", example = "CAMPO")
        private String tipo;
    }
}
