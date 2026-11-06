package gateway.filter;

/**
 * @deprecated Usar {@link JwtAuthenticationFilter} (auth JWT + roles + X-User-*).
 * Se mantiene el nombre de paquete histórico vacío a propósito.
 */
@Deprecated
public final class AuthFilter {

    private AuthFilter() {
    }
}
