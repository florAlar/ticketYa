package ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ventas.dto.ReporteDTO;
import ventas.exception.UnauthorizedException;
import ventas.service.ReporteService;

/**
 * Reportes del organizador (E2-REP-01): entradas vendidas y devueltas por sector, porcentaje de
 * ocupación y recaudación. Roles: ORGANIZADOR (solo sus eventos) o ADMIN.
 * Identidad: headers X-User-Id / X-User-Role del gateway.
 */
@Tag(name = "Reportes", description = "Reportes del organizador sobre sus eventos: ventas, devoluciones, ocupación y recaudación.")
@RestController
public class ReporteController {

    private static final String DESC_ACCESO =
            "ORGANIZADOR dueño del evento o ADMIN. Importes y conteos según las definiciones de ReporteEvento.";

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Reporte completo del evento",
            description = "Totales y detalle por sector (ventas, devoluciones, ocupación y recaudación). " + DESC_ACCESO,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte",
                    content = @Content(schema = @Schema(implementation = ReporteDTO.Evento.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "403", description = "No es ADMIN ni el organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Evento inexistente"),
            @ApiResponse(responseCode = "503", description = "No se pudo consultar eventos o entradas")
    })
    @GetMapping("/reportes/eventos/{idEvento}")
    public ReporteDTO.Evento reporte(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        return reporteService.reporte(idEvento, requireUser(userId), role);
    }

    @Operation(summary = "Ventas del evento",
            description = "Compras aprobadas y entradas vendidas, devueltas, vigentes e ingresadas. " + DESC_ACCESO,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventas",
                    content = @Content(schema = @Schema(implementation = ReporteDTO.Ventas.class))),
            @ApiResponse(responseCode = "403", description = "No es ADMIN ni el organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Evento inexistente")
    })
    @GetMapping("/reportes/eventos/{idEvento}/ventas")
    public ReporteDTO.Ventas ventas(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        return reporteService.ventas(idEvento, requireUser(userId), role);
    }

    @Operation(summary = "Ventas y devoluciones por sector",
            description = "Una fila por sector con vendidas, devueltas, ocupación y recaudación. " + DESC_ACCESO,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sectores",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReporteDTO.Sector.class)))),
            @ApiResponse(responseCode = "403", description = "No es ADMIN ni el organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Evento inexistente")
    })
    @GetMapping("/reportes/eventos/{idEvento}/sectores")
    public List<ReporteDTO.Sector> sectores(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        return reporteService.sectores(idEvento, requireUser(userId), role);
    }

    @Operation(summary = "Porcentaje de ocupación",
            description = "Ocupación total y por sector (entradas vigentes / capacidad). " + DESC_ACCESO,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ocupación",
                    content = @Content(schema = @Schema(implementation = ReporteDTO.Ocupacion.class))),
            @ApiResponse(responseCode = "403", description = "No es ADMIN ni el organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Evento inexistente")
    })
    @GetMapping("/reportes/eventos/{idEvento}/ocupacion")
    public ReporteDTO.Ocupacion ocupacion(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        return reporteService.ocupacion(idEvento, requireUser(userId), role);
    }

    @Operation(summary = "Recaudación del evento",
            description = "Recaudación bruta, devuelta, pendiente de reintegro y neta, total y por sector. " + DESC_ACCESO,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recaudación",
                    content = @Content(schema = @Schema(implementation = ReporteDTO.Recaudacion.class))),
            @ApiResponse(responseCode = "403", description = "No es ADMIN ni el organizador del evento"),
            @ApiResponse(responseCode = "404", description = "Evento inexistente")
    })
    @GetMapping("/reportes/eventos/{idEvento}/recaudacion")
    public ReporteDTO.Recaudacion recaudacion(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        return reporteService.recaudacion(idEvento, requireUser(userId), role);
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }
}
