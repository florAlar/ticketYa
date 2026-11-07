import { NavLink } from 'react-router-dom'
import { HiOutlineUser } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'

const DesktopNav = () => {
  const {
    isAuthenticated,
    user,
    logout,
    canManageEvents,
    canScanAccess,
    isAcceso,
  } = useAuth()

  const accountTitle = user?.nombre
    ? `${user.nombre}${user.rol ? ` (${user.rol})` : ''}`
    : user?.email || 'Mi perfil'

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
        {isAuthenticated ? (
          <>
            <li>
              <NavLink
                to="/perfil"
                className="nav-account"
                title={accountTitle}
                aria-label={user?.nombre || 'Mi perfil'}
              >
                <HiOutlineUser className="nav-account__icon" aria-hidden />
              </NavLink>
            </li>
            <li>
              <button type="button" className="nav-logout" onClick={logout}>
                Salir
              </button>
            </li>
          </>
        ) : (
          <li>
            <NavLink
              to="/login"
              className="nav-account"
              title="Ingresar"
              aria-label="Ingresar"
            >
              <HiOutlineUser className="nav-account__icon" aria-hidden />
            </NavLink>
          </li>
        )}
      </ul>
    </nav>
  )
}

export default DesktopNav
