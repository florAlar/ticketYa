package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ventas.dto.FilaEstadoResponseDTO;
import ventas.dto.FilaHabilitacionResponseDTO;
import ventas.dto.FilaPosicionResponseDTO;
import ventas.exception.FilaException;
import ventas.exception.ForbiddenException;
import ventas.redis.FilaRedisStore;

/** FIL-01: lógica de ingreso/posición/estado con store y caché mockeados. */
@ExtendWith(MockitoExtension.class)
class FilaServiceTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";
    private static final String USER = "b0000000-0000-4000-8000-000000000001";

    @Mock
    private FilaRedisStore store;

    @Mock
    private FilaEventoCache eventos;

    @InjectMocks
    private FilaServiceImpl service;

    private static FilaEventoCache.Info info() {
        return new FilaEventoCache.Info(EVENTO, "PUBLICADO", true);
    }

    @Test
    void ingresoPrimeraVez_devuelvePosicionYDlante() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.ingresar(EVENTO, USER)).thenReturn(new FilaRedisStore.Ingreso(57, true, 50));

        FilaService.IngresoResultado r = service.ingresar(EVENTO, USER);

        assertTrue(r.creado());
        assertEquals(57, r.respuesta().posicion());
        assertEquals(50, r.respuesta().posicionHabilitadaHasta());
        assertEquals(7, r.respuesta().delante());
        assertEquals("EN_ESPERA", r.respuesta().estado());
    }

    @Test
    void reingreso_devuelveMismaPosicionYNoCreado() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.ingresar(EVENTO, USER)).thenReturn(new FilaRedisStore.Ingreso(57, false, 50));

        FilaService.IngresoResultado r = service.ingresar(EVENTO, USER);

        assertFalse(r.creado());
        assertEquals(57, r.respuesta().posicion());
    }

    @Test
    void eventoNoPublicado_oNoAltaDemanda_409() {
        when(eventos.obtener(EVENTO)).thenReturn(new FilaEventoCache.Info(EVENTO, "BORRADOR", true));
        FilaException e = assertThrows(FilaException.class, () -> service.ingresar(EVENTO, USER));
        assertEquals("FILA_EVENTO_NO_ELEGIBLE", e.getCode());

        when(eventos.obtener(EVENTO)).thenReturn(new FilaEventoCache.Info(EVENTO, "PUBLICADO", false));
        e = assertThrows(FilaException.class, () -> service.ingresar(EVENTO, USER));
        assertEquals("FILA_EVENTO_NO_ELEGIBLE", e.getCode());
    }

    @Test
    void turnoUsado_409() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.ingresar(EVENTO, USER)).thenThrow(FilaException.turnoUsado());

        FilaException e = assertThrows(FilaException.class, () -> service.ingresar(EVENTO, USER));
        assertEquals("FILA_TURNO_USADO", e.getCode());
    }

    @Test
    void redisCaido_enIngreso_503() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.ingresar(EVENTO, USER)).thenThrow(FilaException.noDisponible());

        FilaException e = assertThrows(FilaException.class, () -> service.ingresar(EVENTO, USER));
        assertEquals("FILA_NO_DISPONIBLE", e.getCode());
    }

    @Test
    void posicion_estadosDerivados() {
        when(eventos.obtener(EVENTO)).thenReturn(info());

        when(store.posicion(EVENTO, USER)).thenReturn(new FilaRedisStore.Posicion(57L, 50, false, false));
        FilaPosicionResponseDTO p = service.posicion(EVENTO, USER);
        assertEquals("EN_ESPERA", p.estado());
        assertEquals(7, p.delante());

        when(store.posicion(EVENTO, USER)).thenReturn(new FilaRedisStore.Posicion(40L, 100, false, false));
        p = service.posicion(EVENTO, USER);
        assertEquals("HABILITADO", p.estado());
        assertEquals(0, p.delante());

        when(store.posicion(EVENTO, USER)).thenReturn(new FilaRedisStore.Posicion(40L, 100, true, false));
        assertEquals("CON_TOKEN", service.posicion(EVENTO, USER).estado());

        when(store.posicion(EVENTO, USER)).thenReturn(new FilaRedisStore.Posicion(40L, 100, false, true));
        assertEquals("TURNO_USADO", service.posicion(EVENTO, USER).estado());
    }

    @Test
    void posicionSinIngreso_404() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.posicion(EVENTO, USER)).thenReturn(new FilaRedisStore.Posicion(null, 50, false, false));

        FilaException e = assertThrows(FilaException.class, () -> service.posicion(EVENTO, USER));
        assertEquals("FILA_NO_INGRESADO", e.getCode());
    }

    @Test
    void habilitacionAdmin_fijaTanda() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.resumen(EVENTO)).thenReturn(new FilaRedisStore.Resumen(200, 50, 40));
        when(store.habilitar(EVENTO, 100)).thenReturn(100L);

        FilaHabilitacionResponseDTO r = service.habilitar(EVENTO, 100, "ADMIN");

        assertEquals(100, r.posicionHabilitadaHasta());
        assertEquals("MANUAL", r.modo());
    }

    @Test
    void habilitacionOtroRol_403() {
        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> service.habilitar(EVENTO, 100, "ORGANIZADOR"));
        assertTrue(e.getMessage().contains("ADMIN"));
    }

    @Test
    void habilitacionQueRetrocede_409() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.resumen(EVENTO)).thenReturn(new FilaRedisStore.Resumen(200, 50, 40));
        when(store.habilitar(EVENTO, 20)).thenReturn(-1L);

        FilaException e = assertThrows(FilaException.class, () -> service.habilitar(EVENTO, 20, "ADMIN"));
        assertEquals("FILA_TANDA_NO_REDUCIBLE", e.getCode());
    }

    @Test
    void estado_publicoSinIdentidades() {
        when(eventos.obtener(EVENTO)).thenReturn(info());
        when(store.estado(EVENTO)).thenReturn(new FilaRedisStore.Estado(340, 100));

        FilaEstadoResponseDTO e = service.estado(EVENTO);
        assertEquals(340, e.totalEnFila());
        assertEquals(100, e.posicionHabilitadaHasta());
        assertTrue(e.abierta());
    }
}
