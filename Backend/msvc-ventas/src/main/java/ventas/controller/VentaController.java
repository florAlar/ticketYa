package ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ventas", description = "Disponibilidad, reservas, compras, pagos y cupos comerciales.")
@RestController
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
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
        return "microservicio ventas ok!";
    }

    // --- disponibilidad (público vía GW) ---

    /** Batch catálogo: GET /eventos/disponibilidad?ids=a,b,c (antes del path {id}). */
    @Operation(
            summary = "Disponibilidad de varios eventos",
            description = """
                    Batch para catálogo: devuelve la disponibilidad comercial de hasta 50 eventos.
                    Endpoint público a través del gateway.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad por evento",
                    content = @Content(schema = @Schema(implementation = DisponibilidadBatchResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Lista de ids inválida o demasiado grande",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos/disponibilidad")
    public DisponibilidadBatchResponseDTO disponibilidadBatch(
            @Parameter(description = "Ids de evento separados por coma (máx. 50)",
                    example = "d0000000-0000-4000-8000-000000000001,d0000000-0000-4000-8000-000000000002")
            @RequestParam(value = "ids", required = false) List<String> ids) {
        return ventaService.disponibilidadBatch(ids != null ? ids : List.of());
    }

    @Operation(
            summary = "Disponibilidad de un evento",
            description = """
                    Cupos por sector, total disponible y clasificación `nivelCupo`
                    (AGOTADO | ULTIMOS_LUGARES | MENOS_DE_50 | MENOS_DE_100 | OK).
                    Endpoint público a través del gateway.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad del evento",
                    content = @Content(schema = @Schema(implementation = DisponibilidadResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento sin cupos comerciales",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos/{id}/disponibilidad")
    public DisponibilidadResponseDTO disponibilidad(
            @Parameter(description = "UUID del evento", required = true,
                    example = "d0000000-0000-4000-8000-000000000001")
            @PathVariable("id") String idEvento) {
        return ventaService.disponibilidad(idEvento);
    }

    /**
     * Abre stock comercial (cupo_sector) al publicar.
     * Interno: Feign msvc-eventos. También vía GW POST /api/eventos/{id}/cupos (ORG/ADMIN).
     */
    @Operation(
            summary = "Abrir cupos comerciales del evento",
            description = """
                    Crea/asegura el stock por sector al publicar. Idempotente por (idEvento, idSector):
                    si el cupo ya existe no resetea vendido/reservado.
                    Uso principal: Feign desde msvc-eventos; vía gateway requiere ORGANIZADOR o ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cupos abiertos/asegurados",
                    content = @Content(schema = @Schema(implementation = DisponibilidadResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido o sin sectores",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto al abrir el cupo",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/eventos/{id}/cupos")
    public ResponseEntity<DisponibilidadResponseDTO> abrirCupos(
            @Parameter(description = "UUID del evento", required = true) @PathVariable("id") String idEvento,
            @Valid @RequestBody AbrirCuposRequestDTO body) {
        DisponibilidadResponseDTO dto = ventaService.abrirCupos(idEvento, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Devuelve 1+ unidades al disponible (devolución aprobada).
     * Interno: Feign msvc-entradas. GW opcional POST /api/eventos/{id}/cupos/liberacion.
     */
    @Operation(
            summary = "Liberar cupo por devolución",
            description = """
                    Devuelve unidades al disponible del sector (devolución aprobada).
                    Uso principal: Feign desde msvc-entradas; vía gateway requiere ORGANIZADOR o ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cupo liberado",
                    content = @Content(schema = @Schema(implementation = DisponibilidadResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido (idSector/cantidad)",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Sector sin cupo comercial",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/eventos/{id}/cupos/liberacion")
    public DisponibilidadResponseDTO liberarCupo(
            @Parameter(description = "UUID del evento", required = true) @PathVariable("id") String idEvento,
            @Valid @RequestBody LiberarCupoRequestDTO body) {
        return ventaService.liberarCupoDevolucion(idEvento, body);
    }

    // --- reservas ---

    @Operation(
            summary = "Crear reserva temporal",
            description = """
                    Reserva cupo por sector con TTL (10 min por defecto). El cliente sale de
                    X-User-Id. `Idempotency-Key` es opcional: si se repite, devuelve la reserva
                    ya creada para esa key.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada",
                    content = @Content(schema = @Schema(implementation = ReservaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido o sin precio activo",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Cupo insuficiente o límite por cliente superado",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponseDTO> crearReserva(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Clave de idempotencia opcional para reintentos seguros",
                    example = "reserva-2026-03-14-0001")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReservaCreateRequestDTO body) {
        ReservaResponseDTO created = ventaService.crearReserva(requireUser(userId), body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Consultar reserva propia",
            description = "Solo devuelve reservas del usuario autenticado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva",
                    content = @Content(schema = @Schema(implementation = ReservaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente o de otro usuario",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/reservas/{id}")
    public ReservaResponseDTO getReserva(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la reserva", required = true) @PathVariable String id) {
        return ventaService.obtenerReserva(requireUser(userId), id);
    }

    @Operation(
            summary = "Confirmar reserva",
            description = "Marca la intención de compra. Solo reservas propias y activas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada",
                    content = @Content(schema = @Schema(implementation = ReservaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Estado o expiración no permiten confirmar",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/reservas/{id}/confirmacion")
    public ReservaResponseDTO confirmar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la reserva", required = true) @PathVariable String id) {
        return ventaService.confirmarReserva(requireUser(userId), id);
    }

    @Operation(
            summary = "Cancelar reserva",
            description = "Cancela una reserva propia y libera el cupo. No cancelable con compra en curso o aprobada.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva cancelada",
                    content = @Content(schema = @Schema(implementation = ReservaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Compra en curso/aprobada; no cancelable",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/reservas/{id}/cancelacion")
    public ReservaResponseDTO cancelar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la reserva", required = true) @PathVariable String id) {
        return ventaService.cancelarReserva(requireUser(userId), id);
    }

    @Operation(
            summary = "Renovar reserva",
            description = "Extiende el TTL de una reserva propia activa (no es automática ni ilimitada).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva renovada",
                    content = @Content(schema = @Schema(implementation = ReservaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Reserva no activa o expirada",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/reservas/{id}/renovacion")
    public ReservaResponseDTO renovar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la reserva", required = true) @PathVariable String id) {
        return ventaService.renovarReserva(requireUser(userId), id);
    }

    // --- compras ---

    @Operation(
            summary = "Crear compra desde una reserva",
            description = """
                    Convierte una reserva válida en compra. `Idempotency-Key` es **obligatoria**
                    (400 si falta); repetir la key devuelve la compra ya creada.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra creada",
                    content = @Content(schema = @Schema(implementation = CompraResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Falta Idempotency-Key o body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Reserva no activa o ya usada en otra compra",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/compras")
    public ResponseEntity<CompraResponseDTO> crearCompra(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Clave de idempotencia obligatoria", required = true,
                    example = "compra-2026-03-14-0001")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CompraCreateRequestDTO body) {
        CompraResponseDTO created = ventaService.crearCompra(requireUser(userId), body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Consultar compra propia",
            description = "Solo devuelve compras del usuario autenticado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compra",
                    content = @Content(schema = @Schema(implementation = CompraResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Compra inexistente o de otro usuario",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/compras/{id}")
    public CompraResponseDTO getCompra(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la compra", required = true) @PathVariable String id) {
        return ventaService.obtenerCompra(requireUser(userId), id);
    }

    @Operation(
            summary = "Listar compras propias",
            description = "Compras del usuario autenticado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compras del usuario",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CompraResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/compras")
    public List<CompraResponseDTO> listCompras(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ventaService.listarCompras(requireUser(userId));
    }

    /**
     * Interno: ids de clientes con compra APROBADA del evento.
     * Uso principal: Feign desde msvc-notificaciones (fan-out de avisos push).
     */
    @Operation(
            summary = "Clientes con compra aprobada de un evento",
            description = """
                    Uso interno (Feign msvc-notificaciones): ids de clientes a notificar
                    ante una cancelación. No requiere identidad de usuario.
                    """,
            tags = "Ventas",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ids de clientes (puede ser vacío)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "400", description = "idEvento inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/compras/evento/{idEvento}")
    public List<String> listarClientesAprobadosPorEvento(
            @Parameter(description = "UUID del evento", required = true) @PathVariable String idEvento) {
        return ventaService.listarClientesAprobadosPorEvento(idEvento);
    }

    @Operation(
            summary = "Iniciar pago de una compra",
            description = """
                    Pago mock: `resultado` APROBAR (default) o RECHAZAR; `proveedor` opcional
                    (default MOCK_PASARELA). Body opcional. Con APROBAR se dispara la emisión
                    de entradas en msvc-entradas (reintentos + job).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago registrado",
                    content = @Content(schema = @Schema(implementation = PagoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Resultado inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Compra inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "La compra no admite pago en su estado",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/compras/{id}/pagos")
    public ResponseEntity<PagoResponseDTO> pagar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la compra", required = true) @PathVariable String id,
            @RequestBody(required = false) PagoCreateRequestDTO body) {
        PagoResponseDTO pago =
                ventaService.iniciarPago(requireUser(userId), id, body != null ? body : new PagoCreateRequestDTO(null, null));
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @Operation(
            summary = "Listar pagos de una compra",
            description = "Pagos registrados para una compra propia.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagos de la compra",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PagoResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Compra inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/compras/{id}/pagos")
    public List<PagoResponseDTO> listPagos(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la compra", required = true) @PathVariable String id) {
        return ventaService.listarPagos(requireUser(userId), id);
    }

    // --- pagos ---

    @Operation(
            summary = "Reintentar pago rechazado",
            description = "Solo se reintenta un pago RECHAZADO y una compra reintentable. Body opcional.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago reintentado",
                    content = @Content(schema = @Schema(implementation = PagoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Resultado inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Pago inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Pago no RECHAZADO, compra no reintentable o sin reserva activa",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/pagos/{id}/reintentos")
    public PagoResponseDTO reintentar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID del pago", required = true) @PathVariable String id,
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
