package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Evento ya armado para recomendaciones (mismo shape que el catálogo de msvc-eventos). */
@Getter
@Setter
@Schema(name = "EventoRecomendado", description = "Evento sugerido con el motivo de la recomendación")
public class EventoRecomendadoDTO {

    private String id;
    private String nombre;
    private String tipo;
    private LocalDateTime fechaInicio;
    private LugarResumenDTO lugar;
    private String imagenUrl;
    private BigDecimal precioDesde;
    private boolean destacado;
    private String estado;
    private boolean esAltaDemanda;
    private List<String> artistas = new ArrayList<>();

    /** Motivo legible para la UI (ej. "porque te gusta Metallica"). Opcional. */
    @Schema(description = "Motivo legible de la recomendación",
            example = "Porque te gusta Metallica")
    private String motivo;

    @Getter
    @Setter
    public static class LugarResumenDTO {
        private String nombre;
        private String ciudad;
    }
}
