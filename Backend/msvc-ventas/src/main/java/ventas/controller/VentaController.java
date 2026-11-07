package ventas.controller;

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
import ventas.dto.AbrirCuposRequestDTO;
import ventas.dto.LiberarCupoRequestDTO;
import ventas.dto.CompraCreateRequestDTO;
import ventas.dto.CompraResponseDTO;
import ventas.dto.DisponibilidadBatchResponseDTO;
import ventas.dto.DisponibilidadResponseDTO;
import ventas.dto.PagoCreateRequestDTO;
import ventas.dto.PagoResponseDTO;
import ventas.dto.ReservaCreateRequestDTO;
import ventas.dto.ReservaResponseDTO;
import ventas.exception.UnauthorizedException;
import ventas.service.VentaService;

/**
 * API E1 ventas (paths internos post StripPrefix /api).
 * Identidad: header X-User-Id del gateway.
 */
@RestController
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    /** Smoke infra. */
    @GetMapping("/")
    public String ok() {
        return "microservicio ventas ok!";
    }

    // --- disponibilidad (público vía GW) ---

        /** Batch catálogo: GET /eventos/disponibilidad?ids=a,b,c (antes del path {id}). */
        @GetMapping("/eventos/disponibilidad")
        public DisponibilidadBatchResponseDTO disponibilidadBatch(
                @RequestParam(value = "ids", required = false) List<String> ids) {
            return ventaService.disponibilidadBatch(ids != null ? ids : List.of());
        }

        @GetMapping("/eventos/{id}/disponibilidad")
        public DisponibilidadResponseDTO disponibilidad(@PathVariable("id") String idEvento) {
            return ventaService.disponibilidad(idEvento);
        }

        /**
         * Abre stock comercial (cupo_sector) al publicar.
         * Interno: Feign msvc-eventos. También vía GW POST /api/eventos/{id}/cupos (ORG/ADMIN).
         */
        @PostMapping("/eventos/{id}/cupos")
                public ResponseEntity<DisponibilidadResponseDTO> abrirCupos(
                        @PathVariable("id") String idEvento, @Valid @RequestBody AbrirCuposRequestDTO body) {
                    DisponibilidadResponseDTO dto = ventaService.abrirCupos(idEvento, body);
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                }

                /**
                 * Devuelve 1+ unidades al disponible (devolución aprobada).
                 * Interno: Feign msvc-entradas. GW opcional POST /api/eventos/{id}/cupos/liberacion.
                 */
                @PostMapping("/eventos/{id}/cupos/liberacion")
                public DisponibilidadResponseDTO liberarCupo(
                        @PathVariable("id") String idEvento, @Valid @RequestBody LiberarCupoRequestDTO body) {
                    return ventaService.liberarCupoDevolucion(idEvento, body);
                }

                // --- reservas ---

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponseDTO> crearReserva(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReservaCreateRequestDTO body) {
        ReservaResponseDTO created = ventaService.crearReserva(requireUser(userId), body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/reservas/{id}")
    public ReservaResponseDTO getReserva(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return ventaService.obtenerReserva(requireUser(userId), id);
    }

    @PostMapping("/reservas/{id}/confirmacion")
    public ReservaResponseDTO confirmar(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return ventaService.confirmarReserva(requireUser(userId), id);
    }

    @PostMapping("/reservas/{id}/cancelacion")
    public ReservaResponseDTO cancelar(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return ventaService.cancelarReserva(requireUser(userId), id);
    }

    @PostMapping("/reservas/{id}/renovacion")
    public ReservaResponseDTO renovar(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return ventaService.renovarReserva(requireUser(userId), id);
    }

    // --- compras ---

    @PostMapping("/compras")
    public ResponseEntity<CompraResponseDTO> crearCompra(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CompraCreateRequestDTO body) {
        CompraResponseDTO created = ventaService.crearCompra(requireUser(userId), body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/compras/{id}")
    public CompraResponseDTO getCompra(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return ventaService.obtenerCompra(requireUser(userId), id);
    }

    @GetMapping("/compras")
    public List<CompraResponseDTO> listCompras(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ventaService.listarCompras(requireUser(userId));
    }

    @PostMapping("/compras/{id}/pagos")
    public ResponseEntity<PagoResponseDTO> pagar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id,
            @RequestBody(required = false) PagoCreateRequestDTO body) {
        PagoResponseDTO pago =
                ventaService.iniciarPago(requireUser(userId), id, body != null ? body : new PagoCreateRequestDTO(null, null));
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @GetMapping("/compras/{id}/pagos")
    public List<PagoResponseDTO> listPagos(
            @RequestHeader(value = "X-User-Id", required = false) String userId, @PathVariable String id) {
        return ventaService.listarPagos(requireUser(userId), id);
    }

    // --- pagos ---

    @PostMapping("/pagos/{id}/reintentos")
    public PagoResponseDTO reintentar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id,
            @RequestBody(required = false) PagoCreateRequestDTO body) {
        return ventaService.reintentarPago(
                requireUser(userId), id, body != null ? body : new PagoCreateRequestDTO(null, null));
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta identidad (X-User-Id). ¿Pasó por el gateway con JWT?");
        }
        return userId.trim();
    }
}
