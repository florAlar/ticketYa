package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "AbrirCuposRequest", description = "Sectores y capacidades para abrir el stock comercial")
public class AbrirCuposRequestDTO {

    @NotEmpty
    @Valid
    @Schema(description = "Sectores a abrir (al menos uno)")
    private List<Item> sectores = new ArrayList<>();

    public List<Item> getSectores() {
        return sectores;
    }

    public void setSectores(List<Item> sectores) {
        this.sectores = sectores != null ? sectores : new ArrayList<>();
    }

    @Schema(name = "AbrirCupoItem", description = "Capacidad comercial de un sector")
    public static class Item {

        @NotBlank
        @Schema(description = "UUID del sector", example = "a0000000-0000-4000-8000-000000000001")
        private String idSector;

        @NotNull
        @Min(1)
        @Schema(description = "Capacidad total a abrir", example = "500")
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
