package gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integración filtro + {@link JwtUtil} REALES (sin mocks), con tokens armados igual que los emite
 * {@code msvc-usuarios} ({@code usuarios.util.JwtUtil#generateToken}): claims userId, email, nombre,
 * rol, roles; subject = email; HS256.
 *
 * <p>Cubre lo que {@code JwtAuthenticationFilterTest} (JwtUtil mockeado) no puede: que el secreto
 * y los claims de usuarios y gateway estén alineados, y la propagación de headers X-User-*.
 */
class JwtGatewayIntegrationTest {

    /** Mismo default que {@code jwt.secret} en application.yml de gateway y usuarios. */
    private static final String SECRET = "ticketya-dev-jwt-secret-change-me-32b";

    /** Token compartido que el gateway inyecta en las rutas de la fila. */
    private static final String TOKEN_INTERNO = "secreto-interno-test";

    private static final String USER_ID = "b0000000-0000-4000-8000-000000000001";
    private static final String EMAIL = "demo@ticketya.com";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    /** Réplica de usuarios.util.JwtUtil#generateToken. */
    private static String tokenComoUsuarios(String secret, String rol, long expiraEnMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .claim("userId", USER_ID)
                .claim("email", EMAIL)
                .claim("nombre", "Usuario Demo")
                .claim("rol", rol)
                .subject(EMAIL)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiraEnMs))
                .claim("roles", List.of(rol))
                .signWith(key)
                .compact();
    }

    private static final class Resultado {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        boolean paso() {
            return chain.getRequest() != null;
        }

        HttpServletRequest requestDownstream() {
            ServletRequest r = chain.getRequest();
            return (HttpServletRequest) r;
        }
    }

    private Resultado ejecutar(MockHttpServletRequest req) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, TOKEN_INTERNO);
        Resultado r = new Resultado();
        filter.doFilterInternal(req, r.response, r.chain);
        return r;
    }

    private static MockHttpServletRequest get(String uri, String bearer) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        if (bearer != null) {
            req.addHeader("Authorization", "Bearer " + bearer);
        }
        return req;
    }

    @Test
    void tokenDeUsuarios_esAceptadoYPropagaHeadersDeIdentidad() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);

        Resultado r = ejecutar(get("/api/entradas", token));

        assertTrue(r.paso(), "el gateway debe aceptar un token emitido por usuarios");
        HttpServletRequest down = r.requestDownstream();
        assertEquals(USER_ID, down.getHeader(JwtAuthenticationFilter.HDR_USER_ID));
        assertEquals(EMAIL, down.getHeader(JwtAuthenticationFilter.HDR_USER_EMAIL));
        assertEquals("CLIENTE", down.getHeader(JwtAuthenticationFilter.HDR_USER_ROLE));
        assertEquals("CLIENTE", down.getHeader(JwtAuthenticationFilter.HDR_USER_ROLES));
        assertNull(down.getHeader(JwtAuthenticationFilter.HDR_INTERNAL));
    }

    @Test
    void headersFalsificadosPorElCliente_sonReemplazadosPorLosDelToken() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);
        MockHttpServletRequest req = get("/api/entradas", token);
        req.addHeader("X-User-Id", "00000000-0000-0000-0000-000000000000");
        req.addHeader("X-User-Role", "ADMIN");

        Resultado r = ejecutar(req);

        assertTrue(r.paso());
        assertEquals(USER_ID, r.requestDownstream().getHeader("X-User-Id"));
        assertEquals("CLIENTE", r.requestDownstream().getHeader("X-User-Role"));
    }

    @Test
    void tokenInternoDelCliente_noSePropagaYEnFilaSeReemplaza() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);

        MockHttpServletRequest normal = get("/api/entradas", token);
        normal.addHeader(JwtAuthenticationFilter.HDR_INTERNAL, "intruso");
        assertNull(ejecutar(normal).requestDownstream()
                .getHeader(JwtAuthenticationFilter.HDR_INTERNAL));

        MockHttpServletRequest fila = get(
                "/api/eventos/d0000000-0000-4000-8000-000000000001/fila/posicion", token);
        fila.addHeader(JwtAuthenticationFilter.HDR_INTERNAL, "intruso");
        assertEquals(TOKEN_INTERNO, ejecutar(fila).requestDownstream()
                .getHeader(JwtAuthenticationFilter.HDR_INTERNAL));
    }

    @Test
    void headersDelClienteEnMinusculas_noSobrevivenComoDuplicados() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);
        MockHttpServletRequest req = get("/api/entradas", token);
        req.addHeader("x-user-id", "00000000-0000-0000-0000-000000000000");
        req.addHeader("x-user-role", "ADMIN");
        req.addHeader("x-internal-token", "intruso");

        HttpServletRequest down = ejecutar(req).requestDownstream();

        assertEquals(USER_ID, down.getHeader("X-User-Id"));
        assertEquals("CLIENTE", down.getHeader("X-User-Role"));
        assertNull(down.getHeader("X-Internal-Token"));
        assertEquals(1, contarHeader(down, "X-User-Id"));
        assertEquals(1, contarHeader(down, "X-User-Role"));
    }

    @Test
    void excepcionDelDownstream_noSeConvierteEn401() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);
        MockHttpServletRequest req = get("/api/entradas", token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (rq, rs) -> {
            throw new IllegalStateException("proxy caído");
        };
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, TOKEN_INTERNO);

        assertThrows(IllegalStateException.class,
                () -> filter.doFilterInternal(req, res, chain));
        assertEquals(200, res.getStatus());
    }

    private static long contarHeader(HttpServletRequest request, String name) {
        return Collections.list(request.getHeaderNames()).stream()
                .filter(n -> n.equalsIgnoreCase(name))
                .count();
    }

    @Test
    void tokenFirmadoConOtroSecreto_devuelve401() throws Exception {
        String token = tokenComoUsuarios("otro-secreto-distinto-de-32-caracteres-o-mas!!", "ADMIN", 60_000);

        Resultado r = ejecutar(get("/api/entradas", token));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void tokenExpirado_devuelve401() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", -60_000);

        Resultado r = ejecutar(get("/api/entradas", token));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void clienteNoPuedeListarUsuarios_devuelve403() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);

        Resultado r = ejecutar(get("/api/usuarios", token));

        assertEquals(403, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void adminPuedeListarUsuarios() throws Exception {
        String token = tokenComoUsuarios(SECRET, "ADMIN", 60_000);

        Resultado r = ejecutar(get("/api/usuarios", token));

        assertTrue(r.paso());
        assertEquals("ADMIN", r.requestDownstream().getHeader("X-User-Role"));
    }

    @Test
    void bajaDeLugares_esSoloAdmin() throws Exception {
        MockHttpServletRequest comoOrg = new MockHttpServletRequest("DELETE", "/api/lugares/c0000000-0000-4000-8000-000000000001");
        comoOrg.addHeader("Authorization", "Bearer " + tokenComoUsuarios(SECRET, "ORGANIZADOR", 60_000));
        Resultado r1 = ejecutar(comoOrg);
        assertEquals(403, r1.response.getStatus());
        assertEquals(false, r1.paso());

        MockHttpServletRequest comoAdmin = new MockHttpServletRequest("DELETE", "/api/lugares/c0000000-0000-4000-8000-000000000001");
        comoAdmin.addHeader("Authorization", "Bearer " + tokenComoUsuarios(SECRET, "ADMIN", 60_000));
        Resultado r2 = ejecutar(comoAdmin);
        assertTrue(r2.paso());
    }

    @Test
    void lugaresYSectores_elOrganizadorSoloLee_lasEscriturasSonDeAdmin() throws Exception {
        String org = tokenComoUsuarios(SECRET, "ORGANIZADOR", 60_000);
        String admin = tokenComoUsuarios(SECRET, "ADMIN", 60_000);
        String[][] escrituras = {
            {"POST", "/api/lugares"},
            {"PUT", "/api/lugares/c0000000-0000-4000-8000-000000000001"},
            {"POST", "/api/lugares/c0000000-0000-4000-8000-000000000001/sectores"},
            {"PUT", "/api/sectores/c0000000-0000-4000-8000-000000000011"},
            {"DELETE", "/api/lugares/c0000000-0000-4000-8000-000000000001"},
        };
        for (String[] e : escrituras) {
            MockHttpServletRequest comoOrg = new MockHttpServletRequest(e[0], e[1]);
            comoOrg.addHeader("Authorization", "Bearer " + org);
            Resultado r = ejecutar(comoOrg);
            assertEquals(403, r.response.getStatus(), "organizador " + e[0] + " " + e[1]);

            MockHttpServletRequest comoAdmin = new MockHttpServletRequest(e[0], e[1]);
            comoAdmin.addHeader("Authorization", "Bearer " + admin);
            assertTrue(ejecutar(comoAdmin).paso(), "admin " + e[0] + " " + e[1]);
        }
        // lectura: el organizador elige lugar al crear un evento
        MockHttpServletRequest lee = new MockHttpServletRequest("GET", "/api/lugares");
        lee.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(lee).paso());
        MockHttpServletRequest leeUno = new MockHttpServletRequest("GET", "/api/lugares/c0000000-0000-4000-8000-000000000001");
        leeUno.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(leeUno).paso());

        MockHttpServletRequest geoOrg = new MockHttpServletRequest("GET", "/api/lugares/geocodificar");
        geoOrg.addHeader("Authorization", "Bearer " + org);
        assertEquals(403, ejecutar(geoOrg).response.getStatus());

        MockHttpServletRequest geoAdmin = new MockHttpServletRequest("GET", "/api/lugares/geocodificar");
        geoAdmin.addHeader("Authorization", "Bearer " + admin);
        assertTrue(ejecutar(geoAdmin).paso());
    }

    @Test
    void participantes_elOrganizadorSoloDaDeAlta_editarYEliminarEsDeAdmin() throws Exception {
        String org = tokenComoUsuarios(SECRET, "ORGANIZADOR", 60_000);
        String admin = tokenComoUsuarios(SECRET, "ADMIN", 60_000);
        String id = "/api/participantes/p0000000-0000-4000-8000-000000000001";
        for (String[] e : new String[][] {{"PUT", id}, {"DELETE", id}}) {
            MockHttpServletRequest comoOrg = new MockHttpServletRequest(e[0], e[1]);
            comoOrg.addHeader("Authorization", "Bearer " + org);
            Resultado r = ejecutar(comoOrg);
            assertEquals(403, r.response.getStatus(), "organizador " + e[0]);

            MockHttpServletRequest comoAdmin = new MockHttpServletRequest(e[0], e[1]);
            comoAdmin.addHeader("Authorization", "Bearer " + admin);
            assertTrue(ejecutar(comoAdmin).paso(), "admin " + e[0]);
        }
        // el alta sigue abierta al organizador (para no bloquear la creación del evento)
        MockHttpServletRequest alta = new MockHttpServletRequest("POST", "/api/participantes");
        alta.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(alta).paso());
        // y asociar participantes a su evento también
        MockHttpServletRequest asociar = new MockHttpServletRequest("PUT", "/api/eventos/d0000000-0000-4000-8000-000000000001/participantes");
        asociar.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(asociar).paso());
    }

    @Test
    void fila_ingresoPosicionYValidacion_exigenJwtDeCliente() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);
        for (String uri : new String[] {
                "/api/eventos/d0000000-0000-4000-8000-000000000001/fila/ingreso",
                "/api/eventos/d0000000-0000-4000-8000-000000000001/fila/posicion",
                "/api/eventos/d0000000-0000-4000-8000-000000000001/fila/validacion"}) {
            String metodo = uri.endsWith("posicion") ? "GET" : "POST";
            MockHttpServletRequest conToken = new MockHttpServletRequest(metodo, uri);
            conToken.addHeader("Authorization", "Bearer " + token);
            Resultado r = ejecutar(conToken);
            assertTrue(r.paso(), uri);
            assertEquals(USER_ID, r.requestDownstream().getHeader("X-User-Id"), uri);
            assertEquals(TOKEN_INTERNO,
                    r.requestDownstream().getHeader(JwtAuthenticationFilter.HDR_INTERNAL), uri);

            MockHttpServletRequest sinToken = new MockHttpServletRequest(metodo, uri);
            assertEquals(401, ejecutar(sinToken).response.getStatus(), uri);
        }
    }

    @Test
    void fila_estadoEsPublico_yNoPropagaIdentidad() throws Exception {
        MockHttpServletRequest req = get(
                "/api/eventos/d0000000-0000-4000-8000-000000000001/fila/estado", null);
        req.addHeader("X-User-Id", USER_ID);
        req.addHeader("X-User-Role", "ADMIN");

        Resultado r = ejecutar(req);

        assertTrue(r.paso());
        assertNull(r.requestDownstream().getHeader("X-User-Id"));
        assertNull(r.requestDownstream().getHeader("X-User-Role"));
        assertEquals(TOKEN_INTERNO,
                r.requestDownstream().getHeader(JwtAuthenticationFilter.HDR_INTERNAL));
    }

    @Test
    void fila_habilitacion_soloAdminEnElGateway() throws Exception {
        String uri = "/api/eventos/d0000000-0000-4000-8000-000000000001/fila/habilitacion";
        for (String rol : new String[] {"CLIENTE", "ORGANIZADOR", "PERSONAL_ACCESO"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
            req.addHeader("Authorization", "Bearer " + tokenComoUsuarios(SECRET, rol, 60_000));
            assertEquals(403, ejecutar(req).response.getStatus(), rol);
        }
        MockHttpServletRequest admin = new MockHttpServletRequest("POST", uri);
        admin.addHeader("Authorization", "Bearer " + tokenComoUsuarios(SECRET, "ADMIN", 60_000));
        Resultado r = ejecutar(admin);
        assertTrue(r.paso());
        assertEquals(TOKEN_INTERNO,
                r.requestDownstream().getHeader(JwtAuthenticationFilter.HDR_INTERNAL));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        Resultado r = ejecutar(get("/api/entradas", null));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    /**
     * El JWT es obligatorio: sin Bearer, los headers X-User-* del cliente NO habilitan el paso.
     * Nadie se puede declarar ADMIN a mano.
     */
    @Test
    void sinToken_conHeadersXUserFalsificados_devuelve401() throws Exception {
        MockHttpServletRequest req = get("/api/usuarios", null);
        req.addHeader("X-User-Id", USER_ID);
        req.addHeader("X-User-Role", "ADMIN");

        Resultado r = ejecutar(req);

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void conBearerInvalido_devuelve401() throws Exception {
        Resultado r = ejecutar(get("/api/entradas", "abc.def.ghi"));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    /**
     * En rutas públicas tampoco se propaga la identidad del cliente: los headers X-User-* se vacían.
     */
    @Test
    void rutaPublica_noPropagaHeadersXUserDelCliente() throws Exception {
        MockHttpServletRequest req = get("/api/eventos", null);
        req.addHeader("X-User-Id", USER_ID);
        req.addHeader("X-User-Role", "ADMIN");

        Resultado r = ejecutar(req);

        assertTrue(r.paso());
        assertNull(r.requestDownstream().getHeader("X-User-Id"));
        assertNull(r.requestDownstream().getHeader("X-User-Role"));
        assertNull(r.requestDownstream().getHeader(JwtAuthenticationFilter.HDR_INTERNAL));
    }

    /**
     * Contrato de configuración: el secreto por defecto de gateway y de usuarios debe ser el mismo.
     * Si alguien cambia uno y no el otro, el login emite tokens que el gateway rechaza.
     */
    @Test
    void secretoPorDefecto_esElMismoEnGatewayYUsuarios() throws Exception {
        Path gw = Path.of("src/main/resources/application.yml");
        Path us = Path.of("../msvc-usuarios/src/main/resources/application.yml");
        assumeTrue(Files.exists(gw) && Files.exists(us), "se ejecuta solo desde Backend/APIGateway del monorepo");

        String sGw = defaultDelSecret(Files.readString(gw));
        String sUs = defaultDelSecret(Files.readString(us));

        assertNotNull(sGw, "secret por defecto no encontrado en gateway");
        assertNotNull(sUs, "secret por defecto no encontrado en usuarios");
        assertEquals(sGw, sUs);
        assertEquals(SECRET, sGw);
    }

    private static String defaultDelSecret(String yml) {
        Matcher m = Pattern.compile("secret:\\s*\\$\\{JWT_SECRET:([^}]+)}").matcher(yml);
        return m.find() ? m.group(1).trim() : null;
    }
}
