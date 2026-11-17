package preferencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import preferencias.dto.BackfillResultDTO;
import preferencias.service.BackfillService;

/**
 * Endpoint interno para importar las compras históricas de msvc-ventas.
 * No está expuesto por el gateway y exige {@code X-Internal-Token}.
 */
@Tag(name = "Interno", description = "Operaciones internas entre microservicios.")
@RestController
public class InternoController {

    private final BackfillService backfillService;

    public InternoController(BackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @Operation(summary = "Backfill de compras al historial",
            description = """
                    Importa las compras APROBADAS de msvc-ventas y las upsertea por idCompra.
                    Idempotente. Requiere X-Internal-Token; no está expuesto por el gateway.
                    """,
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @Parameter(name = "X-Internal-Token", in = ParameterIn.HEADER, required = true,
            description = "Token compartido entre microservicios")
    @PostMapping("/internas/backfill")
    public BackfillResultDTO backfill() {
        return backfillService.ejecutar();
    }
}
