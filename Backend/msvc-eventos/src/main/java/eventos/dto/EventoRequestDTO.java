package eventos.dto;

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
public class EventoRequestDTO {

    private String nombre;
    private String descripcion;
    private String tipo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    /** Si se omite en alta → BORRADOR. PUBLICADO en alta dispara publicación al final. */
    private String estado;
    private Boolean esAltaDemanda;

    private String idLugar;
    private String lugarNombre;
    private String lugarCiudad;
    private String lugarDireccion;

    private List<SectorInputDTO> sectores = new ArrayList<>();

    @Getter
    @Setter
    public static class SectorInputDTO {
        private String id;
        private String nombre;
        private BigDecimal precio;
        private Integer capacidadTotal;
        private Integer capacidad;
        private Boolean sinCupo;
        private Boolean esNumerado;
        private String tipo;
    }
}
