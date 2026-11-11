package gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private Resultado ejecutar(boolean jwtEnabled, MockHttpServletRequest req) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtEnabled);
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

        Resultado r = ejecutar(true, get("/api/entradas", token));

        assertTrue(r.paso(), "el gateway debe aceptar un token emitido por usuarios");
        HttpServletRequest down = r.requestDownstream();
        assertEquals(USER_ID, down.getHeader(JwtAuthenticationFilter.HDR_USER_ID));
        assertEquals(EMAIL, down.getHeader(JwtAuthenticationFilter.HDR_USER_EMAIL));
        assertEquals("CLIENTE", down.getHeader(JwtAuthenticationFilter.HDR_USER_ROLE));
        assertEquals("CLIENTE", down.getHeader(JwtAuthenticationFilter.HDR_USER_ROLES));
    }

    @Test
    void headersFalsificadosPorElCliente_sonReemplazadosPorLosDelToken() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);
        MockHttpServletRequest req = get("/api/entradas", token);
        req.addHeader("X-User-Id", "00000000-0000-0000-0000-000000000000");
        req.addHeader("X-User-Role", "ADMIN");

        Resultado r = ejecutar(true, req);

        assertTrue(r.paso());
        assertEquals(USER_ID, r.requestDownstream().getHeader("X-User-Id"));
        assertEquals("CLIENTE", r.requestDownstream().getHeader("X-User-Role"));
    }

    @Test
    void tokenFirmadoConOtroSecreto_devuelve401() throws Exception {
        String token = tokenComoUsuarios("otro-secreto-distinto-de-32-caracteres-o-mas!!", "ADMIN", 60_000);

        Resultado r = ejecutar(true, get("/api/entradas", token));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void tokenExpirado_devuelve401() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", -60_000);

        Resultado r = ejecutar(true, get("/api/entradas", token));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void clienteNoPuedeListarUsuarios_devuelve403() throws Exception {
        String token = tokenComoUsuarios(SECRET, "CLIENTE", 60_000);

        Resultado r = ejecutar(true, get("/api/usuarios", token));

        assertEquals(403, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    @Test
    void adminPuedeListarUsuarios() throws Exception {
        String token = tokenComoUsuarios(SECRET, "ADMIN", 60_000);

        Resultado r = ejecutar(true, get("/api/usuarios", token));

        assertTrue(r.paso());
        assertEquals("ADMIN", r.requestDownstream().getHeader("X-User-Role"));
    }

    @Test
    void bajaDeLugares_esSoloAdmin() throws Exception {
        MockHttpServletRequest comoOrg = new MockHttpServletRequest("DELETE", "/api/lugares/c0000000-0000-4000-8000-000000000001");
        comoOrg.addHeader("Authorization", "Bearer " + tokenComoUsuarios(SECRET, "ORGANIZADOR", 60_000));
        Resultado r1 = ejecutar(true, comoOrg);
        assertEquals(403, r1.response.getStatus());
        assertEquals(false, r1.paso());

        MockHttpServletRequest comoAdmin = new MockHttpServletRequest("DELETE", "/api/lugares/c0000000-0000-4000-8000-000000000001");
        comoAdmin.addHeader("Authorization", "Bearer " + tokenComoUsuarios(SECRET, "ADMIN", 60_000));
        Resultado r2 = ejecutar(true, comoAdmin);
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
            Resultado r = ejecutar(true, comoOrg);
            assertEquals(403, r.response.getStatus(), "organizador " + e[0] + " " + e[1]);

            MockHttpServletRequest comoAdmin = new MockHttpServletRequest(e[0], e[1]);
            comoAdmin.addHeader("Authorization", "Bearer " + admin);
            assertTrue(ejecutar(true, comoAdmin).paso(), "admin " + e[0] + " " + e[1]);
        }
        // lectura: el organizador elige lugar al crear un evento
        MockHttpServletRequest lee = new MockHttpServletRequest("GET", "/api/lugares");
        lee.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(true, lee).paso());
        MockHttpServletRequest leeUno = new MockHttpServletRequest("GET", "/api/lugares/c0000000-0000-4000-8000-000000000001");
        leeUno.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(true, leeUno).paso());
    }

    @Test
    void participantes_elOrganizadorSoloDaDeAlta_editarYEliminarEsDeAdmin() throws Exception {
        String org = tokenComoUsuarios(SECRET, "ORGANIZADOR", 60_000);
        String admin = tokenComoUsuarios(SECRET, "ADMIN", 60_000);
        String id = "/api/participantes/p0000000-0000-4000-8000-000000000001";
        for (String[] e : new String[][] {{"PUT", id}, {"DELETE", id}}) {
            MockHttpServletRequest comoOrg = new MockHttpServletRequest(e[0], e[1]);
            comoOrg.addHeader("Authorization", "Bearer " + org);
            Resultado r = ejecutar(true, comoOrg);
            assertEquals(403, r.response.getStatus(), "organizador " + e[0]);

            MockHttpServletRequest comoAdmin = new MockHttpServletRequest(e[0], e[1]);
            comoAdmin.addHeader("Authorization", "Bearer " + admin);
            assertTrue(ejecutar(true, comoAdmin).paso(), "admin " + e[0]);
        }
        // el alta sigue abierta al organizador (para no bloquear la creación del evento)
        MockHttpServletRequest alta = new MockHttpServletRequest("POST", "/api/participantes");
        alta.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(true, alta).paso());
        // y asociar participantes a su evento también
        MockHttpServletRequest asociar = new MockHttpServletRequest("PUT", "/api/eventos/d0000000-0000-4000-8000-000000000001/participantes");
        asociar.addHeader("Authorization", "Bearer " + org);
        assertTrue(ejecutar(true, asociar).paso());
    }

    @Test
    void jwtHabilitado_sinToken_devuelve401() throws Exception {
        Resultado r = ejecutar(true, get("/api/entradas", null));

        assertEquals(401, r.response.getStatus());
        assertEquals(false, r.paso());
    }

    /**
     * Documenta la limitación de la Entrega 1 (jwt-enabled=false): sin Bearer el gateway CONFÍA en
     * los headers X-User-* que manda el cliente, por lo que cualquiera puede declararse ADMIN.
     * Con TICKETYA_JWT_ENABLED=true (Entrega 2) esto deja de ser posible.
     */
    @Test
    void entrega1_sinToken_confiaEnHeadersDelCliente_limitacionConocida() throws Exception {
        MockHttpServletRequest req = get("/api/usuarios", null);
        req.addHeader("X-User-Id", USER_ID);
        req.addHeader("X-User-Role", "ADMIN");

        Resultado r = ejecutar(false, req);

        assertTrue(r.paso());
        assertEquals("ADMIN", r.requestDownstream().getHeader("X-User-Role"));
    }

    @Test
    void entrega1_conBearerInvalido_sigueDevolviendo401() throws Exception {
        Resultado r = ejecutar(false, get("/api/entradas", "abc.def.ghi"));

        assertEquals(401, r.response.getStatus());
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
