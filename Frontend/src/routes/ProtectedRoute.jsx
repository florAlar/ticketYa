import { Navigate, useLocation } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { hasRole } from '../auth/roles'
import RouteFallback from '../components/layout/RouteFallback'

/**
 * Rutas que requieren sesión y, opcionalmente, uno de `roles`.
 * Sin sesión → /login
 * Con sesión pero rol incorrecto → /sin-permiso
 *
 * Mock de paneles en front; ms-usuarios + gateway validan de verdad después.
 */
const ProtectedRoute = ({ children, roles }) => {
  const { isAuthenticated, initializing, user } = useAuth()
  const location = useLocation()
  const next = `${location.pathname}${location.search}`

  // Mientras validamos el token guardado no redirigimos: evitamos el flash
  // login → ruta protegida en cada recarga.
  if (initializing) {
    return <RouteFallback />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
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
