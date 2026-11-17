package preferencias.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Lectura de catálogo en msvc-eventos: candidatos para recomendaciones y enriquecimiento del
 * historial (snapshot del evento).
 */
@FeignClient(name = "msvc-eventos", url = "${ticketya.uri.eventos:http://localhost:8082}")
public interface EventosClient {

    @GetMapping("/eventos")
    EventosPageDTO listar(
            @RequestParam("estado") String estado,
            @RequestParam(value = "fechaDesde", required = false) String fechaDesde,
            @RequestParam("size") int size,
            @RequestParam("incluirParticipantes") boolean incluirParticipantes);

    @GetMapping("/eventos/{id}")
    EventoDetalleDTO getEvento(@PathVariable("id") String id);

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class EventosPageDTO {
        private List<EventoResumenDTO> content = new ArrayList<>();
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class EventoResumenDTO {
        private String id;
        private String nombre;
        private String tipo;
        private LocalDateTime fechaInicio;
        private LugarDTO lugar;
        private String imagenUrl;
        private BigDecimal precioDesde;
        private boolean destacado;
        private String estado;
        private String idOrganizador;
        private boolean esAltaDemanda;
        private List<String> artistas = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class LugarDTO {
        private String nombre;
        private String ciudad;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class EventoDetalleDTO {
        private String id;
        private String nombre;
        private String tipo;
        private LocalDateTime fechaInicio;
        private String estado;
        private LugarDetalleDTO lugar;
        private BigDecimal precioDesde;
        private boolean destacado;
        private boolean esAltaDemanda;
        private List<ParticipanteDTO> participantes = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class LugarDetalleDTO {
        private String id;
        private String nombre;
        private String direccion;
        private String ciudad;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ParticipanteDTO {
        private String id;
        private String nombre;
        private String tipo;
        private String rol;
        private Integer orden;
    }
}
