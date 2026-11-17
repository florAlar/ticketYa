package ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ventas.dto.CompraInternaDTO;
import ventas.service.VentaService;

/**
 * Endpoints internos usados por otros microservicios en llamadas directas (Feign).
 * No están expuestos por el gateway.
 */
@Tag(name = "Interno", description = "Operaciones internas entre microservicios.")
@RestController
public class InternoController {

    private final VentaService ventaService;

    public InternoController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @Operation(
            summary = "Compras aprobadas para backfill del historial",
            description = "Uso interno de msvc-preferencias. No se expone por el gateway.",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @GetMapping("/internas/compras")
    public List<CompraInternaDTO> comprasInternas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ventaService.listarComprasInternas(desde, hasta);
    }
}
