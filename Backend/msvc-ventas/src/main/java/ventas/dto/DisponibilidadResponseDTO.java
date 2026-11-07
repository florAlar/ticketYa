package ventas.dto;

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
public class DisponibilidadResponseDTO {

    private String idEvento;
    /** Suma de cantidadDisponible de todos los sectores (0 si sin cupos). */
    private int disponibleTotal;
    private boolean agotado;
    /** AGOTADO | ULTIMOS_LUGARES | MENOS_DE_50 | MENOS_DE_100 | OK */
    private String nivelCupo;
    /** Label UI es-AR; null si OK / sin aviso. */
    private String nivelCupoLabel;
    private List<CupoSectorDTO> sectores = new ArrayList<>();

    @Getter
    @Setter
    public static class CupoSectorDTO {
        private String idSector;
        private Integer capacidadTotal;
        private Integer cantidadDisponible;
        private Integer cantidadReservada;
        private Integer cantidadVendida;
        private boolean agotado;
        private String nivelCupo;
        private String nivelCupoLabel;
    }
}
