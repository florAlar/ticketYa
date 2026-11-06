package usuarios.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emisión/validación JWT (patrón usersejemplo).
 * Claims alineados al gateway: userId (String UUID), email, nombre, rol, roles.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String email, String nombre, String rol, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("nombre", nombre)
                .claim("rol", rol)
                .subject(email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSigningKey());

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }
        return builder.compact();
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        String email = claims.get("email", String.class);
        return email != null ? email : claims.getSubject();
    }

    public String extractUserId(String token) {
        Object raw = extractAllClaims(token).get("userId");
        return raw != null ? String.valueOf(raw) : null;
    }

    public String extractRol(String token) {
        return extractClaim(token, c -> c.get("rol", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp != null && exp.before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
