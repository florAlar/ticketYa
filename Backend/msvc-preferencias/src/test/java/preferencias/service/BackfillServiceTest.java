package preferencias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import preferencias.clients.VentasClient;
import preferencias.clients.VentasClient.CompraInternaDTO;
import preferencias.dto.BackfillResultDTO;

@ExtendWith(MockitoExtension.class)
class BackfillServiceTest {

    @Mock
    private VentasClient ventasClient;

    @Mock
    private InteraccionService interaccionService;

    @Test
    void backfill_cuentaInsertadasYActualizadas() {
        CompraInternaDTO nueva = compra("c-1", "cli-1", "ev-1");
        CompraInternaDTO existente = compra("c-2", "cli-2", "ev-2");
        when(ventasClient.comprasInternas()).thenReturn(List.of(nueva, existente));
        when(interaccionService.existeCompra("c-1")).thenReturn(false);
        when(interaccionService.existeCompra("c-2")).thenReturn(true);

        BackfillResultDTO result = new BackfillService(ventasClient, interaccionService).ejecutar();

        assertEquals(2, result.getProcesadas());
        assertEquals(1, result.getInsertadas());
        assertEquals(1, result.getActualizadas());
        assertEquals(0, result.getErrores());
        verify(interaccionService).registrarCompra(
                eq("c-1"), eq("cli-1"), eq("ev-1"), any(BigDecimal.class), eq(1), any(Instant.class));
    }

    @Test
    void backfill_siVentasFalla_devuelveErrorSinRomper() {
        when(ventasClient.comprasInternas()).thenThrow(new RuntimeException("timeout"));

        BackfillResultDTO result = new BackfillService(ventasClient, interaccionService).ejecutar();

        assertEquals(0, result.getProcesadas());
        assertEquals(1, result.getDetalleErrores().size());
        verify(interaccionService, never()).registrarCompra(any(), any(), any(), any(), any(), any());
    }

    @Test
    void backfill_compraSinIdCuentaError() {
        CompraInternaDTO invalida = compra(null, "cli-1", "ev-1");
        when(ventasClient.comprasInternas()).thenReturn(List.of(invalida));

        BackfillResultDTO result = new BackfillService(ventasClient, interaccionService).ejecutar();

        assertEquals(0, result.getProcesadas());
        assertEquals(1, result.getErrores());
    }

    private static CompraInternaDTO compra(String idCompra, String cliente, String evento) {
        CompraInternaDTO dto = new CompraInternaDTO();
        dto.setIdCompra(idCompra);
        dto.setIdCliente(cliente);
        dto.setIdEvento(evento);
        dto.setFechaCompra(LocalDateTime.of(2026, 10, 1, 14, 5));
        dto.setTotal(new BigDecimal("95000"));
        dto.setCantidadEntradas(1);
        return dto;
    }
}
