package gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lectura/validación JWT en el gateway (emisión = msvc-usuarios).
 * Claims esperados (alineados al ejemplo + TicketYa):
 * - sub / email
 * - userId (String UUID)
 * - rol (String) y/o roles (lista o CSV)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email;
        }
        return claims.getSubject();
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object raw = claims.get("userId");
        if (raw == null) {
            raw = claims.get("id");
        }
        return raw != null ? String.valueOf(raw) : null;
    }

    /** Rol principal (claim {@code rol} o primero de {@code roles}). */
    public String extractRol(String token) {
        List<String> roles = extractRoles(token);
        return roles.isEmpty() ? null : roles.get(0);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        List<String> out = new ArrayList<>();

        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null && !String.valueOf(o).isBlank()) {
                    out.add(normalizeRole(String.valueOf(o)));
                }
            }
        } else if (rolesClaim instanceof String csv && !csv.isBlank()) {
            for (String part : csv.split(",")) {
                if (!part.isBlank()) {
                    out.add(normalizeRole(part.trim()));
                }
            }
        }

        String single = claims.get("rol", String.class);
        if (single == null) {
            single = claims.get("role", String.class);
        }
        if (single != null && !single.isBlank()) {
            String n = normalizeRole(single);
            if (!out.contains(n)) {
                out.add(0, n);
            }
        }
        return out;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
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

    /** Alias legado FE: ACCESO → PERSONAL_ACCESO. */
    public static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String r = role.trim().toUpperCase();
        if ("ACCESO".equals(r)) {
            return "PERSONAL_ACCESO";
        }
        return r;
    }
}
