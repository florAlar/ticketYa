package entradas.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import entradas.dto.DevolucionCreateRequestDTO;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;
import entradas.exception.UnauthorizedException;
import entradas.service.EntradaService;

/**
 * Paths internos post StripPrefix /api.
 * Identidad: X-User-Id / X-User-Role del gateway.
 */
@RestController
public class EntradaController {

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @GetMapping("/")
    public String ok() {
        return "microservicio entradas ok!";
    }

    /** Emisión post-compra (ventas Feign o cliente autenticado dueño). */
    @PostMapping("/entradas/emision")
    public ResponseEntity<List<EntradaResponseDTO>> emitir(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody EmitirEntradasRequestDTO body) {
        // dueño de la compra o rol de sistema/admin/ventas-internal
        if (userId != null && !userId.isBlank() && !userId.equals(body.idCliente()) && !isStaff(role)) {
            throw new UnauthorizedException("No puede emitir entradas para otro cliente");
        }
        List<EntradaResponseDTO> created = entradaService.emitir(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/entradas")
    public List<EntradaResponseDTO> mias(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return entradaService.listarMias(requireUser(userId));
    }

    /**
     * GET /entradas/{id} — un solo método.
     * Dueño (X-User-Id = idCliente) o ADMIN (cualquier id).
     */
    @GetMapping("/entradas/{id}")
    public EntradaResponseDTO una(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id) {
        boolean admin = isAdmin(role);
        // admin no exige ser dueño; cliente sí (id en header)
        String cliente = admin ? (userId != null ? userId.trim() : "") : requireUser(userId);
        return entradaService.obtener(cliente, id, admin);
    }

    @GetMapping("/entradas/{id}/qr")
    public QrResponseDTO qr(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return entradaService.qrMia(requireUser(userId), id);
    }

    @PostMapping("/entradas/{id}/devoluciones")
    public ResponseEntity<DevolucionResponseDTO> solicitarDev(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id,
            @RequestBody(required = false) DevolucionCreateRequestDTO body) {
        DevolucionResponseDTO d = entradaService.solicitarDevolucion(
                requireUser(userId), id, body != null ? body : new DevolucionCreateRequestDTO(null));
        return ResponseEntity.status(HttpStatus.CREATED).body(d);
    }

    @GetMapping("/devoluciones/{id}")
    public DevolucionResponseDTO getDev(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id) {
        boolean admin = isAdmin(role);
        return entradaService.obtenerDevolucion(admin ? "" : requireUser(userId), id, admin);
    }

    @GetMapping("/devoluciones")
    public List<DevolucionResponseDTO> listDev(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String scope) {
        if (isAdmin(role) && (scope == null || "pendientes".equalsIgnoreCase(scope) || "all".equalsIgnoreCase(scope))) {
            return entradaService.listarDevolucionesPendientes();
        }
        return entradaService.listarDevolucionesCliente(requireUser(userId));
    }

    @PostMapping("/devoluciones/{id}/aprobacion")
    public DevolucionResponseDTO aprobar(
            @RequestHeader(value = "X-User-Role", required = false) String role, @PathVariable String id) {
        requireAdmin(role);
        return entradaService.aprobarDevolucion(id);
    }

    @PostMapping("/devoluciones/{id}/rechazo")
    public DevolucionResponseDTO rechazar(
            @RequestHeader(value = "X-User-Role", required = false) String role, @PathVariable String id) {
        requireAdmin(role);
        return entradaService.rechazarDevolucion(id);
    }

    /** Contrato E1-ENT-05: cancelación de evento → reintegro de entradas. */
    @PostMapping("/eventos/{idEvento}/reintegro-cancelacion")
    public ResponseEntity<java.util.Map<String, Object>> reintegroCancelacion(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String idEvento) {
        requireAdmin(role);
        int n = entradaService.reintegroPorCancelacionEvento(idEvento);
        return ResponseEntity.ok(java.util.Map.of("idEvento", idEvento, "entradasAfectadas", n));
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }

    private static void requireAdmin(String role) {
        if (!isAdmin(role)) {
            throw new UnauthorizedException("Se requiere rol ADMIN");
        }
    }

    private static boolean isAdmin(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }

    private static boolean isStaff(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return "ADMIN".equals(r) || "ORGANIZADOR".equals(r) || "SYSTEM".equals(r);
    }
}
