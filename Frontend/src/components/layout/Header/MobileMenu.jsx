import { NavLink } from 'react-router-dom'
import { HiX, HiOutlineUser } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'

const MobileMenu = ({ open, close }) => {
  const {
    isAuthenticated,
    user,
    logout,
    canManageEvents,
    canScanAccess,
    isAcceso,
  } = useAuth()

  const handleLogout = () => {
    logout()
    close()
  }

  return (
    <div className={`mobile-menu ${open ? 'show' : ''}`}>
      <button className="mobile-close" onClick={close} aria-label="Close menu">
        <HiX />
      </button>

      <nav>
        <ul className="mobile-links">
          <li><NavLink to="/" end onClick={close}>Inicio</NavLink></li>
          {isAuthenticated && !isAcceso && (
            <li>
              <NavLink to="/eventos" onClick={close}>Eventos</NavLink>
            </li>
          )}
          {!isAcceso && (
            <li>
              <NavLink to="/mis-entradas" onClick={close}>Mis entradas</NavLink>
            </li>
          )}
          {canManageEvents && (
            <li>
              <NavLink to="/gestion" onClick={close}>Gestión</NavLink>
            </li>
          )}
          {canScanAccess && (
            <li>
              <NavLink to="/acceso" onClick={close}>Acceso</NavLink>
            </li>
          )}
          {isAuthenticated ? (
            <>
              <li>
                <NavLink
                  to="/perfil"
                  className="nav-account"
                  onClick={close}
                  title={user?.nombre || user?.email || 'Mi perfil'}
                  aria-label={user?.nombre || 'Mi perfil'}
                >
                  <HiOutlineUser className="nav-account__icon" aria-hidden />
                </NavLink>
              </li>
              <li>
                <button type="button" className="nav-logout" onClick={handleLogout}>
                  Salir
                </button>
              </li>
            </>
          ) : (
            <li>
              <NavLink
                to="/login"
                className="nav-account"
                onClick={close}
                title="Ingresar"
                aria-label="Ingresar"
              >
                <HiOutlineUser className="nav-account__icon" aria-hidden />
              </NavLink>
            </li>
          )}
        </ul>
      </nav>
    </div>
  )
}

export default MobileMenu
