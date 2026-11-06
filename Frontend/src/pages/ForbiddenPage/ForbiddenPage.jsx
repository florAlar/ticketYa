import { Link, useLocation } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { ROLE_LABELS } from '../../auth/roles'
import '../GestionPage/GestionPage.css'

/** Rol insuficiente para la ruta pedida. */
const ForbiddenPage = () => {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()
  const from = location.state?.from || '/'
  const rolLabel = user?.rol ? ROLE_LABELS[user.rol] || user.rol : null

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <h1 className="gestion-page__title">Sin permiso</h1>
        <p className="gestion-page__sub">
          {isAuthenticated
            ? `Tu sesión (${rolLabel || 'sin rol'}) no puede abrir esta sección.`
            : 'Necesitás iniciar sesión.'}
        </p>
      </header>
      <p className="gestion-page__foot">
        <Link to={from}>Volver</Link>
        {' · '}
        <Link to="/">Inicio</Link>
        {!isAuthenticated && (
          <>
            {' · '}
            <Link to="/login">Ingresar</Link>
          </>
        )}
      </p>
    </section>
  )
}

export default ForbiddenPage
