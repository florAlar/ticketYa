package preferencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import preferencias.dto.HistorialItemDTO;
import preferencias.dto.PageResponseDTO;
import preferencias.exception.ForbiddenException;
import preferencias.exception.UnauthorizedException;
import preferencias.service.InteraccionService;

@Tag(name = "Historial", description = "Compras registradas del cliente.")
@RestController
public class ClienteController {

    private final InteraccionService interaccionService;

    public ClienteController(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }

    /** Historial de compras del cliente. Dueño o ADMIN. */
    @Operation(summary = "Historial de compras del cliente",
            description = "Dueño (X-User-Id == id) o ADMIN. Paginado, orden fechaCompra desc.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/clientes/{id}/historial")
    public PageResponseDTO<HistorialItemDTO> historial(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        String solicitante = requireUser(userId);
        if (!esAdmin(roles) && !solicitante.equals(id)) {
            throw new ForbiddenException("Solo el dueño del historial o un ADMIN puede consultarlo");
        }
        return interaccionService.historial(id, page, size);
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta X-User-Id");
        }
        return userId;
    }

    private static boolean esAdmin(String roles) {
        if (roles == null || roles.isBlank()) {
            return false;
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch("ADMIN"::equalsIgnoreCase);
    }
}
