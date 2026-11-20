package ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ventas.dto.FilaEstadoResponseDTO;
import ventas.dto.FilaHabilitacionRequestDTO;
import ventas.dto.FilaHabilitacionResponseDTO;
import ventas.dto.FilaIngresoResponseDTO;
import ventas.dto.FilaPosicionResponseDTO;
import ventas.exception.ForbiddenException;
import ventas.exception.UnauthorizedException;
import ventas.service.FilaService;

/** Fila virtual (E2-FILA-01). Paths internos tras el StripPrefix del gateway. */
@Tag(name = "Fila virtual", description = "Ingreso, posición y estado de la cola de alta demanda.")
@RestController
@RequestMapping("/eventos/{idEvento}/fila")
public class FilaController {

    private final FilaService filaService;

    public FilaController(FilaService filaService) {
        this.filaService = filaService;
    }

    @Operation(summary = "Ingresar a la fila", description = "Idempotente: 201 la primera vez, 200 si ya estaba.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Primera entrada",
                    content = @Content(schema = @Schema(implementation = FilaIngresoResponseDTO.class))),
            @ApiResponse(responseCode = "200", description = "Reingreso con la misma posición"),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "409", description = "FILA_EVENTO_NO_ELEGIBLE o FILA_TURNO_USADO"),
            @ApiResponse(responseCode = "503", description = "FILA_NO_DISPONIBLE")
    })
    @PostMapping("/ingreso")
    public ResponseEntity<FilaIngresoResponseDTO> ingresar(
            @Parameter(description = "UUID del evento") @PathVariable String idEvento,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId) {
        FilaService.IngresoResultado resultado = filaService.ingresar(idEvento, requireUser(userId));
        return ResponseEntity.status(resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.respuesta());
    }

    @Operation(summary = "Posición propia", description = "Turno inmutable, tanda y cuánto falta (delante).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posición",
                    content = @Content(schema = @Schema(implementation = FilaPosicionResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "FILA_NO_INGRESADO o evento inexistente"),
            @ApiResponse(responseCode = "409", description = "FILA_EVENTO_NO_ELEGIBLE"),
            @ApiResponse(responseCode = "503", description = "FILA_NO_DISPONIBLE")
    })
    @GetMapping("/posicion")
    public FilaPosicionResponseDTO posicion(
            @Parameter(description = "UUID del evento") @PathVariable String idEvento,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return filaService.posicion(idEvento, requireUser(userId));
    }

    @Operation(summary = "Habilitar tanda (override ADMIN)",
            description = "Defensa en profundidad: el gateway también restringe este path a ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tanda fijada",
                    content = @Content(schema = @Schema(implementation = FilaHabilitacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "hastaPosicion ausente o < 1"),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMIN"),
            @ApiResponse(responseCode = "409", description = "FILA_TANDA_NO_REDUCIBLE o FILA_EVENTO_NO_ELEGIBLE"),
            @ApiResponse(responseCode = "503", description = "FILA_NO_DISPONIBLE")
    })
    @PostMapping("/habilitacion")
    public FilaHabilitacionResponseDTO habilitar(
            @Parameter(description = "UUID del evento") @PathVariable String idEvento,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String rol,
            @Valid @RequestBody FilaHabilitacionRequestDTO body) {
        if (!"ADMIN".equals(rol)) {
            throw new ForbiddenException("Solo ADMIN puede habilitar tandas");
        }
        return filaService.habilitar(idEvento, body.hastaPosicion(), rol);
    }

    @Operation(summary = "Estado general de la fila",
            description = "Público a través del gateway. No expone identidades.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado",
                    content = @Content(schema = @Schema(implementation = FilaEstadoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente"),
            @ApiResponse(responseCode = "503", description = "FILA_NO_DISPONIBLE")
    })
    @GetMapping("/estado")
    public FilaEstadoResponseDTO estado(
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        return filaService.estado(idEvento);
    }

    /** La habilitación (FIL-02) y la validación (FIL-03) se agregan en sus bloques. */

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }
}
