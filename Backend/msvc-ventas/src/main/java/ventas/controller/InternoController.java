package ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ventas.dto.CompraInternaDTO;
import ventas.service.FilaService;
import ventas.service.VentaService;

/**
 * Endpoints internos usados por otros microservicios en llamadas directas (Feign).
 * No están expuestos por el gateway.
 */
@Tag(name = "Interno", description = "Operaciones internas entre microservicios.")
@RestController
public class InternoController {

    private final VentaService ventaService;
    private final FilaService filaService;

    public InternoController(VentaService ventaService, FilaService filaService) {
        this.ventaService = ventaService;
        this.filaService = filaService;
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

    @Operation(
            summary = "Sincronizar la fila tras un cambio de estado del evento",
            description = "Uso interno de msvc-eventos al publicar/suspender o cambiar el flag de alta demanda. "
                    + "Exige X-Internal-Token; no está expuesto por el gateway.",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @PostMapping("/internas/fila/{idEvento}/invalidacion")
    public ResponseEntity<Void> invalidarCacheFila(@PathVariable("idEvento") String idEvento) {
        filaService.invalidarCache(idEvento);
        return ResponseEntity.noContent().build();
    }
}
