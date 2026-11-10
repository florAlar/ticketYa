package ventas.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;

/** Tests de jobs de programación. */
class JobsTest {

    @Test
    void emisionPendienteJob_seInstancia() {
        EmisionEntradasService service = new EmisionEntradasService(
                null, null, null, null, null, 3, 0, 20);
        assertNotNull(new EmisionPendienteJob(service));
    }

    @Test
    void emisionPendienteJob_tick_conPendientes_loggea() {
        EmisionEntradasService service = mock(EmisionEntradasService.class);
        when(service.reprocesarPendientes()).thenReturn(1);
        new EmisionPendienteJob(service).tick();
    }

    @Test
    void reservaExpirationJob_seInstancia() {
        VentaService ventaService = new VentaServiceImpl(
                null, null, null, null, null, null, null, null, null, null,
                10L, 6, 24L);
        assertNotNull(new ReservaExpirationJob(ventaService));
    }

    @Test
    void reservaExpirationJob_tick_conVencidas_loggea() {
        VentaService ventaService = mock(VentaService.class);
        when(ventaService.expirarReservasVencidas()).thenReturn(1);
        new ReservaExpirationJob(ventaService).tick();
    }
}
