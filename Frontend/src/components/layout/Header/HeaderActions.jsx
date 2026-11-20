import { NavLink, useNavigate } from 'react-router-dom'
import { HiOutlineLogout, HiOutlineUser } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'
import ThemeToggle from './ThemeToggle'
import NotificationBell from './NotificationBell'

/**
 * Todos los íconos del header juntos, en el extremo derecho:
 * tema · notificaciones · cuenta · salir.
 */
const HeaderActions = () => {
  const navigate = useNavigate()
  const { isAuthenticated, user, logout } = useAuth()

  const handleLogout = async () => {
    await logout()
    // Al salir siempre volvemos al login limpio (sin ?next=).
    navigate('/login', { replace: true })
  }

  const accountTitle = isAuthenticated
    ? user?.nombre
      ? `${user.nombre}${user.rol ? ` (${user.rol})` : ''}`
      : user?.email || 'Mi perfil'
    : 'Ingresar'

  return (
    <div className="header-actions">
      <ThemeToggle />
      <NotificationBell />
      <NavLink
        to={isAuthenticated ? '/perfil' : '/login'}
        className="header-icon header-actions__account"
        title={accountTitle}
        aria-label={isAuthenticated ? user?.nombre || 'Mi perfil' : 'Ingresar'}
      >
        <HiOutlineUser aria-hidden />
      </NavLink>
      {isAuthenticated && (
        <button
          type="button"
          className="header-icon header-actions__logout"
          onClick={handleLogout}
          title="Salir"
          aria-label="Salir"
        >
          <HiOutlineLogout aria-hidden />
        </button>
      )}
    </div>
  )
}

export default HeaderActions
