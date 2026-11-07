package eventos.controller;

import eventos.dto.EventoParticipantesRequestDTO;
import eventos.dto.ParticipanteRequestDTO;
import eventos.dto.ParticipanteResponseDTO;
import eventos.exception.ForbiddenException;
import eventos.exception.UnauthorizedException;
import eventos.service.EventoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RestController
public class ParticipanteController {

    private final EventoService eventoService;

    public ParticipanteController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/participantes")
    public List<ParticipanteResponseDTO> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo) {
        return eventoService.listarParticipantes(q, tipo);
    }

    @GetMapping("/participantes/{id}")
    public ParticipanteResponseDTO uno(@PathVariable String id) {
        return eventoService.obtenerParticipante(id);
    }

    @PostMapping("/participantes")
    public ResponseEntity<ParticipanteResponseDTO> crear(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody ParticipanteRequestDTO body) {
        requireOrgOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearParticipante(body));
    }

    @PutMapping("/participantes/{id}")
    public ParticipanteResponseDTO actualizar(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody ParticipanteRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.actualizarParticipante(id, body);
    }

    @GetMapping("/eventos/{id}/participantes")
    public List<ParticipanteResponseDTO> deEvento(@PathVariable String id) {
        return eventoService.listarParticipantesDeEvento(id);
    }

    @PutMapping("/eventos/{id}/participantes")
    public List<ParticipanteResponseDTO> asociar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
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
