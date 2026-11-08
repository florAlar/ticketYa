package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Disponibilidad comercial por evento.
 * Números reales en sectores + disponibleTotal; nivelCupo/label son clasificación del DTO.
 */
@Getter
@Setter
@Schema(name = "Disponibilidad", description = "Disponibilidad comercial por sector y clasificación de cupo")
public class DisponibilidadResponseDTO {

    @Schema(description = "UUID del evento", example = "d0000000-0000-4000-8000-000000000001")
    private String idEvento;

    /** Suma de cantidadDisponible de todos los sectores (0 si sin cupos). */
    @Schema(description = "Suma de unidades disponibles de todos los sectores", example = "118")
    private int disponibleTotal;

    @Schema(description = "true si no queda ninguna unidad", example = "false")
    private boolean agotado;

    /** AGOTADO | ULTIMOS_LUGARES | MENOS_DE_50 | MENOS_DE_100 | OK */
    @Schema(description = "Clasificación del disponible total",
            allowableValues = {"AGOTADO", "ULTIMOS_LUGARES", "MENOS_DE_50", "MENOS_DE_100", "OK"},
            example = "OK")
    private String nivelCupo;

    /** Label UI es-AR; null si OK / sin aviso. */
    @Schema(description = "Etiqueta para UI (null si OK o sin aviso)", example = "Últimos lugares")
    private String nivelCupoLabel;

    @Schema(description = "Disponibilidad por sector")
    private List<CupoSectorDTO> sectores = new ArrayList<>();

    @Getter
    @Setter
    @Schema(name = "CupoSector", description = "Disponibilidad de un sector del evento")
    public static class CupoSectorDTO {

        @Schema(description = "UUID del sector", example = "c0000000-0000-4000-8000-000000000011")
        private String idSector;

        @Schema(description = "Capacidad total del sector", example = "120")
        private Integer capacidadTotal;

        @Schema(description = "Unidades libres", example = "118")
        private Integer cantidadDisponible;

        @Schema(description = "Unidades reservadas (TTL activo)", example = "2")
        private Integer cantidadReservada;

        @Schema(description = "Unidades vendidas", example = "0")
        private Integer cantidadVendida;

        @Schema(description = "true si el sector no tiene unidades libres", example = "false")
        private boolean agotado;

        @Schema(description = "Clasificación del disponible del sector",
                allowableValues = {"AGOTADO", "ULTIMOS_LUGARES", "MENOS_DE_50", "MENOS_DE_100", "OK"},
                example = "MENOS_DE_100")
        private String nivelCupo;

        @Schema(description = "Etiqueta para UI del sector", example = "Menos de 100")
        private String nivelCupoLabel;
    }
}
