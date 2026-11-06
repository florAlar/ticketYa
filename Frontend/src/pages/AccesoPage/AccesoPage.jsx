import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { ROLE_LABELS } from '../../auth/roles'
import '../GestionPage/GestionPage.css'

/**
 * Hub personal de acceso (ingreso al evento).
 * Mock de panel; validación real = ms-entradas vía gateway.
 */
const AccesoPage = () => {
  const { user } = useAuth()
  const rolLabel = ROLE_LABELS[user?.rol] || user?.rol

  return (
    <section className="gestion-page container section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">Ingreso al evento</p>
        <h1 className="gestion-page__title">Personal de acceso</h1>
        <p className="gestion-page__sub">
          Sesión: <strong>{user?.nombre || user?.email}</strong>
          {rolLabel ? ` · ${rolLabel}` : ''}
        </p>
      </header>

      <div className="gestion-page__grid">
        <article className="gestion-card">
          <h2>Escanear / validar QR</h2>
          <p>
            En la puerta del evento: un solo ingreso por entrada. El backend
            (ms-entradas) confirma; acá solo mock de acceso al panel.
          </p>
          <Link className="btn btn-primary" to="/acceso/scan">
            Ir a escanear
          </Link>
        </article>

        <article className="gestion-card">
          <h2>Fuera de alcance</h2>
          <p>
            Este rol no crea eventos ni compra entradas desde el panel de
            acceso. Catálogo y gestión quedan ocultos en la nav.
          </p>
        </article>
      </div>

      <p className="gestion-page__foot">
        <Link to="/">← Inicio</Link>
      </p>
    </section>
  )
}

export default AccesoPage
