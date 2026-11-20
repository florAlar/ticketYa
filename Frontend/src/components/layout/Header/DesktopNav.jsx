import { NavLink } from 'react-router-dom'
import useAuth from '../../../hooks/useAuth'

/** Links de navegación del header (los íconos de cuenta viven en HeaderActions). */
const DesktopNav = () => {
  const { isAuthenticated, canManageEvents, canScanAccess, isAcceso } = useAuth()

  return (
    <nav className="desktop-nav">
      <ul className="nav-links">
        <li><NavLink to="/" end>Inicio</NavLink></li>
        {/* Catálogo JWT: visible solo logueado (no anónimo). Acceso también SearchBar. */}
        {isAuthenticated && !isAcceso && (
          <li><NavLink to="/eventos">Eventos</NavLink></li>
        )}
        {!isAcceso && (
          <li><NavLink to="/mis-entradas">Mis entradas</NavLink></li>
        )}
        {canManageEvents && (
          <li><NavLink to="/gestion">Gestión</NavLink></li>
        )}
        {canScanAccess && (
          <li><NavLink to="/acceso">Acceso</NavLink></li>
        )}
      </ul>
    </nav>
  )
}

export default DesktopNav
