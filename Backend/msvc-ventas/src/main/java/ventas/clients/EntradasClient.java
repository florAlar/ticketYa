package ventas.clients;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "msvc-entradas", url = "${ticketya.uri.entradas:http://localhost:8084}")
public interface EntradasClient {

    @PostMapping("/entradas/emision")
    List<Object> emitir(@RequestBody EmitirRequest body);

    record EmitirRequest(String idCompra, String idCliente, String idEvento, List<Item> items) {
        public record Item(String idSector, Integer cantidad, BigDecimal precioUnitario) {}
    }
}
