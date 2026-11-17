package preferencias.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import preferencias.document.InteraccionEvento;
import preferencias.dto.HistorialItemDTO;
import preferencias.dto.PageResponseDTO;
import preferencias.messaging.CompraConfirmadaDTO;

public interface InteraccionService {

    /** Ingesta idempotente de una compra confirmada (RabbitMQ). */
    void registrarCompra(CompraConfirmadaDTO compra);

    /** Upsert idempotente usado por la ingesta y por el backfill. */
    InteraccionEvento registrarCompra(
            String idCompra,
            String idCliente,
            String idEvento,
            BigDecimal monto,
            Integer cantidadEntradas,
            Instant fechaCompra);

    PageResponseDTO<HistorialItemDTO> historial(String idCliente, int page, int size);

    boolean existeCompra(String idCompra);

    List<InteraccionEvento> comprasDeCliente(String idCliente);

    List<InteraccionEvento> comprasDeEvento(String idEvento);

    List<InteraccionEvento> comprasDeEventos(Collection<String> idEventos);

    List<InteraccionEvento> comprasDeClientes(Collection<String> idClientes);
}
