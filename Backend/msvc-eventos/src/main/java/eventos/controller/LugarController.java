package eventos.controller;

import eventos.dto.AsientoResponseDTO;
import eventos.dto.LugarRequestDTO;
import eventos.dto.LugarResponseDTO;
import eventos.dto.SectorEventoDTO;
import eventos.dto.SectorRequestDTO;
import eventos.exception.ForbiddenException;
import eventos.service.EventoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lugares", description = "Lugares, sectores y asientos usados por los eventos.")
@RestController
public class LugarController {

    private final EventoService eventoService;

    public LugarController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @Operation(
            summary = "Listar lugares",
            description = "Lugares con sus sectores. Requiere JWT (el gateway no lo expone como público).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Lugares",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LugarResponseDTO.class))))
    @GetMapping("/lugares")
    public List<LugarResponseDTO> listar(
            @Parameter(description = "Ciudad a filtrar", example = "Buenos Aires")
            @RequestParam(required = false) String ciudad) {
        return eventoService.listarLugares(ciudad);
    }

    @Operation(
            summary = "Obtener un lugar",
            description = "Detalle del lugar con sus sectores. Requiere JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lugar encontrado",
                    content = @Content(schema = @Schema(implementation = LugarResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Lugar inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/lugares/{id}")
    public LugarResponseDTO uno(
            @Parameter(description = "UUID del lugar", required = true) @PathVariable String id) {
        return eventoService.obtenerLugar(id);
    }

    @Operation(
            summary = "Crear lugar",
            description = "Alta de lugar. Requiere ORGANIZADOR o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lugar creado",
                    content = @Content(schema = @Schema(implementation = LugarResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/lugares")
    public ResponseEntity<LugarResponseDTO> crear(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody LugarRequestDTO body) {
        requireAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearLugar(body));
    }

    @Operation(
            summary = "Editar lugar",
            description = "Actualiza los datos del lugar. Solo ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lugar actualizado",
                    content = @Content(schema = @Schema(implementation = LugarResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Lugar inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PutMapping("/lugares/{id}")
    public LugarResponseDTO actualizar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del lugar", required = true) @PathVariable String id,
            @RequestBody LugarRequestDTO body) {
        requireAdmin(role);
        return eventoService.actualizarLugar(id, body);
    }

    @Operation(
            summary = "Eliminar lugar",
            description = "Elimina un lugar y sus sectores. Solo ADMIN. Si el lugar tiene eventos asociados "
                    + "(de cualquier estado) responde 409: en ese caso solo se puede editar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lugar eliminado"),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Lugar inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "El lugar tiene eventos asociados",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @DeleteMapping("/lugares/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del lugar", required = true) @PathVariable String id) {
        requireAdmin(role);
        eventoService.eliminarLugar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Crear sector en un lugar",
            description = "Alta de sector (nombre, capacidad, tipo, numerado). Solo ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sector creado",
                    content = @Content(schema = @Schema(implementation = SectorEventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Lugar inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/lugares/{id}/sectores")
    public ResponseEntity<SectorEventoDTO> crearSector(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del lugar", required = true) @PathVariable String id,
            @RequestBody SectorRequestDTO body) {
        requireAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearSector(id, body));
    }

    @Operation(
            summary = "Editar sector",
            description = "Actualiza un sector existente. Solo ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sector actualizado",
                    content = @Content(schema = @Schema(implementation = SectorEventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Sector inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PutMapping("/sectores/{id}")
    public SectorEventoDTO actualizarSector(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del sector", required = true) @PathVariable String id,
            @RequestBody SectorRequestDTO body) {
        requireAdmin(role);
        return eventoService.actualizarSector(id, body);
    }

    @Operation(
            summary = "Listar asientos de un sector",
            description = "Asientos del sector del lugar. Requiere JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asientos",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AsientoResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Lugar o sector inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/lugares/{id}/sectores/{sectorId}/asientos")
    public List<AsientoResponseDTO> asientos(
            @Parameter(description = "UUID del lugar", required = true) @PathVariable String id,
            @Parameter(description = "UUID del sector", required = true) @PathVariable String sectorId) {
        return eventoService.listarAsientos(id, sectorId);
    }

    private static void requireAdmin(String role) {
        if (role == null || !"ADMIN".equals(role.trim().toUpperCase())) {
            throw new ForbiddenException("Se requiere rol ADMIN");
        }
    }
}
