package entradas.controller;

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
import entradas.dto.DevolucionCreateRequestDTO;
import entradas.dto.DevolucionResponseDTO;
import entradas.dto.EmitirEntradasRequestDTO;
import entradas.dto.EntradaResponseDTO;
import entradas.dto.QrResponseDTO;
import entradas.dto.ValidacionRegistroDTO;
import entradas.dto.ValidacionRequestDTO;
import entradas.dto.ValidacionResponseDTO;
import entradas.exception.ForbiddenException;
import entradas.exception.UnauthorizedException;
import entradas.service.EntradaService;

/**
 * API E1 entradas (paths internos post StripPrefix /api).
 * Identidad: header X-User-Id / X-User-Role del gateway.
 */
@Tag(name = "Entradas", description = "Entradas, código QR y devoluciones de entrada.")
@RestController
public class EntradaController {

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
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
        return "microservicio entradas ok!";
    }

    /** Emisión post-compra (ventas Feign o cliente autenticado dueño). */
    @Operation(
            summary = "Emitir entradas",
            description = """
                    Crea entradas con código QR a partir de una compra.
                    Llamado por msvc-ventas (Feign) o por el cliente autenticado dueño de la compra.
                    Estado de entrada: VENDIDA.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entradas emitidas",
                    content = @Content(schema = @Schema(implementation = EntradaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido o compra no pagada",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "No es dueño de la compra ni staff",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Compra inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/entradas/emision")
    public ResponseEntity<List<EntradaResponseDTO>> emitir(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody EmitirEntradasRequestDTO body) {
        if (userId != null && !userId.isBlank() && !userId.equals(body.idCliente()) && !isStaff(role)) {
            throw new UnauthorizedException("No puede emitir entradas para otro cliente");
        }
        List<EntradaResponseDTO> created = entradaService.emitir(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Mis entradas (cliente autenticado). */
    @Operation(
            summary = "Listar mis entradas",
            description = "Entradas del cliente autenticado (X-User-Id).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entradas del cliente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EntradaResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/entradas")
    public List<EntradaResponseDTO> misEntradas(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return entradaService.listarMias(requireUser(userId));
    }

    /** Una entrada (dueño o ADMIN). */
    @Operation(
            summary = "Obtener una entrada",
            description = """
                    Entrada propia (cliente dueño) o cualquiera (ADMIN).
                    Devuelve QR solo si la política lo permite (evento no cancelado).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada",
                    content = @Content(schema = @Schema(implementation = EntradaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "No es dueño ni ADMIN",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Entrada inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/entradas/{id}")
    public EntradaResponseDTO una(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID de la entrada", required = true) @PathVariable String id) {
        boolean admin = isAdmin(role);
        String cliente = admin ? (userId != null ? userId.trim() : "") : requireUser(userId);
        return entradaService.obtener(cliente, id, admin);
    }

    /** Código QR de una entrada propia. */
    @Operation(
            summary = "Código QR de una entrada",
            description = """
                    QR de entrada propia. Devuelto solo si el evento está activo (no CANCELADO)
                    y la entrada no está USADA/DEVUELTA/A_REINTEGRAR.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "QR de la entrada",
                    content = @Content(schema = @Schema(implementation = QrResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "No es dueña de la entrada",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Entrada inexistente o sin QR",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/entradas/{id}/qr")
    public QrResponseDTO qr(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la entrada", required = true) @PathVariable String id) {
        return entradaService.qrMia(requireUser(userId), id);
    }

    /** Solicitar devolución de una entrada propia (plazo 72 h desde emisión). */
    @Operation(
            summary = "Solicitar devolución",
            description = """
                    Solicita devolución de una entrada propia VENDIDA dentro del plazo
                    (72 h por defecto desde la emisión).
                    Estado de la devolución: PENDIENTE → cola de admin.
                    Entradas USADA / DEVUELTA / A_REINTEGRAR → 409.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Devolución solicitada",
                    content = @Content(schema = @Schema(implementation = DevolucionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "No es dueña de la entrada",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Entrada inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Entrada no devoluble o plazo vencido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/entradas/{id}/devoluciones")
    public ResponseEntity<DevolucionResponseDTO> solicitarDev(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "UUID de la entrada", required = true) @PathVariable String id,
            @RequestBody(required = false) DevolucionCreateRequestDTO body) {
        DevolucionResponseDTO d = entradaService.solicitarDevolucion(
                requireUser(userId), id, body != null ? body : new DevolucionCreateRequestDTO(null));
        return ResponseEntity.status(HttpStatus.CREATED).body(d);
    }

    /** Una devolución (dueña o ADMIN). */
    @Operation(
            summary = "Obtener una devolución",
            description = "Devolución propia o cualquiera (ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución",
                    content = @Content(schema = @Schema(implementation = DevolucionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Devolución inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/devoluciones/{id}")
    public DevolucionResponseDTO getDev(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID de la devolución", required = true) @PathVariable String id) {
        boolean admin = isAdmin(role);
        return entradaService.obtenerDevolucion(admin ? "" : requireUser(userId), id, admin);
    }

    /** Devoluciones pendientes (ADMIN) o del cliente. */
    @Operation(
            summary = "Listar devoluciones",
            description = """
                    ADMIN: devoluciones PENDIENTE (scope=pendientes/all).
                    Cliente: devoluciones propias.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devoluciones",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DevolucionResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/devoluciones")
    public List<DevolucionResponseDTO> listDev(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "scope: pendientes | all (solo ADMIN)") @RequestParam(required = false) String scope) {
        if (isAdmin(role) && (scope == null || "pendientes".equalsIgnoreCase(scope) || "all".equalsIgnoreCase(scope))) {
            return entradaService.listarDevolucionesPendientes();
        }
        return entradaService.listarDevolucionesCliente(requireUser(userId));
    }

    /** Aprobar devolución (ADMIN) → libera cupo en ventas. */
    @Operation(
            summary = "Aprobar devolución",
            description = """
                    Aprueba una devolución PENDIENTE y libera el cupo en msvc-ventas
                    (POST /eventos/{id}/cupos/liberacion).
                    Estado: APROBADA → entrada DEVUELTA.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución aprobada",
                    content = @Content(schema = @Schema(implementation = DevolucionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Devolución inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "No está PENDIENTE",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/devoluciones/{id}/aprobacion")
    public DevolucionResponseDTO aprobar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID de la devolución", required = true) @PathVariable String id) {
        requireAdmin(role);
        return entradaService.aprobarDevolucion(id);
    }

    /** Rechazar devolución (ADMIN). */
    @Operation(
            summary = "Rechazar devolución",
            description = "Rechaza una devolución PENDIENTE. Estado: RECHAZADA.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución rechazada",
                    content = @Content(schema = @Schema(implementation = DevolucionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Devolución inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "No está PENDIENTE",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/devoluciones/{id}/rechazo")
    public DevolucionResponseDTO rechazar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID de la devolución", required = true) @PathVariable String id) {
        requireAdmin(role);
        return entradaService.rechazarDevolucion(id);
    }

    /** E1-ENT-05: reintegro por cancelación de evento (ADMIN). */
    @Operation(
            summary = "Reintegro por cancelación de evento",
            description = """
                    Marca entradas VENDIDA del evento como A_REINTEGRAR y crea devoluciones
                    EN_CURSO (no entran a cola de admin). Require ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reintegro ejecutado",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/eventos/{idEvento}/reintegro-cancelacion")
    public ResponseEntity<java.util.Map<String, Object>> reintegroCancelacion(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String idEvento) {
        requireAdmin(role);
        int n = entradaService.reintegroPorCancelacionEvento(idEvento);
        return ResponseEntity.ok(java.util.Map.of("idEvento", idEvento, "entradasAfectadas", n));
    }

    /** Control de acceso: el personal valida el QR en la puerta (E2-ACC-01). */
    @Operation(
            summary = "Validar entrada en la puerta",
            description = """
                    Valida el código QR de una entrada al ingresar al evento. Verifica que exista, corresponda
                    al evento y no haya sido usada; si es válida la marca USADA (un QR no puede ingresar dos
                    veces, ni siquiera con escaneos simultáneos). Cada intento sobre una entrada existente queda
                    registrado, también los rechazados.

                    Resultado en el body: VALIDA (200) | YA_USADA | OTRO_EVENTO | NO_VALIDA | EVENTO_CANCELADO (409).
                    Roles: PERSONAL_ACCESO o ADMIN. Simplificación: no hay asignación de eventos por persona.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ingreso aceptado (resultado VALIDA)",
                    content = @Content(schema = @Schema(implementation = ValidacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "codigoQR vacío o inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso (se requiere PERSONAL_ACCESO o ADMIN)",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "No existe una entrada con ese QR",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Ingreso rechazado (YA_USADA, OTRO_EVENTO, NO_VALIDA, EVENTO_CANCELADO)",
                    content = @Content(schema = @Schema(implementation = ValidacionResponseDTO.class)))
    })
    @PostMapping("/eventos/{idEvento}/validaciones")
    public ResponseEntity<ValidacionResponseDTO> validarIngreso(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento en cuya puerta se escanea") @PathVariable String idEvento,
            @Valid @RequestBody ValidacionRequestDTO body) {
        String personal = requireUser(userId);
        if (!isAcceso(role) && !isAdmin(role)) {
            throw new ForbiddenException("Se requiere rol PERSONAL_ACCESO o ADMIN");
        }
        ValidacionResponseDTO res = entradaService.validarIngreso(idEvento, personal, body);
        return ResponseEntity.status(res.isOk() ? HttpStatus.OK : HttpStatus.CONFLICT).body(res);
    }

    /** Intentos de validación de un evento. */
    @Operation(
            summary = "Listar validaciones de un evento",
            description = """
                    Intentos de validación (aceptados y rechazados) de las entradas del evento, el más reciente
                    primero. ADMIN: cualquier evento. ORGANIZADOR: solo los eventos que organiza.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validaciones",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ValidacionRegistroDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "No es ADMIN ni organizador del evento",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos/{idEvento}/validaciones")
    public List<ValidacionRegistroDTO> validaciones(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento") @PathVariable String idEvento) {
        String usuario = requireUser(userId);
        boolean admin = isAdmin(role);
        if (!admin && !isOrganizador(role)) {
            throw new ForbiddenException("Se requiere rol ADMIN u ORGANIZADOR");
        }
        return entradaService.listarValidaciones(idEvento, usuario, admin);
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

    private static boolean isAcceso(String role) {
        return role != null && "PERSONAL_ACCESO".equalsIgnoreCase(role.trim());
    }

    private static boolean isOrganizador(String role) {
        return role != null && "ORGANIZADOR".equalsIgnoreCase(role.trim());
    }

    private static boolean isStaff(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return "ADMIN".equals(r) || "ORGANIZADOR".equals(r) || "SYSTEM".equals(r);
    }
}
