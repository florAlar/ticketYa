package eventos.controller;

import eventos.dto.EventoDetalleResponseDTO;
import eventos.dto.EventoResponseDTO;
import eventos.dto.PageResponseDTO;
import eventos.service.EventoService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Smoke infra — no es API de negocio. */
    @GetMapping("/")
    public String ok() {
        return "microservicio eventos ok!";
    }

    /**
     * Catálogo público.
     * GET /eventos?tipo&ciudad&organizador&estado&fechaDesde&fechaHasta&orden&page&size
     * Default estado=PUBLICADO.
     */
    @GetMapping("/eventos")
    public PageResponseDTO<EventoResponseDTO> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String organizador,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "fecha") String orden,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return eventoService.buscar(
                tipo, ciudad, organizador, estado, fechaDesde, fechaHasta, orden, page, size);
    }

    /** Detalle. GET /eventos/{id} */
    @GetMapping("/eventos/{id}")
    public EventoDetalleResponseDTO detalle(@PathVariable String id) {
        return eventoService.obtenerPorId(id);
    }
}
