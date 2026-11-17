package preferencias.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import preferencias.clients.VentasClient;
import preferencias.clients.VentasClient.CompraInternaDTO;
import preferencias.dto.BackfillResultDTO;

/**
 * Importa las compras aprobadas que ya existen en msvc-ventas al historial de preferencias.
 * Es idempotente: el upsert se hace por {@code idCompra}.
 */
@Service
public class BackfillService {

    private static final Logger log = LoggerFactory.getLogger(BackfillService.class);

    private final VentasClient ventasClient;
    private final InteraccionService interaccionService;

    public BackfillService(VentasClient ventasClient, InteraccionService interaccionService) {
        this.ventasClient = ventasClient;
        this.interaccionService = interaccionService;
    }

    public BackfillResultDTO ejecutar() {
        BackfillResultDTO result = new BackfillResultDTO();
        List<CompraInternaDTO> compras;
        try {
            compras = ventasClient.comprasInternas();
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar las compras de msvc-ventas: {}", ex.getMessage());
            result.getDetalleErrores().add("msvc-ventas: " + ex.getMessage());
            return result;
        }
        if (compras == null || compras.isEmpty()) {
            return result;
        }

        for (CompraInternaDTO compra : compras) {
            if (compra == null || compra.getIdCompra() == null || compra.getIdCompra().isBlank()) {
                result.setErrores(result.getErrores() + 1);
                result.getDetalleErrores().add("compra sin idCompra");
                continue;
            }
            result.setProcesadas(result.getProcesadas() + 1);
            try {
                boolean existia = interaccionService.existeCompra(compra.getIdCompra());
                interaccionService.registrarCompra(
                        compra.getIdCompra().trim(),
                        compra.getIdCliente(),
                        compra.getIdEvento(),
                        compra.getTotal(),
                        compra.getCantidadEntradas(),
                        toInstant(compra.getFechaCompra()));
                if (existia) {
                    result.setActualizadas(result.getActualizadas() + 1);
                } else {
                    result.setInsertadas(result.getInsertadas() + 1);
                }
            } catch (RuntimeException ex) {
                result.setErrores(result.getErrores() + 1);
                result.getDetalleErrores().add(compra.getIdCompra() + ": " + ex.getMessage());
                log.warn("Error en backfill de compra {}: {}",
                        compra.getIdCompra(), ex.getMessage());
            }
        }
        log.info("Backfill preferencias: procesadas={} insertadas={} actualizadas={} errores={}",
                result.getProcesadas(), result.getInsertadas(),
                result.getActualizadas(), result.getErrores());
        return result;
    }

    private static Instant toInstant(LocalDateTime fecha) {
        return fecha == null ? Instant.now() : fecha.toInstant(ZoneOffset.UTC);
    }
}
