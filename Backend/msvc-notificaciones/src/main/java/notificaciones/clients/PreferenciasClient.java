package notificaciones.clients;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Integración con msvc-preferencias (Mongo) para canal y matching artista + ciudad. */
@FeignClient(name = "preferencias", url = "${ticketya.uri.preferencias:http://localhost:8085}")
public interface PreferenciasClient {

    /** Clientes con alguno de esos artistas y esa ciudad entre sus favoritos. */
    @PostMapping("/preferencias/clientes/matching")
    List<String> clientesInteresados(@RequestBody MatchingClientesRequest request);

    /** Preferencias de canal del cliente. Si el cliente no existe, devuelve los defaults. */
    @GetMapping("/preferencias/clientes/{idCliente}/notificaciones")
    PreferenciasNotificaciones preferenciasNotificaciones(
            @PathVariable("idCliente") String idCliente);

    record MatchingClientesRequest(String ciudad, List<String> artistas) {}

    record PreferenciasNotificaciones(boolean push, boolean email, List<String> tipos) {

        public boolean permiteCanal(String canal) {
            if (canal == null) {
                return true;
            }
            return switch (canal.toUpperCase()) {
                case "PUSH" -> push;
                case "EMAIL" -> email;
                default -> true;
            };
        }

        public boolean permiteTipo(String tipo) {
            return tipos == null || tipos.isEmpty() || (tipo != null && tipos.contains(tipo));
        }
    }
}
