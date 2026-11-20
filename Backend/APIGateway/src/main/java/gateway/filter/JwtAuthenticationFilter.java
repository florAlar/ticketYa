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
 *
 * <p>El JWT es obligatorio: fuera de las rutas públicas, sin {@code Authorization: Bearer} el request
 * se rechaza con 401. Los headers {@code X-User-*} del cliente NUNCA se trustan: los sobreescribe
 * el token verificado (o se vacían si el token no trae identidad), de modo que no se puede
 * suplantar a otro usuario ni declararse ADMIN a mano.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_USER_EMAIL = "X-User-Email";
    public static final String HDR_USER_ROLE = "X-User-Role";
    public static final String HDR_USER_ROLES = "X-User-Roles";
    public static final String HDR_INTERNAL = "X-Internal-Token";

    private final JwtUtil jwtUtil;
    private final String internalToken;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            @Value("${ticketya.interno.token:}") String internalToken) {
        this.jwtUtil = jwtUtil;
        this.internalToken = internalToken == null ? "" : internalToken.trim();
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
            // Aun siendo pública, se vacían los headers X-User-* del cliente: nadie se anuncia
            // a sí mismo en una ruta sin token (ni en las líneas de log de los msvcs).
            filterChain.doFilter(sinIdentidad(request), response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");
        if (!hasBearer) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                    "Debe proporcionar un token JWT válido");
            return;
        }

        String token = authHeader.substring(7);

        // Solo el procesamiento del token queda dentro del try: una excepción del downstream
        // (proxy, conexión rechazada) no debe convertirse en un 401 "token inválido".
        String email;
        String userId;
        List<String> roles;
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired token for path: {}", path);
                writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                        "Token inválido o expirado");
                return;
            }

            email = jwtUtil.extractEmail(token);
            userId = jwtUtil.extractUserId(token);
            roles = jwtUtil.extractRoles(token);
        } catch (Exception e) {
            log.error("Error processing JWT: {}", e.getMessage());
            writeError(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                    "Error al procesar el token");
            return;
        }

        String primaryRol = roles.isEmpty() ? null : roles.get(0);
        log.debug("Token OK. User: {} (ID: {}), Roles: {}", email, userId, roles);

        if (!hasPermission(path, method, roles)) {
            log.warn("Access denied for user {} roles {} path: {}", email, roles, path);
            writeError(response, HttpStatus.FORBIDDEN, "No autorizado",
                    "No tiene permisos para acceder a este recurso");
            return;
        }

        MutableRequest wrapper = new MutableRequest(request);
        // La identidad sale SIEMPRE del token verificado. Si el token no trae un claim,
        // el header se vacía: nunca se conserva el que mandó el cliente.
        wrapper.putHeader(HDR_USER_ID, userId);
        wrapper.putHeader(HDR_USER_EMAIL, email);
        wrapper.putHeader(HDR_USER_ROLE, primaryRol);
        wrapper.putHeader(HDR_USER_ROLES, roles.isEmpty() ? null : String.join(",", roles));
        marcarComoGateway(wrapper, path);

        filterChain.doFilter(wrapper, response);
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
                && (path.matches("/api/eventos/[^/]+/recomendaciones")
                        || path.equals("/api/eventos/disponibilidad")
                        || path.matches("/api/eventos/[^/]+/disponibilidad"))) {
            return true;
        }
        // E2-FILA-01: el estado general de la fila es público (no expone identidades).
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/eventos/[^/]+/fila/estado")) {
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
        // PAR-01: editar y eliminar participantes es solo ADMIN (el organizador puede darlos de alta: POST)
        if (path.matches("/api/participantes/[^/]+")
                && ("PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))) {
            return true;
        }
        // LUG-01/LUG-04: alta, edición y baja de lugares y sectores son solo ADMIN (el organizador solo los consulta)
        if ((path.startsWith("/api/lugares") || path.startsWith("/api/sectores")) && !"GET".equals(method)) {
            return true;
        }
        if (path.equals("/api/lugares/geocodificar")) {
            return true;
        }
        // FIL-02: fijar la tanda es operación de ADMIN; ventas vuelve a verificar X-User-Role.
        if (path.matches("/api/eventos/[^/]+/fila/habilitacion") && "POST".equals(method)) {
            return true;
        }
        // Notificaciones administrativas: alta manual y consulta de estado son solo ADMIN
        // (la bandeja propia vive bajo /api/notificaciones/me/**).
        if (path.equals("/api/notificaciones") && "POST".equals(method)) {
            return true;
        }
        if (path.matches("/api/notificaciones/[^/]+/estado") && "GET".equals(method)) {
            return true;
        }
        // Endpoints internos (Feign entre msvcs): por el gateway solo se permiten a ADMIN.
        if (path.matches("/api/eventos/[^/]+/cupos(/liberacion)?") && "POST".equals(method)) {
            return true;
        }
        if (path.matches("/api/eventos/[^/]+/reintegro-cancelacion") && "POST".equals(method)) {
            return true;
        }
        if (path.matches("/api/eventos/[^/]+/entradas/resumen") && "GET".equals(method)) {
            return true;
        }
        if (path.equals("/api/entradas/emision") && "POST".equals(method)) {
            return true;
        }
        if (path.equals("/api/preferencias/clientes/matching") && "POST".equals(method)) {
            return true;
        }
        if (path.matches("/api/preferencias/clientes/[^/]+/notificaciones") && "GET".equals(method)) {
            return true;
        }
        if (path.matches("/api/compras/evento/[^/]+") && "GET".equals(method)) {
            return true;
        }
        return false;
    }

    private boolean roleAllows(String path, String method, String rol) {
        // Bandeja propia de notificaciones: cualquier rol autenticado; el msvc valida dueño/ADMIN.
        if (isSelfPath(path, "/api/notificaciones/me")) {
            return true;
        }
        // Preferencias y recomendaciones propias: cualquier rol autenticado (un ORG/ACCESO
        // también puede comprar y tener gustos). El msvc filtra por X-User-Id.
        if (path.startsWith("/api/preferencias") || path.startsWith("/api/recomendaciones")) {
            return true;
        }
        return switch (rol) {
            case "ORGANIZADOR" -> isOrganizadorAccess(path, method);
            case "CLIENTE" -> isClienteAccess(path, method);
            case "PERSONAL_ACCESO" -> isAccesoAccess(path, method);
            default -> false;
        };
    }

    private boolean isOrganizadorAccess(String path, String method) {
        // Control de acceso QR: ORGANIZADOR solo consulta validaciones (GET); escanear (POST) es PERSONAL_ACCESO/ADMIN.
        if (path.matches("/api/eventos/[^/]+/validaciones")) {
            return "GET".equals(method);
        }
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
        if (isSelfPath(path, "/api/usuarios/me")) {
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
        if (isSelfPath(path, "/api/usuarios/me") || path.startsWith("/api/auth/")) {
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
        // Validaciones de acceso QR no son visibles para CLIENTE (solo ORGANIZADOR dueño / PERSONAL_ACCESO / ADMIN).
        if (path.matches("/api/eventos/[^/]+/validaciones")) {
            return false;
        }
        if (path.startsWith("/api/eventos") && "GET".equals(method)) {
            return true;
        }
        return false;
    }

    private boolean isAccesoAccess(String path, String method) {
        if (isSelfPath(path, "/api/usuarios/me") || path.startsWith("/api/auth/")) {
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

    private static boolean isSelfPath(String path, String base) {
        return base.equals(path) || path.startsWith(base + "/");
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

    /** Request sin identidad: los headers X-User-* del cliente quedan vacíos. */
    private MutableRequest sinIdentidad(HttpServletRequest request) {
        MutableRequest wrapper = new MutableRequest(request);
        wrapper.putHeader(HDR_USER_ID, null);
        wrapper.putHeader(HDR_USER_EMAIL, null);
        wrapper.putHeader(HDR_USER_ROLE, null);
        wrapper.putHeader(HDR_USER_ROLES, null);
        marcarComoGateway(wrapper, request.getRequestURI());
        return wrapper;
    }

    /**
     * Sella las rutas de la fila con el token compartido: msvc-ventas rechaza llamadas directas
     * a {@code /eventos/{id}/fila/**} que no vengan del gateway (no alcanza con forjar X-User-*).
     * En el resto de las rutas el header se vacía: el token del cliente nunca se propaga.
     */
    private void marcarComoGateway(MutableRequest wrapper, String path) {
        boolean esFila = !internalToken.isEmpty() && path != null
                && path.matches("/api/eventos/[^/]+/fila/.*");
        wrapper.putHeader(HDR_INTERNAL, esFila ? internalToken : null);
    }

    private static final class MutableRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String> custom = new java.util.HashMap<>();

        MutableRequest(HttpServletRequest request) {
            super(request);
        }

        /** Un valor {@code null} vacía el header: el del cliente no se conserva. */
        void putHeader(String name, String value) {
            String existente = claveCustom(name);
            if (existente != null) {
                custom.remove(existente);
            }
            custom.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String clave = claveCustom(name);
            if (clave != null) {
                return custom.get(clave);
            }
            return super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            String clave = claveCustom(name);
            if (clave != null) {
                String v = custom.get(clave);
                return v == null
                        ? java.util.Collections.emptyEnumeration()
                        : java.util.Collections.enumeration(List.of(v));
            }
            return super.getHeaders(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.Set<String> names = new java.util.HashSet<>();
            java.util.Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String nombre = original.nextElement();
                // Un header del cliente que colisiona (en cualquier capitalización) con uno
                // controlado por el gateway se descarta: evita duplicados tipo x-user-id.
                if (claveCustom(nombre) == null) {
                    names.add(nombre);
                }
            }
            names.addAll(custom.keySet());
            return java.util.Collections.enumeration(names);
        }

        /** Búsqueda case-insensitive: los nombres de header HTTP no distinguen mayúsculas. */
        private String claveCustom(String name) {
            if (name == null) {
                return null;
            }
            for (String clave : custom.keySet()) {
                if (clave.equalsIgnoreCase(name)) {
                    return clave;
                }
            }
            return null;
        }
    }
}
