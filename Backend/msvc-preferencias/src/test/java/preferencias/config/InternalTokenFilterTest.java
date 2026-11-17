package preferencias.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalTokenFilterTest {

    private static final String TOKEN = "secreto-interno";

    @Test
    void sinTokenDevuelve401() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internas/backfill");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void tokenIncorrectoDevuelve401() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internas/backfill");
        request.addHeader(InternalTokenFilter.HEADER, "otro");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void tokenCorrectoDejaPasar() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internas/backfill");
        request.addHeader(InternalTokenFilter.HEADER, TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void rutaNoInternaNoSeControla() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/preferencias/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
    }
}
