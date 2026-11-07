package ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Apertura de stock comercial al publicar un evento (msvc-eventos → Feign).
 * Idempotente por (idEvento, idSector): si el cupo ya existe, no resetea vendido/reservado.
 */
public class AbrirCuposRequestDTO {

    @NotEmpty
    @Valid
    private List<Item> sectores = new ArrayList<>();

    public List<Item> getSectores() {
        return sectores;
    }

    public void setSectores(List<Item> sectores) {
        this.sectores = sectores != null ? sectores : new ArrayList<>();
    }

    public static class Item {
        @NotBlank
        private String idSector;

        @NotNull
        @Min(1)
        private Integer capacidadTotal;

        public String getIdSector() {
            return idSector;
        }

        public void setIdSector(String idSector) {
            this.idSector = idSector;
        }

        public Integer getCapacidadTotal() {
            return capacidadTotal;
        }

        public void setCapacidadTotal(Integer capacidadTotal) {
            this.capacidadTotal = capacidadTotal;
        }
    }
}
