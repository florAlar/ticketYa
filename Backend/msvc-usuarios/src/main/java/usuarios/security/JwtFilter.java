package usuarios.security;

/**
 * @deprecated Auth de request en API Gateway ({@code JwtAuthenticationFilter}).
 * Emisión de tokens: {@link usuarios.util.JwtUtil} + {@link usuarios.service.AuthService}.
 */
@Deprecated
public final class JwtFilter {

    private JwtFilter() {
    }
}
