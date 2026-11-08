package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Devuelve stock comercial al cupo (p.ej. devolución aprobada en msvc-entradas).
 * Path interno: POST /eventos/{id}/cupos/liberacion
 */
@Schema(name = "LiberarCupoRequest", description = "Unidades a devolver al disponible de un sector")
public class LiberarCupoRequestDTO {

    @NotBlank
    @Schema(description = "UUID del sector", example = "a0000000-0000-4000-8000-000000000001")
    private String idSector;

    /** Unidades a devolver (default 1). */
    @Min(1)
    @Schema(description = "Unidades a devolver", example = "1")
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
