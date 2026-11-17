package notificaciones.controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import notificaciones.dto.NotificacionPageResponseDTO;
import notificaciones.dto.NotificacionRequestDTO;
import notificaciones.dto.NotificacionResponseDTO;
import notificaciones.entity.Notificacion;
import notificaciones.exception.ForbiddenException;
import notificaciones.exception.ResourceNotFoundException;
import notificaciones.exception.UnauthorizedException;
import notificaciones.mapper.NotificacionMapper;
import notificaciones.repository.NotificacionRepository;
import notificaciones.service.NotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificacionController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> ORDEN_CANALES = List.of("PUSH", "EMAIL");

    private final NotificacionService notificacionService;
    private final NotificacionRepository notificacionRepository;

    public NotificacionController(
            NotificacionService notificacionService,
            NotificacionRepository notificacionRepository) {
        this.notificacionService = notificacionService;
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping({"/", "/notificaciones"})
    public String ok() {
        return "microservicio notificaciones ok!";
    }

    /**
     * Bandeja del usuario autenticado: una entrada por mensaje (no por canal). Si el aviso se envió
     * por PUSH y EMAIL se muestra la fila PUSH y {@code canales} informa ambos. {@code noLeidas}
     * alimenta el badge de la campanita y cuenta mensajes, no filas.
     */
    @GetMapping("/notificaciones/me")
    public NotificacionPageResponseDTO listarPropias(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean leida) {
        String cliente = requireUser(userId);
        int pagina = Math.max(0, page);
        int tamano = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(pagina, tamano);

        Page<Notificacion> resultado = leida == null
                ? notificacionRepository.findVisiblesByIdUsuario(cliente, pageable)
                : notificacionRepository.findVisiblesByIdUsuarioAndLeida(cliente, leida, pageable);

        Map<String, List<String>> canales = canalesDe(cliente, resultado.getContent());

        NotificacionPageResponseDTO respuesta = new NotificacionPageResponseDTO();
        respuesta.setContent(resultado.getContent().stream()
                .map(n -> NotificacionMapper.toResponse(n, canales.get(n.getClaveMensaje())))
                .toList());
        respuesta.setPage(resultado.getNumber());
        respuesta.setSize(resultado.getSize());
        respuesta.setTotalElements(resultado.getTotalElements());
        respuesta.setTotalPages(resultado.getTotalPages());
        respuesta.setNoLeidas(notificacionRepository.countVisiblesNoLeidas(cliente));
        return respuesta;
    }

    @GetMapping("/notificaciones/me/{id}")
    public NotificacionResponseDTO detallePropio(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {
        Notificacion n = requirePropia(requireUser(userId), id);
        Map<String, List<String>> canales = canalesDe(n.getIdUsuario(), List.of(n));
        return NotificacionMapper.toResponse(n, canales.get(n.getClaveMensaje()));
    }

    /**
     * Marca como leído el mensaje completo: las filas PUSH y EMAIL que lo componen, para que el
     * badge no quede con una mitad sin leer.
     */
    @PatchMapping("/notificaciones/me/{id}/leida")
    public NotificacionResponseDTO marcarLeida(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {
        String cliente = requireUser(userId);
        Notificacion notificacion = requirePropia(cliente, id);
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);

        Notificacion representante = notificacion;
        List<Notificacion> mensaje = notificacion.getClaveMensaje() == null
                ? List.of()
                : notificacionRepository.findByIdUsuarioAndClaveMensaje(
                        cliente, notificacion.getClaveMensaje());
        if (mensaje.isEmpty()) {
            if (!Boolean.TRUE.equals(notificacion.getLeida())) {
                notificacion.setLeida(Boolean.TRUE);
                notificacion.setFechaLectura(ahora);
                representante = notificacionRepository.save(notificacion);
            }
        } else {
            for (Notificacion hermana : mensaje) {
                if (!Boolean.TRUE.equals(hermana.getLeida())) {
                    hermana.setLeida(Boolean.TRUE);
                    hermana.setFechaLectura(ahora);
                    notificacionRepository.save(hermana);
                }
                if (hermana.getIdNotificacion().equals(id)) {
                    representante = hermana;
                }
            }
        }

        Map<String, List<String>> canales = canalesDe(cliente, List.of(representante));
        return NotificacionMapper.toResponse(
                representante, canales.get(representante.getClaveMensaje()));
    }

    /** Estado de envío para soporte/administración. */
    @GetMapping("/notificaciones/{id}/estado")
    public NotificacionResponseDTO estado(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id) {
        requireAdmin(role);
        Notificacion n = notificacionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada: " + id));
        return NotificacionMapper.toResponse(n);
    }

    /** Alta administrativa; queda PENDIENTE para que el job de envío la procese. */
    @PostMapping("/notificaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificacionResponseDTO crear(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody NotificacionRequestDTO request) {
        requireAdmin(role);
        return NotificacionMapper.toResponse(notificacionService.crearAdministrativa(request));
    }

    private Map<String, List<String>> canalesDe(
            String idUsuario, List<Notificacion> notificaciones) {
        List<String> claves = notificaciones.stream()
                .map(Notificacion::getClaveMensaje)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, List<String>> canales = new HashMap<>();
        if (claves.isEmpty()) {
            return canales;
        }
        for (Object[] fila : notificacionRepository.findCanalesPorClaves(idUsuario, claves)) {
            String clave = (String) fila[0];
            String canal = (String) fila[1];
            if (canal != null) {
                canales.computeIfAbsent(clave, k -> new ArrayList<>()).add(canal);
            }
        }
        canales.values().forEach(lista -> lista.sort(Comparator.comparingInt(
                NotificacionController::ordenCanal)));
        return canales;
    }

    private static int ordenCanal(String canal) {
        int indice = ORDEN_CANALES.indexOf(canal);
        return indice < 0 ? Integer.MAX_VALUE : indice;
    }

    private Notificacion requirePropia(String idCliente, String id) {
        return notificacionRepository
                .findByIdNotificacionAndIdUsuario(id, idCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación no encontrada: " + id));
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta X-User-Id");
        }
        return userId;
    }

    private static void requireAdmin(String role) {
        if (role == null || !role.toUpperCase(Locale.ROOT).contains("ADMIN")) {
            throw new ForbiddenException("Requiere rol ADMIN");
        }
    }
}
