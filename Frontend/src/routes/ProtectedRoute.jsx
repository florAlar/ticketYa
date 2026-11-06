import { Navigate, useLocation } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { hasRole } from '../auth/roles'

/**
 * Rutas que requieren sesión y, opcionalmente, uno de `roles`.
 * Sin sesión → /login?next=
 * Con sesión pero rol incorrecto → /sin-permiso
 *
 * Mock de paneles en front; ms-usuarios + gateway validan de verdad después.
 */
const ProtectedRoute = ({ children, roles }) => {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()
  const next = `${location.pathname}${location.search}`

  if (!isAuthenticated) {
    return (
      <Navigate
        to={`/login?next=${encodeURIComponent(next)}`}
        replace
      />
    )
  }

  if (Array.isArray(roles) && roles.length > 0) {
    const allowed = hasRole(user, ...roles)
    if (!allowed) {
      return (
        <Navigate
          to="/sin-permiso"
          replace
          state={{ from: next }}
        />
      )
    }
  }

  return children
}

export default ProtectedRoute
