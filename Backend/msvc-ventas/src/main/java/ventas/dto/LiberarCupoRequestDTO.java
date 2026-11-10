package ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

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

    /** Sector numerado: asientos vendidos a liberar. Si se informa, reemplaza a cantidad. */
    @Schema(description = "Asientos vendidos a liberar (sector numerado). Si se envía, se ignora cantidad",
            nullable = true)
    private List<@Valid AsientoRefDTO> asientos;

    public LiberarCupoRequestDTO() {}

    public LiberarCupoRequestDTO(String idSector, Integer cantidad) {
        this.idSector = idSector;
        this.cantidad = cantidad;
    }

    public List<AsientoRefDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoRefDTO> asientos) {
        this.asientos = asientos;
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
