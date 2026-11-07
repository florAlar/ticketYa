package ventas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Devuelve stock comercial al cupo (p.ej. devolución aprobada en msvc-entradas).
 * Path interno: POST /eventos/{id}/cupos/liberacion
 */
public class LiberarCupoRequestDTO {

    @NotBlank
    private String idSector;

    /** Unidades a devolver (default 1). */
    @Min(1)
    private Integer cantidad = 1;

    public LiberarCupoRequestDTO() {}

    public LiberarCupoRequestDTO(String idSector, Integer cantidad) {
        this.idSector = idSector;
        this.cantidad = cantidad;
    }

    public String getIdSector() {
        return idSector;
    }

    public void setIdSector(String idSector) {
        this.idSector = idSector;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
