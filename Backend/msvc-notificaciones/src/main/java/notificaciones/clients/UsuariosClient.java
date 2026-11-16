package notificaciones.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Lectura del email del destinatario en msvc-usuarios (endpoint interno/permitAll). */
@FeignClient(name = "usuarios", url = "${ticketya.uri.usuarios:http://localhost:8081}")
public interface UsuariosClient {

    @GetMapping("/usuarios/{id}")
    UsuarioEmailDTO porId(@PathVariable("id") String id);

    record UsuarioEmailDTO(String id, String email) {}
}
