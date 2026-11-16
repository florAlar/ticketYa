package entradas.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import feign.RequestTemplate;

/** E2-NFR-03: correlation id (filtro, propagación por Feign y bitácora). */
class CorrelationIdTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    private static final class Captura {
        final AtomicReference<String> mdcDuranteLaCadena = new AtomicReference<>();
        final AtomicReference<HttpServletRequest> request = new AtomicReference<>();
        final MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                mdcDuranteLaCadena.set(MDC.get(CorrelationIdFilter.MDC_KEY));
                request.set((HttpServletRequest) req);
                super.doFilter(req, res);
            }
        };
    }

    private Captura ejecutar(String headerEntrante, MockHttpServletResponse res) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/algo");
        if (headerEntrante != null) req.addHeader(CorrelationIdFilter.HEADER, headerEntrante);
        Captura c = new Captura();
        filter.doFilter(req, res, c.chain);
        return c;
    }

    @Test
    void sinHeader_generaUnId_loPonePorLaCadenaYEnLaRespuesta() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();

        Captura c = ejecutar(null, res);

        String id = res.getHeader(CorrelationIdFilter.HEADER);
        assertTrue(id != null && UUID.fromString(id) != null, "se genera un UUID");
        assertEquals(id, c.mdcDuranteLaCadena.get(), "el MDC lo tiene mientras se procesa");
        assertEquals(id, c.request.get().getHeader(CorrelationIdFilter.HEADER), "viaja en la request reenviada");
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "el MDC se limpia al terminar");
    }

    @Test
    void elUsuarioDelGateway_quedaEnElMdc_soloDuranteLaCadena() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/algo");
        req.addHeader("X-User-Id", "b0000000-0000-4000-8000-000000000001");
        AtomicReference<String> visto = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse res) {
                visto.set(MDC.get(CorrelationIdFilter.MDC_USER));
            }
        };

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertEquals("b0000000-0000-4000-8000-000000000001", visto.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_USER));
    }

    @Test
    void headerValido_seRespeta() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();

        Captura c = ejecutar("abc-123_XYZ.456", res);

        assertEquals("abc-123_XYZ.456", res.getHeader(CorrelationIdFilter.HEADER));
        assertEquals("abc-123_XYZ.456", c.mdcDuranteLaCadena.get());
    }

    @Test
    void headerInvalido_seReemplaza_yNoLlegaALosLogs() throws Exception {
        for (String malo : new String[] {"corto", "con espacios y \n salto", "a".repeat(65), "<script>alert(1)</script>"}) {
            MockHttpServletResponse res = new MockHttpServletResponse();

            Captura c = ejecutar(malo, res);

            String id = res.getHeader(CorrelationIdFilter.HEADER);
            assertNotEquals(malo, id, malo);
            assertEquals(id, c.mdcDuranteLaCadena.get());
            assertEquals(id, c.request.get().getHeader(CorrelationIdFilter.HEADER));
        }
    }

    @Test
    void siLaCadenaFalla_elMdcSeLimpiaIgual() {
        MockFilterChain explota = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new IllegalStateException("boom");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), explota));

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void feign_propagaElIdVigente_ySinIdNoAgregaNada() {
        var interceptor = new FeignCorrelationConfig().correlationIdInterceptor();

        MDC.put(CorrelationIdFilter.MDC_KEY, "id-de-la-request-1");
        RequestTemplate con = new RequestTemplate();
        interceptor.apply(con);
        assertEquals("id-de-la-request-1", con.headers().get(CorrelationIdFilter.HEADER).iterator().next());

        MDC.clear();
        RequestTemplate sin = new RequestTemplate();
        interceptor.apply(sin);
        assertTrue(sin.headers().get(CorrelationIdFilter.HEADER) == null);
    }

    @Test
    void bitacora_noRompeConValoresNulosOSaltosDeLinea() {
        Bitacora.registrar("PRUEBA", null, "r-1", "a=1\nINJECTED accion=FALSA");
    }
}
