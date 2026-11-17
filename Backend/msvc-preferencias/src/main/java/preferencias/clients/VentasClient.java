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

/** Lectura interna de compras aprobadas en msvc-ventas (backfill del historial). */
@FeignClient(name = "msvc-ventas", url = "${ticketya.uri.ventas:http://localhost:8083}")
public interface VentasClient {

    @GetMapping("/internas/compras")
    List<CompraInternaDTO> comprasInternas();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    class CompraInternaDTO {
        private String idCompra;
        private String idCliente;
        private String idEvento;
        private LocalDateTime fechaCompra;
        private BigDecimal total;
        private String moneda;
        private Integer cantidadEntradas;
    }
}
