package gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * E2-TST-02 / E2-SEC-02: matriz de autorización por endpoint y por rol en el API Gateway.
 *
 * <p>Ejecuta {@link JwtAuthenticationFilter} con {@link JwtUtil} REAL (tokens HS256 idénticos a los
 * emitidos por {@code msvc-usuarios}) recorriendo sistemáticamente los endpoints expuestos de los
 * 6 microservicios con los 5 actores del sistema: anónimo, {@code CLIENTE}, {@code ORGANIZADOR},
 * {@code PERSONAL_ACCESO} y {@code ADMIN}.
 */
class AutorizacionPorRolMatrixTest {

    private static final String SECRET = "ticketya-dev-jwt-secret-change-me-32b";
    private static final String USER_ID = "b0000000-0000-4000-8000-000000000001";
    private static final String EMAIL = "matriz@ticketya.com";
    private static final String ID = "d0000000-0000-4000-8000-000000000001";
    private static final String SECTOR_ID = "c0000000-0000-4000-8000-000000000011";

    private static final int OK = 200;
    private static final int UNAUTH = 401;
    private static final int FORBIDDEN = 403;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        filter = new JwtAuthenticationFilter(jwtUtil, "secreto-interno-test");
    }

    private static String tokenConRol(String rol) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .claim("userId", USER_ID)
                .claim("email", EMAIL)
                .claim("nombre", "Usuario Matriz")
                .claim("rol", rol)
                .subject(EMAIL)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .claim("roles", List.of(rol))
                .signWith(key)
                .compact();
    }

    private void verificar(String method, String path, String rol, int esperado, boolean rutaPublica)
            throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        if (rol != null) {
            req.addHeader("Authorization", "Bearer " + tokenConRol(rol));
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        String actor = rol == null ? "ANONIMO" : rol;
        if (esperado == OK) {
            assertTrue(
                    chain.getRequest() != null,
                    () -> actor + " debería poder acceder a " + method + " " + path
                            + " (obtuvo status " + res.getStatus() + ")");
            HttpServletRequest down = (HttpServletRequest) chain.getRequest();
            if (rutaPublica) {
                assertEquals(
                        null,
                        down.getHeader(JwtAuthenticationFilter.HDR_USER_ID),
                        () -> "En ruta pública " + method + " " + path + " X-User-Id debe ir vacío");
            } else if (rol != null) {
                assertEquals(USER_ID, down.getHeader(JwtAuthenticationFilter.HDR_USER_ID));
            }
        } else {
            assertFalse(
                    chain.getRequest() != null,
                    () -> actor + " NO debería pasar el filtro en " + method + " " + path);
            assertEquals(
                    esperado,
                    res.getStatus(),
                    () -> "Status esperado para " + actor + " en " + method + " " + path);
        }
    }

    @ParameterizedTest(name = "{0} {1} -> anon:{2} cli:{3} org:{4} acc:{5} adm:{6}")
    @MethodSource("matrizEndpoints")
    void matrizDeAutorizacionPorRol(
            String method,
            String path,
            int anonimo,
            int cliente,
            int organizador,
            int acceso,
            int admin)
            throws Exception {
        boolean publica = anonimo == OK;
        verificar(method, path, null, anonimo, publica);
        verificar(method, path, "CLIENTE", cliente, publica);
        verificar(method, path, "ORGANIZADOR", organizador, publica);
        verificar(method, path, "PERSONAL_ACCESO", acceso, publica);
        verificar(method, path, "ADMIN", admin, publica);
    }

    static Stream<Arguments> matrizEndpoints() {
        return Stream.of(
                // --- Públicos y auth ---
                Arguments.of("GET", "/", OK, OK, OK, OK, OK),
                Arguments.of("POST", "/api/auth/login", OK, OK, OK, OK, OK),
                Arguments.of("POST", "/api/auth/register", OK, OK, OK, OK, OK),
                Arguments.of("POST", "/api/auth/refresh", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/auth/logout", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/eventos", OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/eventos/" + ID, OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/participantes", OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/participantes/" + ID, OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/eventos/" + ID + "/participantes", OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/eventos/disponibilidad", OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/eventos/" + ID + "/disponibilidad", OK, OK, OK, OK, OK),
                Arguments.of("GET", "/api/eventos/" + ID + "/recomendaciones", OK, OK, OK, OK, OK),

                // --- Usuarios (msvc-usuarios) ---
                Arguments.of("GET", "/api/usuarios/me", UNAUTH, OK, OK, OK, OK),
                Arguments.of("PATCH", "/api/usuarios/me", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/usuarios", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/usuarios/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/usuarios/mentira", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("PATCH", "/api/usuarios/" + ID + "/estado", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/usuarios/" + ID + "/roles", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/usuarios/" + ID + "/roles", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),

                // --- Eventos, lugares, sectores, participantes y organizadores (msvc-eventos) ---
                Arguments.of("POST", "/api/eventos", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/eventos/" + ID, UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("DELETE", "/api/eventos/" + ID, UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("POST", "/api/eventos/" + ID + "/publicacion", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("PATCH", "/api/eventos/" + ID + "/estado", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("PATCH", "/api/eventos/" + ID + "/fecha", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/eventos/" + ID + "/sectores", UNAUTH, OK, OK, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/eventos/" + ID + "/sectores/" + SECTOR_ID + "/precio", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/eventos/" + ID + "/participantes", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/organizadores/me/eventos", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/lugares", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/lugares/" + ID, UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/lugares/" + ID + "/sectores/" + SECTOR_ID + "/asientos", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/lugares/geocodificar", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/lugares", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/lugares/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("DELETE", "/api/lugares/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/lugares/" + ID + "/sectores", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/sectores/" + SECTOR_ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/participantes", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("PUT", "/api/participantes/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("DELETE", "/api/participantes/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),

                // --- Ventas: reservas, compras, pagos y reportes (msvc-ventas) ---
                Arguments.of("POST", "/api/reservas", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/reservas/" + ID, UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/reservas/" + ID + "/confirmacion", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/reservas/" + ID + "/cancelacion", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/reservas/" + ID + "/renovacion", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/compras", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/compras", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/compras/" + ID, UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/compras/" + ID + "/pagos", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/compras/" + ID + "/pagos", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/pagos/" + ID + "/reintentos", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/reportes/eventos/" + ID, UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/reportes/eventos/" + ID + "/ventas", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/reportes/eventos/" + ID + "/sectores", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/reportes/eventos/" + ID + "/ocupacion", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("GET", "/api/reportes/eventos/" + ID + "/recaudacion", UNAUTH, FORBIDDEN, OK, FORBIDDEN, OK),
                Arguments.of("DELETE", "/api/reportes/eventos/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),

                // --- Entradas, devoluciones y control de acceso QR (msvc-entradas) ---
                Arguments.of("GET", "/api/entradas", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/entradas/" + ID, UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/entradas/" + ID + "/qr", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/entradas/" + ID + "/devoluciones", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/devoluciones", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/devoluciones/" + ID, UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/devoluciones/" + ID + "/aprobacion", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/devoluciones/" + ID + "/rechazo", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/eventos/" + ID + "/validaciones", UNAUTH, FORBIDDEN, FORBIDDEN, OK, OK),
                Arguments.of("GET", "/api/eventos/" + ID + "/validaciones", UNAUTH, FORBIDDEN, OK, OK, OK),

                // --- Preferencias, recomendaciones e historial (msvc-preferencias) ---
                Arguments.of("GET", "/api/preferencias/me", UNAUTH, OK, OK, OK, OK),
                Arguments.of("PUT", "/api/preferencias/me", UNAUTH, OK, OK, OK, OK),
                Arguments.of("PUT", "/api/preferencias/me/artistas", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/preferencias/me/notificaciones", UNAUTH, OK, OK, OK, OK),
                Arguments.of("PUT", "/api/preferencias/me/notificaciones", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/recomendaciones/me", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/clientes/" + ID + "/historial", UNAUTH, OK, FORBIDDEN, FORBIDDEN, OK),

                // --- Notificaciones (msvc-notificaciones) ---
                Arguments.of("GET", "/api/notificaciones/me", UNAUTH, OK, OK, OK, OK),
                Arguments.of("GET", "/api/notificaciones/me/" + ID, UNAUTH, OK, OK, OK, OK),
                Arguments.of("PATCH", "/api/notificaciones/me/" + ID + "/leida", UNAUTH, OK, OK, OK, OK),
                Arguments.of("POST", "/api/notificaciones", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/notificaciones/" + ID + "/estado", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),

                // --- Endpoints internos Feign (por gateway solo ADMIN) ---
                Arguments.of("POST", "/api/eventos/" + ID + "/cupos", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/eventos/" + ID + "/cupos/liberacion", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/eventos/" + ID + "/reintegro-cancelacion", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/eventos/" + ID + "/entradas/resumen", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/entradas/emision", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("POST", "/api/preferencias/clientes/matching", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/preferencias/clientes/" + ID + "/notificaciones", UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK),
                Arguments.of("GET", "/api/compras/evento/" + ID, UNAUTH, FORBIDDEN, FORBIDDEN, FORBIDDEN, OK));
    }

    @Test
    void rolLegadoAcceso_seNormalizaAPersonalAcceso() throws Exception {
        verificar("POST", "/api/eventos/" + ID + "/validaciones", "ACCESO", OK, false);
        verificar("GET", "/api/usuarios", "ACCESO", FORBIDDEN, false);
        verificar("POST", "/api/eventos", "ACCESO", FORBIDDEN, false);
    }

    @Test
    void rolDesconocido_recibe403EnRutasProtegidas() throws Exception {
        verificar("GET", "/api/entradas", "INVITADO", FORBIDDEN, false);
        verificar("POST", "/api/eventos", "INVITADO", FORBIDDEN, false);
        verificar("GET", "/api/usuarios", "INVITADO", FORBIDDEN, false);
    }
}
