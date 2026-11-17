package preferencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import preferencias.dto.ArtistasRequestDTO;
import preferencias.dto.MatchingClientesRequestDTO;
import preferencias.dto.NotificacionesPreferenciasDTO;
import preferencias.dto.PreferenciaRequestDTO;
import preferencias.dto.PreferenciaResponseDTO;
import preferencias.exception.UnauthorizedException;
import preferencias.service.PreferenciaService;

@Tag(name = "Preferencias", description = "Preferencias del cliente y matching interno.")
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

    @Operation(summary = "Consultar preferencias propias",
            description = "Devuelve defaults si el cliente todavía no tiene documento.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/preferencias/me")
    public PreferenciaResponseDTO obtenerPropias(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return preferenciaService.obtener(requireUser(userId));
    }

    @Operation(summary = "Actualizar preferencias propias",
            description = "Categorías/tipos de evento, ciudades, artistas y rango de precio.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/preferencias/me")
    public PreferenciaResponseDTO actualizarPropias(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody PreferenciaRequestDTO request) {
        return preferenciaService.actualizar(requireUser(userId), request);
    }

    @Operation(summary = "Actualizar artistas favoritos",
            description = "Actualiza solo artistas; no pisa categorías, ciudades ni rango.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/preferencias/me/artistas")
    public PreferenciaResponseDTO actualizarArtistas(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody ArtistasRequestDTO request) {
        return preferenciaService.actualizarArtistas(requireUser(userId), request.getArtistas());
    }

    @Operation(summary = "Consultar preferencias de notificación",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/preferencias/me/notificaciones")
    public NotificacionesPreferenciasDTO obtenerNotificaciones(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return preferenciaService.obtenerNotificaciones(requireUser(userId));
    }

    @Operation(summary = "Actualizar preferencias de notificación",
            security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "Matching interno por artista y ciudad",
            description = "Uso interno (Feign de msvc-notificaciones). No requiere identidad.",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @PostMapping("/preferencias/clientes/matching")
    public List<String> clientesInteresados(@RequestBody MatchingClientesRequestDTO request) {
        return preferenciaService.clientesInteresados(request);
    }

    /** Endpoint interno (Feign): preferencias de canal de un cliente, con defaults si no existe. */
    @Operation(summary = "Preferencias de canal de un cliente (interno)",
            description = "Uso interno (Feign). Devuelve defaults si el cliente no tiene documento.",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
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
