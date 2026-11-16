package preferencias.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import preferencias.dto.MatchingClientesRequestDTO;
import preferencias.dto.NotificacionesPreferenciasDTO;
import preferencias.dto.PreferenciaRequestDTO;
import preferencias.dto.PreferenciaResponseDTO;
import preferencias.exception.UnauthorizedException;
import preferencias.service.PreferenciaService;

@RestController
public class PreferenciaController {

    private final PreferenciaService preferenciaService;

    public PreferenciaController(PreferenciaService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping({"/", "/preferencias"})
    public String ok() {
        return "microservicio preferencias ok!";
    }

    @GetMapping("/preferencias/me")
    public PreferenciaResponseDTO obtenerPropias(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return preferenciaService.obtener(requireUser(userId));
    }

    @PutMapping("/preferencias/me")
    public PreferenciaResponseDTO actualizarPropias(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody PreferenciaRequestDTO request) {
        return preferenciaService.actualizar(requireUser(userId), request);
    }

    @GetMapping("/preferencias/me/notificaciones")
    public NotificacionesPreferenciasDTO obtenerNotificaciones(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return preferenciaService.obtenerNotificaciones(requireUser(userId));
    }

    @PutMapping("/preferencias/me/notificaciones")
    public NotificacionesPreferenciasDTO actualizarNotificaciones(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody NotificacionesPreferenciasDTO request) {
        return preferenciaService.actualizarNotificaciones(requireUser(userId), request);
    }

    /**
     * Endpoint interno (Feign desde msvc-notificaciones): clientes con alguno de esos artistas
     * favoritos que además tienen la ciudad del evento.
     */
    @PostMapping("/preferencias/clientes/matching")
    public List<String> clientesInteresados(@RequestBody MatchingClientesRequestDTO request) {
        return preferenciaService.clientesInteresados(request);
    }

    /** Endpoint interno (Feign): preferencias de canal de un cliente, con defaults si no existe. */
    @GetMapping("/preferencias/clientes/{idCliente}/notificaciones")
    public NotificacionesPreferenciasDTO notificacionesDeCliente(@PathVariable String idCliente) {
        return preferenciaService.obtenerNotificaciones(idCliente);
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta X-User-Id");
        }
        return userId;
    }
}
