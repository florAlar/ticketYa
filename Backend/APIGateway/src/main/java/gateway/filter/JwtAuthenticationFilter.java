package gateway.filter;

import gateway.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro global de auth (equivalente al GlobalFilter del gatewayejemplo, en servlet/MVC).
 * - Rutas públicas sin token
 * - Bearer JWT → validar → roles TicketYa → headers X-User-* hacia msvcs (E1-GW-02)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_USER_EMAIL = "X-User-Email";
    public static final String HDR_USER_ROLE = "X-User-Role";
    public static final String HDR_USER_ROLES = "X-User-Roles";

    private final JwtUtil jwtUtil;
    private final boolean jwtEnabled;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this(jwtUtil, true);
    }

    /**
     * jwtEnabled=false (Entrega 1): sin Bearer, el request pasa con los headers X-User-* del cliente
     * (identificación por encabezado). Con Bearer se valida igual. Entrega 2: ticketya.security.jwt-enabled=true.
     */
    @Autowired
    public JwtAuthenticationFilter(
            JwtUtil jwtUtil, @Value("${ticketya.security.jwt-enabled:true}") boolean jwtEnabled) {
        this.jwtUtil = jwtUtil;
        this.jwtEnabled = jwtEnabled;
        if (!jwtEnabled) {
            log.warn("JWT DESHABILITADO (ticketya.security.jwt-enabled=false): identificación por headers X-User-*. "
                    + "Solo para Entrega 1 / pruebas.");
        }
    }

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-ui.html",
            "/webjars/",
            "/api-docs");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Processing request: {} {}", method, path);

        if (isPublicPath(path, method)) {
            log.debug("Public path, skipping authentication: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");
        if (!jwtEnabled && !hasBearer) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!hasBearer) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                    "Debe proporcionar un token JWT válido");
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired token for path: {}", path);
                writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                        "Token inválido o expirado");
                return;
            }

            String email = jwtUtil.extractEmail(token);
            String userId = jwtUtil.extractUserId(token);
            List<String> roles = jwtUtil.extractRoles(token);
            String primaryRol = roles.isEmpty() ? null : roles.get(0);

            log.debug("Token OK. User: {} (ID: {}), Roles: {}", email, userId, roles);

            if (!hasPermission(path, method, roles)) {
                log.warn("Access denied for user {} roles {} path: {}", email, roles, path);
                writeError(response, HttpStatus.FORBIDDEN, "No autorizado",
                        "No tiene permisos para acceder a este recurso");
                return;
            }

            MutableRequest wrapper = new MutableRequest(request);
            if (userId != null) {
                wrapper.putHeader(HDR_USER_ID, userId);
            }
            if (email != null) {
                wrapper.putHeader(HDR_USER_EMAIL, email);
            }
            if (primaryRol != null) {
                wrapper.putHeader(HDR_USER_ROLE, primaryRol);
            }
            if (!roles.isEmpty()) {
                wrapper.putHeader(HDR_USER_ROLES, String.join(",", roles));
            }

            filterChain.doFilter(wrapper, response);

        } catch (Exception e) {
            log.error("Error processing JWT: {}", e.getMessage());
            writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                    "Error al procesar el token");
        }
    }

    private boolean isPublicPath(String path, String method) {
        if ("/".equals(path) || path.isEmpty()) {
            return true;
        }
        for (String p : PUBLIC_PREFIXES) {
            if (path.startsWith(p)) {
                return true;
            }
        }
        if ("GET".equalsIgnoreCase(method) && isPublicEventoRead(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)
                && (path.equals("/api/recomendaciones")
                        || path.startsWith("/api/recomendaciones/")
                        || path.matches("/api/eventos/[^/]+/recomendaciones")
                        || path.equals("/api/eventos/disponibilidad")
                        || path.matches("/api/eventos/[^/]+/disponibilidad"))) {
            return true;
        }
        return false;
    }

    private boolean isPublicEventoRead(String path) {
        if ("/api/eventos".equals(path)) {
            return true;
        }
        if ("/api/participantes".equals(path) || path.matches("/api/participantes/[^/]+")) {
            return true;
        }
        if (path.matches("/api/eventos/[^/]+/participantes")) {
            return true;
        }
        return path.matches("/api/eventos/[^/]+");
    }

    private boolean hasPermission(String path, String method, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        if (roles.contains("ADMIN")) {
            return true;
        }
        if (isAdminOnly(path, method)) {
            return false;
        }
        for (String rol : roles) {
            if (roleAllows(path, method, rol)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminOnly(String path, String method) {
        if (path.equals("/api/usuarios") && "GET".equals(method)) {
            return true;
        }
        if (path.matches("/api/usuarios/[^/]+/estado") && "PATCH".equals(method)) {
            return true;
        }
        if (path.matches("/api/usuarios/[^/]+/roles")
                && ("GET".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
            return true;
        }
        // PATCH estado: ORG puede SUSPENDIDO (lo filtra msvc-eventos); CANCELADO/etc = ADMIN allí.
        // No bloquear acá todo el path o el front org ve botones que siempre fallan.
        if (path.matches("/api/devoluciones/[^/]+/(aprobacion|rechazo)") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/api/reportes") && "DELETE".equals(method)) {
            return true;
        }
        return false;
    }

    private boolean roleAllows(String path, String method, String rol) {
        return switch (rol) {
            case "ORGANIZADOR" -> isOrganizadorAccess(path, method);
            case "CLIENTE" -> isClienteAccess(path, method);
            case "PERSONAL_ACCESO" -> isAccesoAccess(path, method);
            default -> false;
        };
    }

    private boolean isOrganizadorAccess(String path, String method) {
        if (path.startsWith("/api/eventos")
                || path.startsWith("/api/lugares")
                || path.startsWith("/api/sectores")
                || path.startsWith("/api/participantes")
                || path.startsWith("/api/organizadores")) {
            return true;
        }
        if (path.startsWith("/api/reportes")) {
            return true;
        }
        if (path.matches("/api/eventos/[^/]+/validaciones") && "GET".equals(method)) {
            return true;
        }
        if (path.startsWith("/api/usuarios/me")) {
            return true;
        }
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        // Misma persona puede comprar/ver entradas propias (panel Mis entradas)
        if (isBuyerSelfService(path, method)) {
            return true;
        }
        return false;
    }

    private boolean isClienteAccess(String path, String method) {
        if (path.startsWith("/api/usuarios/me") || path.startsWith("/api/auth/")) {
            return true;
        }
        if (isBuyerSelfService(path, method)) {
            return true;
        }
        if ((path.equals("/api/eventos/disponibilidad")
                        || path.matches("/api/eventos/[^/]+/disponibilidad"))
                && "GET".equals(method)) {
            return true;
        }
        if (path.startsWith("/api/eventos/") && path.contains("/fila")) {
            return true;
        }
        if (path.startsWith("/api/preferencias") || path.startsWith("/api/clientes")) {
            return true;
        }
        if (path.startsWith("/api/eventos") && "GET".equals(method)) {
            return true;
        }
        return false;
    }

    private boolean isAccesoAccess(String path, String method) {
        if (path.startsWith("/api/usuarios/me") || path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.matches("/api/eventos/[^/]+/validaciones")
                && ("POST".equals(method) || "GET".equals(method))) {
            return true;
        }
        // Entradas propias si también compró (Mis entradas en UI si se habilita)
        if (isBuyerSelfService(path, method)) {
            return true;
        }
        return false;
    }

    /**
     * Flujos de comprador sobre recursos propios (msvc filtra por X-User-Id).
     * Aplica a CLIENTE y también a ORG/ACCESO que usan la misma cuenta.
     */
    private boolean isBuyerSelfService(String path, String method) {
        if (path.startsWith("/api/reservas")
                || path.startsWith("/api/compras")
                || path.startsWith("/api/pagos")
                || path.startsWith("/api/entradas")
                || path.startsWith("/api/devoluciones")) {
            return true;
        }
        if ((path.equals("/api/eventos/disponibilidad")
                        || path.matches("/api/eventos/[^/]+/disponibilidad"))
                && "GET".equals(method)) {
            return true;
        }
        return false;
    }

    private static void writeError(
            HttpServletResponse response, HttpStatus status, String error, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", error, message);
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MutableRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String> custom = new java.util.HashMap<>();

        MutableRequest(HttpServletRequest request) {
            super(request);
        }

        void putHeader(String name, String value) {
            custom.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String v = custom.get(name);
            return v != null ? v : super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if (custom.containsKey(name)) {
                return java.util.Collections.enumeration(List.of(custom.get(name)));
            }
            return super.getHeaders(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.Set<String> names = new java.util.HashSet<>();
            java.util.Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            names.addAll(custom.keySet());
            return java.util.Collections.enumeration(names);
        }
    }
}
