package ventas.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisponibilidadResponseDTO {

    private String idEvento;
    private List<CupoSectorDTO> sectores = new ArrayList<>();

    @Getter
    @Setter
    public static class CupoSectorDTO {
        private String idSector;
        private Integer capacidadTotal;
        private Integer cantidadDisponible;
        private Integer cantidadReservada;
        private Integer cantidadVendida;
    }
}
