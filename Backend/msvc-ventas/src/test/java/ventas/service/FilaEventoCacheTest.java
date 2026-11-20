package ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ventas.clients.EventosClient;
import ventas.clients.EventosClient.EventoDetalleFeignDTO;
import ventas.exception.FilaException;

/** FIL-01 / D8: una sola llamada Feign por TTL y degradación controlada si msvc-eventos cae. */
@ExtendWith(MockitoExtension.class)
class FilaEventoCacheTest {

    private static final String EVENTO = "d0000000-0000-4000-8000-000000000001";

    @Mock
    private EventosClient client;

    private static EventoDetalleFeignDTO dto(String estado, Boolean altaDemanda) {
        EventoDetalleFeignDTO d = new EventoDetalleFeignDTO();
        d.setId(EVENTO);
        d.setEstado(estado);
        d.setEsAltaDemanda(altaDemanda);
        return d;
    }

    @Test
    void dosRequestsDentroDelTtl_hacenUnaSolaLlamadaFeign() {
        when(client.getEvento(EVENTO)).thenReturn(dto("PUBLICADO", true));
        FilaEventoCache cache = new FilaEventoCache(client, 10);

        assertEquals("PUBLICADO", cache.obtener(EVENTO).estado());
        assertTrue(cache.obtener(EVENTO).altaDemanda());

        verify(client, times(1)).getEvento(EVENTO);
    }

    @Test
    void sinDatoYEventosCaido_503() {
        when(client.getEvento(EVENTO)).thenThrow(new IllegalStateException("msvc-eventos caído"));
        FilaEventoCache cache = new FilaEventoCache(client, 10);

        FilaException e = assertThrows(FilaException.class, () -> cache.obtener(EVENTO));
        assertEquals("FILA_NO_DISPONIBLE", e.getCode());
    }

    @Test
    void conDatoVencidoYEventosCaido_devuelveCache() {
        when(client.getEvento(EVENTO))
                .thenReturn(dto("PUBLICADO", true))
                .thenThrow(new IllegalStateException("msvc-eventos caído"));
        FilaEventoCache cache = new FilaEventoCache(client, 0);

        assertEquals("PUBLICADO", cache.obtener(EVENTO).estado());
        assertEquals("PUBLICADO", cache.obtener(EVENTO).estado());
    }
}
