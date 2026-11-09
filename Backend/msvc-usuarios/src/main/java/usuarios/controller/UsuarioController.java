package usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import usuarios.dto.RolesUpdateDTO;
import usuarios.dto.UsuarioResponseDTO;
import usuarios.dto.UsuarioUpdateDTO;
import usuarios.exception.ApiError;
import usuarios.exception.UnauthorizedException;
import usuarios.service.UsuarioService;

/**
 * Usuarios + perfil. Identidad llega del gateway en X-User-Id (E1-GW-02).
 * Paths: /usuarios/** (tras StripPrefix del /api).
 */
@Tag(name = "Usuarios", description = "Perfil propio y administración de usuarios (ADMIN en el gateway).")
@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Smoke infra. */
    @Operation(
            summary = "Smoke del microservicio",
            description = "Chequeo de infraestructura. No forma parte del contrato de negocio.",
            tags = "Smoke",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @ApiResponse(responseCode = "200", description = "Texto de estado")
    @GetMapping("/")
    public String ok() {
        return "microservicio usuarios ok!";
    }

    @Operation(
            summary = "Obtener mi perfil",
            description = "Devuelve el usuario indicado por el header X-User-Id que propaga el gateway.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario",
                    content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id (no pasó por el gateway)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/usuarios/me")
    public UsuarioResponseDTO me(
            @Parameter(description = "UUID del usuario autenticado (lo inyecta el gateway)", required = true,
                    example = "b0000000-0000-4000-8000-000000000001")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return usuarioService.me(requireUserId(userId));
    }

    @Operation(
            summary = "Editar mi perfil",
            description = "Actualiza nombre, apellido y ciudad. Email, roles y estado no se modifican.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/usuarios/me")
    public UsuarioResponseDTO patchMe(
            @Parameter(description = "UUID del usuario autenticado", required = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody UsuarioUpdateDTO body) {
        return usuarioService.patchMe(requireUserId(userId), body);
    }

    @Operation(
            summary = "Listar usuarios",
            description = "Listado completo con roles. En el gateway está restringido a ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de usuarios",
                    content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(implementation = UsuarioResponseDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente (gateway exige ADMIN)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/usuarios")
    public List<UsuarioResponseDTO> listAll() {
        return usuarioService.findAll();
    }

    @Operation(
            summary = "Obtener usuario por id",
            description = "Detalle de cualquier usuario. En el gateway está restringido a ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/usuarios/{id}")
    public UsuarioResponseDTO byId(
            @Parameter(description = "UUID del usuario", required = true,
                    example = "b0000000-0000-4000-8000-000000000002")
            @PathVariable String id) {
        return usuarioService.findById(id);
    }

    @Operation(
            summary = "Activar o desactivar usuario",
            description = "Cambia el flag activo. Restringido a ADMIN en el gateway.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Falta el campo activo",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioResponseDTO patchEstado(
            @Parameter(description = "UUID del usuario", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Estado deseado",
                    content = @Content(schema = @Schema(type = "object",
                            example = "{\"activo\": true}")))
            @RequestBody Map<String, Object> body) {
        Object activo = body.get("activo");
        if (activo == null) {
            throw new IllegalArgumentException("Campo 'activo' requerido");
        }
        boolean value = Boolean.parseBoolean(String.valueOf(activo));
        return usuarioService.patchEstado(id, value);
    }

    @Operation(
            summary = "Consultar roles de un usuario",
            description = "Roles asignados. Restringido a ADMIN en el gateway.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombres de roles",
                    content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(type = "string", example = "CLIENTE")))),
            @ApiResponse(responseCode = "404", description = "Usuario inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/usuarios/{id}/roles")
    public List<String> getRoles(
            @Parameter(description = "UUID del usuario", required = true)
            @PathVariable String id) {
        return usuarioService.getRoles(id);
    }

    @Operation(
            summary = "Reemplazar roles de un usuario",
            description = "Asigna el conjunto de roles indicado (reemplaza los anteriores). "
                    + "Acepta el alias ACCESO y lo normaliza a PERSONAL_ACCESO. Restringido a ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles resultantes",
                    content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(type = "string", example = "ORGANIZADOR")))),
            @ApiResponse(responseCode = "400", description = "Lista de roles vacía o inválida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o rol inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/usuarios/{id}/roles")
    public List<String> putRoles(
            @Parameter(description = "UUID del usuario", required = true)
            @PathVariable String id,
            @Valid @RequestBody RolesUpdateDTO body) {
        return usuarioService.putRoles(id, body);
    }

    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }
}
