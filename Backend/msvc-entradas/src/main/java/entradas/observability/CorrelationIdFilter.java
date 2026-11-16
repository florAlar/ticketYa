package entradas.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * E2-NFR-03: correlation id por request (cabecera {@code X-Correlation-Id}).
 *
 * <p>El gateway lo genera (o respeta el que llega si es válido) y lo reenvía a los servicios; cada
 * servicio lo coloca en el MDC (queda en cada línea de log) y lo devuelve en la respuesta. Las
 * llamadas Feign entre servicios lo propagan. Un id inválido (largo, caracteres raros) se descarta
 * y se genera uno nuevo para no inyectar texto arbitrario en los logs.
 *
 * <p>Además escribe una línea de acceso por request ({@code trace.http}) con método, ruta, estado y
 * duración, para seguir una operación entre servicios.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String MDC_USER = "usuario";

    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9._-]{8,64}");
    private static final Pattern USUARIO_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Logger ACCESS = LoggerFactory.getLogger("trace.http");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entrante = request.getHeader(HEADER);
        String id = entrante != null && VALIDO.matcher(entrante).matches()
                ? entrante
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, id);
        // Identidad que inyecta el gateway (ya verificada); solo para la bitácora.
        String usuario = request.getHeader("X-User-Id");
        if (usuario != null && USUARIO_VALIDO.matcher(usuario).matches()) {
            MDC.put(MDC_USER, usuario);
        }
        response.setHeader(HEADER, id);
        long t0 = System.nanoTime();
        try {
            chain.doFilter(new ConCorrelationId(request, id), response);
        } finally {
            if (!esRuidoso(request.getRequestURI())) {
                ACCESS.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), (System.nanoTime() - t0) / 1_000_000);
            }
            MDC.remove(MDC_KEY);
            MDC.remove(MDC_USER);
        }
    }

    private static boolean esRuidoso(String uri) {
        return uri != null
                && (uri.startsWith("/actuator") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui"));
    }

    /** La request que ve el resto de la cadena (y el proxy del gateway) lleva siempre el id vigente. */
    private static final class ConCorrelationId extends HttpServletRequestWrapper {
        private final String id;

        ConCorrelationId(HttpServletRequest request, String id) {
            super(request);
            this.id = id;
        }

        @Override
        public String getHeader(String name) {
            return HEADER.equalsIgnoreCase(name) ? id : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HEADER.equalsIgnoreCase(name) ? Collections.enumeration(List.of(id)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
            names.removeIf(HEADER::equalsIgnoreCase);
            names.add(HEADER);
            return Collections.enumeration(names);
        }
    }
}
