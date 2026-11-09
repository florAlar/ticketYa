package gateway.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-unit-tests-32-chars-minimum";

    private JwtUtil nuevoJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        return jwtUtil;
    }

    private String crearToken(String userId, String email, String nombre, String rol, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("nombre", nombre)
                .claim("rol", rol)
                .claim("roles", roles)
                .subject(email)
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void extractRoles_conClaimLista_devuelveLista() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE", "ORGANIZADOR"));

        List<String> roles = jwtUtil.extractRoles(token);

        assertEquals(List.of("CLIENTE", "ORGANIZADOR"), roles);
    }

    @Test
    void extractRoles_conClaimCsv_devuelveLista() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE", "ADMIN"));

        List<String> roles = jwtUtil.extractRoles(token);

        assertTrue(roles.contains("CLIENTE"));
        assertTrue(roles.contains("ADMIN"));
    }

    @Test
    void extractRol_devuelveRolPrincipal() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        // Rol ADMIN no está en la lista de roles, se agrega al principio
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "ADMIN",
                List.of("CLIENTE"));

        assertEquals("ADMIN", jwtUtil.extractRol(token));
    }

    @Test
    void extractEmail_devuelveEmail() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertEquals("a@b.com", jwtUtil.extractEmail(token));
    }

    @Test
    void extractUserId_devuelveUserId() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertEquals("user-1", jwtUtil.extractUserId(token));
    }

    @Test
    void validateToken_tokenValido_true() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_tokenInvalido_false() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        assertFalse(jwtUtil.validateToken("token.invalido.cualquiercosa"));
    }

    @Test
    void validateToken_tokenNulo_false() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void normalizeRole_accesoSeConvierteAPersonalAcceso() {
        assertEquals("PERSONAL_ACCESO", JwtUtil.normalizeRole("ACCESO"));
    }

    @Test
    void normalizeRole_otroRol_devuelveMayusculas() {
        assertEquals("ADMIN", JwtUtil.normalizeRole("admin"));
        assertEquals("CLIENTE", JwtUtil.normalizeRole(" cliente "));
    }

    @Test
    void normalizeRole_nulo_devuelveNulo() {
        assertNull(JwtUtil.normalizeRole(null));
    }

    @Test
    void isTokenExpired_tokenRecienGenerado_noExpirado() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void extractExpiration_devuelveFechaFutura() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        Date exp = jwtUtil.extractExpiration(token);
        assertNotNull(exp);
        assertTrue(exp.after(new Date()));
    }

    @Test
    void extractClaim_conResolver_devuelveValor() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = crearToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        String rol = jwtUtil.extractClaim(token, c -> c.get("rol", String.class));
        assertEquals("CLIENTE", rol);
    }
}
