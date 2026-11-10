package entradas.clients;

import entradas.dto.AsientoRefDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Liberación de cupo comercial en msvc-ventas al aprobar devolución.
 * Path interno (sin /api): POST /eventos/{id}/cupos/liberacion
 */
@FeignClient(name = "msvc-ventas", url = "${ticketya.uri.ventas:http://localhost:8083}")
public interface VentasClient {

    @PostMapping("/eventos/{idEvento}/cupos/liberacion")
    Object liberarCupo(
            @PathVariable("idEvento") String idEvento, @RequestBody LiberarCupoBody body);

    class LiberarCupoBody {
        private String idSector;
        private Integer cantidad = 1;

        public LiberarCupoBody() {}

        public LiberarCupoBody(String idSector, Integer cantidad) {
            this.idSector = idSector;
            this.cantidad = cantidad;
        }

        /** Sector numerado: la entrada devuelta libera su asiento concreto (reemplaza a cantidad). */
        public LiberarCupoBody(String idSector, List<AsientoRefDTO> asientos) {
            this.idSector = idSector;
            this.cantidad = asientos != null ? asientos.size() : 1;
            this.asientos = asientos;
        }

        private List<AsientoRefDTO> asientos;

        public List<AsientoRefDTO> getAsientos() {
            return asientos;
        }

        public void setAsientos(List<AsientoRefDTO> asientos) {
            this.asientos = asientos;
        }

        public String getIdSector() {
            return idSector;
        }

        public void setIdSector(String idSector) {
            this.idSector = idSector;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }
}
