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

@Tag(name = "Eventos", description = "Catálogo, detalle y gestión del ciclo de vida del evento.")
@RestController
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Smoke infra — no es API de negocio. */
    @Operation(
            summary = "Smoke del microservicio",
            description = "Chequeo de infraestructura. No forma parte del contrato de negocio.",
            tags = "Smoke",
            extensions = @Extension(name = "x-internal",
                    properties = @ExtensionProperty(name = "value", value = "true")))
    @ApiResponse(responseCode = "200", description = "Texto de estado")
    @GetMapping("/")
    public String ok() {
        return "microservicio eventos ok!";
    }

    /**
         * Catálogo / listado.
         * Default: PUBLICADO + AGOTADO (visibles en home/catálogo).
         * Exacto: estado=PUBLICADO|AGOTADO|…. Gestión: estado=TODOS|ALL.
         */
    @Operation(
            summary = "Buscar eventos",
            description = """
                    Catálogo paginado. Sin `estado` devuelve los visibles (PUBLICADO + AGOTADO).
                    `estado` exacto filtra por ese estado; `TODOS`/`ALL`/`*` devuelve todos
                    (uso de gestión). Endpoint público a través del gateway.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de eventos"),
            @ApiResponse(responseCode = "400", description = "Filtro u orden inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos")
        public PageResponseDTO<EventoResponseDTO> listar(
                @Parameter(description = "Tipo de evento", example = "RECITAL")
                @RequestParam(required = false) String tipo,
                @Parameter(description = "Ciudad del lugar", example = "Buenos Aires")
                @RequestParam(required = false) String ciudad,
                @Parameter(description = "UUID del organizador", example = "c0000000-0000-4000-8000-000000000002")
                @RequestParam(required = false) String organizador,
                @Parameter(description = "UUID de un participante (artista/banda)", example = "d0000000-0000-4000-8000-000000000001")
                @RequestParam(required = false) String participante,
                @Parameter(description = "Estado exacto o TODOS/ALL/*", example = "PUBLICADO")
                @RequestParam(required = false) String estado,
                @Parameter(description = "Fecha desde (ISO yyyy-MM-dd)", example = "2026-01-01")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                @Parameter(description = "Fecha hasta (ISO yyyy-MM-dd)", example = "2026-12-31")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                @Parameter(description = "Orden: fecha | precio | nombre", example = "fecha")
                @RequestParam(required = false, defaultValue = "fecha") String orden,
                @Parameter(description = "Página (0-based)", example = "0")
                @RequestParam(required = false, defaultValue = "0") int page,
                @Parameter(description = "Tamaño de página", example = "20")
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

    @Operation(
            summary = "Obtener detalle de un evento",
            description = """
                    Lugar completo, sectores con precio vigente, participantes y datos de
                    publicación. Endpoint público a través del gateway.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del evento",
                    content = @Content(schema = @Schema(implementation = EventoDetalleResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos/{id}")
    public EventoDetalleResponseDTO detalle(
            @Parameter(description = "UUID del evento", required = true,
                    example = "e0000000-0000-4000-8000-000000000001")
            @PathVariable String id) {
        return eventoService.obtenerPorId(id);
    }

    /** POST /eventos — crear borrador (ORGANIZADOR/ADMIN). Body ≈ form FE gestión. */
    @Operation(
            summary = "Crear evento (borrador)",
            description = """
                    Alta de evento. Si el body no trae `estado`, queda en BORRADOR; con
                    `PUBLICADO` se publica al final del alta (valida fechas, lugar y precios).
                    Requiere rol ORGANIZADOR o ADMIN (gateway + revalidación local).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado",
                    content = @Content(schema = @Schema(implementation = EventoDetalleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido o falta fecha para publicar",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id (no pasó por el gateway)",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de publicación o stock en ventas",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/eventos")
    public ResponseEntity<EventoDetalleResponseDTO> crear(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody EventoRequestDTO body) {
        requireOrgOrAdmin(role);
        EventoDetalleResponseDTO created = eventoService.crear(body, requireUser(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Editar evento",
            description = """
                    Actualiza un evento editable. Reemplaza lugar/sectores según el body.
                    Requiere ser dueño del evento o ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento actualizado",
                    content = @Content(schema = @Schema(implementation = EventoDetalleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente o no es dueño",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "El estado del evento no permite edición",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PutMapping("/eventos/{id}")
    public EventoDetalleResponseDTO actualizar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id,
            @RequestBody EventoRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.actualizar(id, body, requireUser(userId), role);
    }

    @Operation(
            summary = "Eliminar evento en borrador",
            description = "Solo se puede eliminar un evento en estado BORRADOR. Dueño o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Evento eliminado (sin cuerpo)"),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente o no es dueño",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "El evento no está en BORRADOR",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @DeleteMapping("/eventos/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id) {
        requireOrgOrAdmin(role);
        eventoService.eliminarBorrador(id, requireUser(userId), role);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Publicar evento",
            description = """
                    Valida nombre, fecha, lugar y al menos un sector con precio; abre el stock
                    en msvc-ventas y pasa a PUBLICADO. Si la apertura de cupos falla, la
                    publicación falla (no queda estado parcial). Dueño o ADMIN.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento publicado",
                    content = @Content(schema = @Schema(implementation = EventoDetalleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente o no es dueño",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Publicación inválida o fallo al abrir cupos",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PostMapping("/eventos/{id}/publicacion")
    public EventoDetalleResponseDTO publicar(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id) {
        requireOrgOrAdmin(role);
        return eventoService.publicar(id, requireUser(userId), role);
    }

    @Operation(
            summary = "Cambiar estado del evento",
            description = """
                    Transiciones de estado (SUSPENDIDO, CANCELADO, AGOTADO, FINALIZADO, …).
                    Pasar a PUBLICADO requiere rol ADMIN; desde BORRADOR solo se puede publicar.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(implementation = EventoDetalleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Estado ausente o inválido",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id o rol",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Sin permiso para esa transición",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Transición no permitida",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PatchMapping("/eventos/{id}/estado")
    public EventoDetalleResponseDTO patchEstado(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id,
            @RequestBody EventoEstadoRequestDTO body) {
        String estado = body != null ? body.getEstado() : null;
        return eventoService.patchEstado(id, estado, userId, role);
    }

    @Operation(
            summary = "Reprogramar evento",
            description = "Cambia fechaInicio/fechaFin. No permitido en estados no editables. Dueño o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fechas actualizadas",
                    content = @Content(schema = @Schema(implementation = EventoDetalleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Faltan fechas",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente o no es dueño",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "El estado del evento no permite reprogramar",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PatchMapping("/eventos/{id}/fecha")
    public EventoDetalleResponseDTO patchFecha(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id,
            @RequestBody EventoFechaRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.patchFecha(id, body, requireUser(userId), role);
    }

    @Operation(
            summary = "Listar sectores del evento con precio",
            description = "Sectores del lugar con el precio vigente del evento (si existe). Requiere JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sectores con precio",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SectorEventoDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Evento inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/eventos/{id}/sectores")
    public List<SectorEventoDTO> sectoresEvento(
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id) {
        return eventoService.listarSectoresEvento(id);
    }

    @Operation(
            summary = "Definir precio de un sector",
            description = "Alta o actualización del precio del sector para el evento. Dueño o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Precio vigente",
                    content = @Content(schema = @Schema(implementation = SectorEventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Precio ausente o sector ajeno al lugar",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente o no es dueño",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "404", description = "Evento, sector o lugar inexistente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de estado del evento",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @PutMapping("/eventos/{id}/sectores/{sectorId}/precio")
    public SectorEventoDTO precioSector(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Parameter(description = "UUID del evento", required = true) @PathVariable String id,
            @Parameter(description = "UUID del sector", required = true) @PathVariable String sectorId,
            @RequestBody PrecioSectorRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.upsertPrecioSector(id, sectorId, body, requireUser(userId), role);
    }

    /** Listado gestión ORGANIZADOR. */
    @Operation(
            summary = "Listar mis eventos",
            description = "Eventos del organizador autenticado (todos los estados). Requiere ORGANIZADOR o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos del organizador",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventoResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta X-User-Id",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    })
    @GetMapping("/organizadores/me/eventos")
    public List<EventoResponseDTO> misEventos(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true)
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
