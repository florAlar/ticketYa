import { Link } from 'react-router-dom'
import { FaStar } from 'react-icons/fa'
import heroCrowd from '../../assets/img/people-crowd-concert-audience-show-lights-stage-performance-event-entertainment-nightclub-performing-arts-live-performance-rock-concert-musical-theatre-1033034-3875179764.jpg'
import '../LoginPage/LoginPage.css'
import './CuentaRolesPage.css'

/**
 * Guía pública: cómo obtener cuenta ORGANIZADOR / ADMIN / ACCESO.
 * No es un formulario de alta (org formal / admin users = siguiente fase).
 */
const OrganizerAccountPage = () => {
  return (
    <section className="auth-page section-v-padding">
      <div
        className="auth-page__bg"
        style={{ backgroundImage: `url(${heroCrowd})` }}
        aria-hidden
      />
      <div className="auth-page__scrim" aria-hidden />

      <div className="auth-box auth-box--wide">
        <div className="auth-box__brand">
          <FaStar className="auth-box__star" aria-hidden />
          <span>TicketYa</span>
        </div>

        <h1 className="auth-box__title">¿Organizás eventos?</h1>
        <p className="auth-box__lead">
          Las cuentas de organizador no se crean con el registro público de
          clientes. Así se separan compradores de quien publica shows.
        </p>

        <ul className="cuenta-roles-list">
          <li>
            <strong>Cliente</strong>
            <span>
              Registro libre en{' '}
              <Link to="/login?tab=register">Crear cuenta cliente</Link>. Compra
              entradas y gestiona devoluciones propias.
            </span>
          </li>
          <li>
            <strong>Organizador</strong>
            <span>
              La plataforma (o un administrador) te habilita el rol. Después
              iniciás sesión y usás el panel <em>Gestión</em> para borradores,
              publicar y suspender tus eventos.
            </span>
          </li>
          <li>
            <strong>Administrador</strong>
            <span>
              Solo otro admin puede darte de alta o asignarte el rol. No hay
              auto-registro.
            </span>
          </li>
          <li>
            <strong>Personal de acceso</strong>
            <span>
              Alta exclusiva de un administrador (y asignación a eventos). Sirve
              para validar QR en puerta.
            </span>
          </li>
        </ul>

        <p className="auth-box__roles-note" style={{ marginTop: '1rem' }}>
          En el simulacro de la materia podés usar las cuentas demo del toast en
          login (org@ / admin@ / acceso@). En producción el alta org quedará
          como solicitud o alta interna.
        </p>

        <div className="gestion-card__actions" style={{ marginTop: '1.25rem' }}>
          <Link className="btn btn-primary" to="/login">
            Ir a ingresar
          </Link>
          <Link className="btn" to="/login?tab=register">
            Prefiero cuenta cliente
          </Link>
          <Link className="btn" to="/">
            Inicio
          </Link>
        </div>
      </div>
    </section>
  )
}

export default OrganizerAccountPage
