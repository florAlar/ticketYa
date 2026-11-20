package eventos.clients;

import eventos.config.FeignInternalTokenConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Apertura de cupo comercial en msvc-ventas al publicar y sincronización de la fila virtual.
 * Paths internos (sin /api): POST /eventos/{id}/cupos y POST /internas/fila/{id}/invalidacion.
 */
@FeignClient(name = "msvc-ventas", url = "${ticketya.uri.ventas:http://localhost:8083}",
        configuration = FeignInternalTokenConfig.class)
public interface VentasClient {

    @PostMapping("/eventos/{idEvento}/cupos")
    Object abrirCupos(@PathVariable("idEvento") String idEvento, @RequestBody AbrirCuposBody body);

    /** Descarta la caché del flag de alta demanda/estado al publicar, suspender o cambiar el flag. */
    @PostMapping("/internas/fila/{idEvento}/invalidacion")
    void invalidarCacheFila(@PathVariable("idEvento") String idEvento);

    class AbrirCuposBody {
        private List<Item> sectores = new ArrayList<>();

        public List<Item> getSectores() {
            return sectores;
        }

        public void setSectores(List<Item> sectores) {
            this.sectores = sectores != null ? sectores : new ArrayList<>();
        }

        public static class Item {
            private String idSector;
            private Integer capacidadTotal;

            public Item() {}

            public Item(String idSector, Integer capacidadTotal) {
                this.idSector = idSector;
                this.capacidadTotal = capacidadTotal;
            }

            public String getIdSector() {
                return idSector;
            }

            public void setIdSector(String idSector) {
                this.idSector = idSector;
            }

            public Integer getCapacidadTotal() {
                return capacidadTotal;
            }

            public void setCapacidadTotal(Integer capacidadTotal) {
                this.capacidadTotal = capacidadTotal;
            }
        }
    }
}
