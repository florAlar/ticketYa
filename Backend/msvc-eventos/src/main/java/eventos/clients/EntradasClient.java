package eventos.clients;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Reintegro por cancelación (E1-ENT-05). Best-effort desde patch estado CANCELADO.
 * El endpoint de entradas exige rol ADMIN; servicio→servicio manda header interno.
 */
@FeignClient(name = "msvc-entradas", url = "${ticketya.uri.entradas:http://localhost:8084}")
public interface EntradasClient {

    @PostMapping("/eventos/{idEvento}/reintegro-cancelacion")
    Map<String, Object> reintegroCancelacion(
            @PathVariable("idEvento") String idEvento,
            @RequestHeader("X-User-Role") String role);
}
