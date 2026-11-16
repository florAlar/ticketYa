package notificaciones.clients;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Endpoint interno de msvc-ventas: destinatarios de avisos por evento. */
@FeignClient(name = "ventas", url = "${ticketya.uri.ventas}")
public interface VentasClient {

    @GetMapping("/compras/evento/{idEvento}")
    List<String> clientesAprobadosPorEvento(@PathVariable("idEvento") String idEvento);
}
