package ventas.clients;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Lectura de catálogo/detalle en msvc-eventos (precios + estado publicación).
 */
@FeignClient(name = "msvc-eventos", url = "${ticketya.uri.eventos:http://localhost:8082}")
public interface EventosClient {

    @GetMapping("/eventos/{id}")
    EventoDetalleFeignDTO getEvento(@PathVariable("id") String id);

    @Getter
    @Setter
    class EventoDetalleFeignDTO {
        private String id;
        private String nombre;
        private String estado;
        /** Para que un ORGANIZADOR solo consulte los reportes de sus propios eventos. */
        private String idOrganizador;
        private List<SectorFeignDTO> sectores = new ArrayList<>();
    }

    @Getter
    @Setter
    class SectorFeignDTO {
        private String id;
        private String nombre;
        private BigDecimal precio;
        private String moneda;
        /** Sector numerado: se vende por asiento concreto (fila + número). */
        private Boolean esNumerado;
        private Integer filas;
        private Integer asientosPorFila;
    }
}
