package eventos.controller;

import eventos.dto.EventoParticipantesRequestDTO;
import eventos.dto.ParticipanteRequestDTO;
import eventos.dto.ParticipanteResponseDTO;
import eventos.exception.ForbiddenException;
import eventos.exception.UnauthorizedException;
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

/**
 * Artistas/bandas. Lectura abierta; escritura ORG/ADMIN. Asociar: dueño del evento o ADMIN.
 */
@Tag(name = "Participantes", description = "Artistas, bandas y elenco; asociación a eventos.")
@RestController
public class ParticipanteController {

    private final EventoService eventoService;

    public ParticipanteController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @Operation(
            summary = "Buscar participantes",
            description = "Catálogo abierto de artistas/bandas, con búsqueda y filtro por tipo.")
    @ApiResponse(responseCode = "200", description = "Participantes",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipanteResponseDTO.class))))
    @GetMapping("/participantes")
    public List<ParticipanteResponseDTO> listar(
            @Parameter(description = "Búsqueda por nombre", example = "Indio")
            @RequestParam(required = false) String q,
            @Parameter(description = "Tipo: ARTISTA | BANDA | ELENCO", example = "BANDA")
            @RequestParam(required = false) String tipo) {
        return eventoService.listarParticipantes(q, tipo);
    }

    @Operation(
            summary = "Obtener un participante",
            description = "Detalle de un artista/banda/elenco. Endpoint público.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado",
                    content = @Content(schema = @Schema(implementation = ParticipanteResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Participante inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/participantes/{id}")
    public ParticipanteResponseDTO uno(
            @Parameter(description = "UUID del participante", required = true) @PathVariable String id) {
        return eventoService.obtenerParticipante(id);
    }

    @Operation(
            summary = "Crear participante",
            description = "Alta de artista/banda/elenco. Requiere ORGANIZADOR o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participante creado",
                    content = @Content(schema = @Schema(implementation = ParticipanteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/participantes")
    public ResponseEntity<ParticipanteResponseDTO> crear(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody ParticipanteRequestDTO body) {
        requireOrgOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearParticipante(body));
    }

    @Operation(
            summary = "Editar participante",
            description = "Actualiza un participante del catálogo (nombre, tipo, descripción). Solo ADMIN: el cambio se ve en todos los eventos que lo usan. 409 si el nombre ya existe.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante actualizado",
                    content = @Content(schema = @Schema(implementation = ParticipanteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Participante inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PutMapping("/participantes/{id}")
    public ParticipanteResponseDTO actualizar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del participante", required = true) @PathVariable String id,
            @RequestBody ParticipanteRequestDTO body) {
        requireAdmin(role);
        return eventoService.actualizarParticipante(id, body);
    }

    @Operation(
            summary = "Eliminar participante",
            description = "Elimina un participante del catálogo. Solo ADMIN. Si participa en algún evento responde 409: en ese caso solo se edita.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participante eliminado"),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Participante inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "El participante participa en eventos",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @DeleteMapping("/participantes/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del participante", required = true) @PathVariable String id) {
        requireAdmin(role);
        eventoService.eliminarParticipante(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Participantes de un evento",
            description = "Artistas/bandas asociados, con rol y orden. Endpoint público.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participantes del evento",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipanteResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos/{id}/participantes")
    public List<ParticipanteResponseDTO> deEvento(
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id) {
        return eventoService.listarParticipantesDeEvento(id);
    }

    @Operation(
            summary = "Asociar participantes a un evento",
            description = """
                    Reemplaza el set de participantes del evento. Acepta `ids` (simple) o
                    `participantes` (id + rol + orden). Dueño del evento o ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participantes asociados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipanteResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente o no es dueño",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento o participante inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PutMapping("/eventos/{id}/participantes")
    public List<ParticipanteResponseDTO> asociar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id,
            @RequestBody EventoParticipantesRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.asociarParticipantes(id, body, requireUser(userId), role);
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }

    private static void requireAdmin(String role) {
        if (role == null || !"ADMIN".equals(role.trim().toUpperCase())) {
            throw new ForbiddenException("Se requiere rol ADMIN");
        }
    }

    private static void requireOrgOrAdmin(String role) {
        if (role == null) {
            throw new ForbiddenException("Se requiere rol ORGANIZADOR o ADMIN");
        }
        String r = role.trim().toUpperCase();
        if (!"ORGANIZADOR".equals(r) && !"ADMIN".equals(r)) {
            throw new ForbiddenException("Se requiere rol ORGANIZADOR o ADMIN");
        }
    }
}
