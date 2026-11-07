package eventos.controller;

import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoEstadoRequestDTO;
import eventos.dto.EventoFechaRequestDTO;
import eventos.dto.EventoRequestDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.PageResponseDTO;
import eventos.dto.PrecioSectorRequestDTO;
import eventos.dto.SectorEventoDTO;
import eventos.exception.ForbiddenException;
import eventos.exception.UnauthorizedException;
import eventos.service.EventoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Smoke infra — no es API de negocio. */
    @GetMapping("/")
    public String ok() {
        return "microservicio eventos ok!";
    }

    /**
         * Catálogo / listado.
         * Default: PUBLICADO + AGOTADO (visibles en home/catálogo).
         * Exacto: estado=PUBLICADO|AGOTADO|…. Gestión: estado=TODOS|ALL.
         */
    @GetMapping("/eventos")
        public PageResponseDTO<EventoResponseDTO> listar(
                @RequestParam(required = false) String tipo,
                @RequestParam(required = false) String ciudad,
                @RequestParam(required = false) String organizador,
                @RequestParam(required = false) String participante,
                @RequestParam(required = false) String estado,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                @RequestParam(required = false, defaultValue = "fecha") String orden,
                @RequestParam(required = false, defaultValue = "0") int page,
                @RequestParam(required = false, defaultValue = "20") int size) {
            return eventoService.buscar(
                    tipo,
                    ciudad,
                    organizador,
                    participante,
                    estado,
                    fechaDesde,
                    fechaHasta,
                    orden,
                    page,
                    size);
        }

    @GetMapping("/eventos/{id}")
    public EventoDetalleResponseDTO detalle(@PathVariable String id) {
        return eventoService.obtenerPorId(id);
    }

    /** POST /eventos — crear borrador (ORGANIZADOR/ADMIN). Body ≈ form FE gestión. */
    @PostMapping("/eventos")
    public ResponseEntity<EventoDetalleResponseDTO> crear(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody EventoRequestDTO body) {
        requireOrgOrAdmin(role);
        EventoDetalleResponseDTO created = eventoService.crear(body, requireUser(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/eventos/{id}")
    public EventoDetalleResponseDTO actualizar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody EventoRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.actualizar(id, body, requireUser(userId), role);
    }

    @DeleteMapping("/eventos/{id}")
    public ResponseEntity<Void> eliminar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id) {
        requireOrgOrAdmin(role);
        eventoService.eliminarBorrador(id, requireUser(userId), role);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/eventos/{id}/publicacion")
    public EventoDetalleResponseDTO publicar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id) {
        requireOrgOrAdmin(role);
        return eventoService.publicar(id, requireUser(userId), role);
    }

    @PatchMapping("/eventos/{id}/estado")
    public EventoDetalleResponseDTO patchEstado(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody EventoEstadoRequestDTO body) {
        String estado = body != null ? body.getEstado() : null;
        return eventoService.patchEstado(id, estado, userId, role);
    }

    @PatchMapping("/eventos/{id}/fecha")
    public EventoDetalleResponseDTO patchFecha(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody EventoFechaRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.patchFecha(id, body, requireUser(userId), role);
    }

    @GetMapping("/eventos/{id}/sectores")
    public List<SectorEventoDTO> sectoresEvento(@PathVariable String id) {
        return eventoService.listarSectoresEvento(id);
    }

    @PutMapping("/eventos/{id}/sectores/{sectorId}/precio")
    public SectorEventoDTO precioSector(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @PathVariable String sectorId,
            @RequestBody PrecioSectorRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.upsertPrecioSector(id, sectorId, body, requireUser(userId), role);
    }

    /** Listado gestión ORGANIZADOR. */
    @GetMapping("/organizadores/me/eventos")
    public List<EventoResponseDTO> misEventos(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireOrgOrAdmin(role);
        return eventoService.listarDeOrganizador(requireUser(userId));
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
