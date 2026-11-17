package gateway.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import gateway.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new JwtAuthenticationFilter(jwtUtil);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    }

    @Test
    void doFilterInternal_rutaPublica_pasaFiltro() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_sinToken_devuelve401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/entradas");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(eq(401));
    }

    @Test
    void doFilterInternal_tokenInvalido_devuelve401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/entradas");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido");
        when(jwtUtil.validateToken("token.invalido")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(eq(401));
    }

    @Test
    void doFilterInternal_tokenValido_pasaFiltro() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/entradas");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token.valido");
        when(jwtUtil.validateToken("token.valido")).thenReturn(true);

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void doFilterInternal_optionsPasaSinFiltro() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/entradas");
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(filter);
    }

    @Test
    void recomendacionesDeEvento_sinToken_esPublica() throws Exception {
        when(request.getRequestURI())
                .thenReturn("/api/eventos/d0000000-0000-4000-8000-000000000001/recomendaciones");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void recomendacionesMe_sinToken_devuelve401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/recomendaciones/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(eq(401));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void recomendacionesMe_conTokenCliente_pasaFiltro() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/recomendaciones/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token.cliente");
        when(jwtUtil.validateToken("token.cliente")).thenReturn(true);
        when(jwtUtil.extractRoles("token.cliente")).thenReturn(List.of("CLIENTE"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void preferenciasMe_conTokenOrganizador_pasaFiltro() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/preferencias/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token.org");
        when(jwtUtil.validateToken("token.org")).thenReturn(true);
        when(jwtUtil.extractRoles("token.org")).thenReturn(List.of("ORGANIZADOR"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    /**
     * El JWT es obligatorio: sin Bearer el request se rechaza, aunque el cliente mande
     * headers X-User-* (no hay modo "identificación por encabezado").
     */
    @Test
    void sinToken_devuelve401AunConHeadersXUserDelCliente() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/entradas");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(eq(401));
        verify(filterChain, never()).doFilter(any(), any());
    }

    /**
     * Con Bearer inválido también 401, y el request no sigue hacia el msvc.
     */
    @Test
    void conTokenInvalido_devuelve401YNoPropaga() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/entradas");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido");
        when(jwtUtil.validateToken("token.invalido")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(eq(401));
        verify(filterChain, never()).doFilter(any(), any());
    }
}
