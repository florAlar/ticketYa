package preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Resultado del backfill idempotente de compras hacia el historial. */
@Getter
@Setter
@Schema(name = "BackfillResult", description = "Resumen del backfill de compras al historial")
public class BackfillResultDTO {

    private int procesadas;
    private int insertadas;
    private int actualizadas;
    private int errores;
    private List<String> detalleErrores = new ArrayList<>();
}
