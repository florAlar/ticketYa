package preferencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import preferencias.dto.EventoRecomendadoDTO;
import preferencias.exception.UnauthorizedException;
import preferencias.service.RecomendacionService;

@Tag(name = "Recomendaciones",
        description = "Eventos sugeridos por preferencias, historial y co-compra.")
@RestController
public class RecomendacionController {

    private final RecomendacionService recomendacionService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    /** Recomendaciones propias según preferencias e historial (requiere JWT). */
    @Operation(summary = "Recomendaciones propias",
            description = """
                    Híbrido: preferencias explícitas (tipo, ciudad, artistas, rango de precio)
                    + co-compra (“quienes compraron X también compraron Y”) + fallback a próximos
                    eventos con alta demanda. Excluye eventos ya comprados.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/recomendaciones/me")
    public List<EventoRecomendadoDTO> paraMi(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return recomendacionService.paraCliente(requireUser(userId), limit);
    }

    /** Eventos similares a uno dado (público). */
    @Operation(summary = "Eventos similares a uno dado",
            description = """
                    Público. Prioriza eventos comprados por clientes que compraron el evento
                    fuente; si no hay señal, usa mismo artista, tipo y ciudad. Excluye el evento
                    fuente. 404 si el evento no existe.
                    """)
    @GetMapping("/eventos/{id}/recomendaciones")
    public List<EventoRecomendadoDTO> similares(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "6") int limit) {
        return recomendacionService.similares(id, limit);
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta X-User-Id");
        }
        return userId;
    }
}
