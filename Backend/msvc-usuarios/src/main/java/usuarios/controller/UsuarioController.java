package usuarios.controller;

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
import usuarios.exception.UnauthorizedException;
import usuarios.service.UsuarioService;

/**
 * Usuarios + perfil. Identidad llega del gateway en X-User-Id (E1-GW-02).
 * Paths: /usuarios/** (tras StripPrefix del /api).
 */
@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Smoke infra. */
    @GetMapping("/")
    public String ok() {
        return "microservicio usuarios ok!";
    }

    @GetMapping("/usuarios/me")
    public UsuarioResponseDTO me(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return usuarioService.me(requireUserId(userId));
    }

    @PatchMapping("/usuarios/me")
    public UsuarioResponseDTO patchMe(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody UsuarioUpdateDTO body) {
        return usuarioService.patchMe(requireUserId(userId), body);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponseDTO> listAll() {
        return usuarioService.findAll();
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioResponseDTO byId(@PathVariable String id) {
        return usuarioService.findById(id);
    }

    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioResponseDTO patchEstado(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        Object activo = body.get("activo");
        if (activo == null) {
            throw new IllegalArgumentException("Campo 'activo' requerido");
        }
        boolean value = Boolean.parseBoolean(String.valueOf(activo));
        return usuarioService.patchEstado(id, value);
    }

    @GetMapping("/usuarios/{id}/roles")
    public List<String> getRoles(@PathVariable String id) {
        return usuarioService.getRoles(id);
    }

    @PutMapping("/usuarios/{id}/roles")
    public List<String> putRoles(@PathVariable String id, @Valid @RequestBody RolesUpdateDTO body) {
        return usuarioService.putRoles(id, body);
    }

    private static String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }
}
