package usuarios.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import usuarios.util.JwtUtil;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-unit-tests-32-chars-minimum";

    private JwtUtil nuevoJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
        return jwtUtil;
    }

    @Test
    void generateToken_thenValidateToken_isValid() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void generateToken_sinRoles_esValido() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE", null);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void extractEmail_devuelveEmail() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertEquals("a@b.com", jwtUtil.extractEmail(token));
    }

    @Test
    void extractUserId_devuelveUserId() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertEquals("user-1", jwtUtil.extractUserId(token));
    }

    @Test
    void extractRol_devuelveRol() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "ORGANIZADOR",
                List.of("CLIENTE", "ORGANIZADOR"));

        assertEquals("ORGANIZADOR", jwtUtil.extractRol(token));
    }

    @Test
    void extractExpiration_devuelveFechaFutura() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        Date exp = jwtUtil.extractExpiration(token);
        assertNotNull(exp);
        assertTrue(exp.after(new Date()));
    }

    @Test
    void extractClaim_conResolver_devuelveValor() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        String rol = jwtUtil.extractClaim(token, c -> c.get("rol", String.class));
        assertEquals("CLIENTE", rol);
    }

    @Test
    void isTokenExpired_tokenRecienGenerado_noExpirado() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));

        assertFalse(jwtUtil.isTokenExpired(token));
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
    void getSigningKey_noEsNulo() {
        JwtUtil jwtUtil = nuevoJwtUtil();
        // forzar inicialización de la clave mediante generateToken
        String token = jwtUtil.generateToken("user-1", "a@b.com", "Usuario Uno", "CLIENTE",
                List.of("CLIENTE"));
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }
}
