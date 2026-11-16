package eventos.dto;

import eventos.util.Coordenadas;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "Coordenadas", description = "Coordenadas geográficas en grados decimales (WGS84)")
public record CoordenadasDTO(
        @Schema(description = "Latitud (-90 a 90)", example = "-34.545300") BigDecimal latitud,
        @Schema(description = "Longitud (-180 a 180)", example = "-58.449700") BigDecimal longitud) {

    public CoordenadasDTO {
        Coordenadas.validar(latitud, longitud);
        latitud = Coordenadas.normalizar(latitud);
        longitud = Coordenadas.normalizar(longitud);
    }
}
