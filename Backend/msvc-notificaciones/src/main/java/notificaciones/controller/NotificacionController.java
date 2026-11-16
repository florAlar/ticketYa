package notificaciones.controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
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

    /** Bandeja del usuario autenticado. {@code noLeidas} alimenta el badge de la campanita. */
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
                ? notificacionRepository.findByIdUsuarioOrderByFechaCreacionDesc(cliente, pageable)
                : notificacionRepository.findByIdUsuarioAndLeidaOrderByFechaCreacionDesc(
                        cliente, leida, pageable);

        NotificacionPageResponseDTO respuesta = new NotificacionPageResponseDTO();
        respuesta.setContent(resultado.getContent().stream()
                .map(NotificacionMapper::toResponse)
                .toList());
        respuesta.setPage(resultado.getNumber());
        respuesta.setSize(resultado.getSize());
        respuesta.setTotalElements(resultado.getTotalElements());
        respuesta.setTotalPages(resultado.getTotalPages());
        respuesta.setNoLeidas(
                notificacionRepository.countByIdUsuarioAndLeidaFalse(cliente));
        return respuesta;
    }

    @GetMapping("/notificaciones/me/{id}")
    public NotificacionResponseDTO detallePropio(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {
        return NotificacionMapper.toResponse(requirePropia(requireUser(userId), id));
    }

    @PatchMapping("/notificaciones/me/{id}/leida")
    public NotificacionResponseDTO marcarLeida(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {
        Notificacion n = requirePropia(requireUser(userId), id);
        if (!Boolean.TRUE.equals(n.getLeida())) {
            n.setLeida(Boolean.TRUE);
            n.setFechaLectura(LocalDateTime.now(ZoneOffset.UTC));
            notificacionRepository.save(n);
        }
        return NotificacionMapper.toResponse(n);
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
