package ventas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Exige el token compartido entre microservicios en dos casos:
 *
 * <ul>
 *   <li>{@code /internas/**}: endpoints que no pasan por el gateway.</li>
 *   <li>{@code /eventos/{id}/fila/**}: los sella el gateway, de modo que un cliente no pueda
 *       llamar a ventas por el puerto 8083 y forjar {@code X-User-Id}/{@code X-User-Role}.</li>
 * </ul>
 *
 * Se usa {@link MessageDigest#isEqual} para comparar sin filtrar tiempos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    private static final Logger log = LoggerFactory.getLogger(InternalTokenFilter.class);

    private final String expectedToken;

    public InternalTokenFilter(@Value("${ticketya.interno.token:}") String expectedToken) {
        this.expectedToken = expectedToken == null ? "" : expectedToken.trim();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean interna = path != null && path.startsWith("/internas/");
        if (!interna && !esRutaDeFila(path)) {
            chain.doFilter(request, response);
            return;
        }
        String received = request.getHeader(HEADER);
        if (!tokenValido(received)) {
            log.warn("Acceso {} rechazado: path={} tokenPresente={}",
                    interna ? "interno" : "directo a la fila", path, received != null);
            rechazar(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean tokenValido(String received) {
        return !expectedToken.isEmpty() && received != null
                && MessageDigest.isEqual(
                        expectedToken.getBytes(StandardCharsets.UTF_8),
                        received.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean esRutaDeFila(String path) {
        return path != null && path.matches("/eventos/[^/]+/fila/.*");
    }

    private static void rechazar(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(
                "{\"error\":\"No autorizado\",\"message\":\"X-Internal-Token inválido\"}"
                        .getBytes(StandardCharsets.UTF_8));
    }
}
