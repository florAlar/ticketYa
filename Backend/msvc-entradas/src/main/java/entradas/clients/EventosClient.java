package entradas.clients;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Lectura de detalle en msvc-eventos para enriquecer DTO de entradas
 * (eventoNombre, lugar, sectorNombre).
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
        private LugarFeignDTO lugar;
        private List<SectorFeignDTO> sectores = new ArrayList<>();
    }

    @Getter
    @Setter
    class LugarFeignDTO {
        private String nombre;
        private String ciudad;
    }

    @Getter
    @Setter
    class SectorFeignDTO {
        private String id;
        private String nombre;
    }
}
