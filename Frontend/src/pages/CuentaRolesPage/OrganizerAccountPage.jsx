import { Link } from 'react-router-dom'
import { FaStar } from 'react-icons/fa'
import DemoAccountsToast, {
  reopenDemoAccountsToast,
} from '../../components/auth/DemoAccountsToast'
import heroCrowd from '../../assets/img/people-crowd-concert-audience-show-lights-stage-performance-event-entertainment-nightclub-performing-arts-live-performance-rock-concert-musical-theatre-1033034-3875179764.jpg'
import '../LoginPage/LoginPage.css'
import './CuentaRolesPage.css'

/**
 * Guía: cómo se obtienen roles ORG / ADMIN / ACCESO.
 * CTA único → registro cliente (/login?tab=register).
 * Demo toast para probar seeds sin auto-alta org.
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
          No hay alta pública de organizador. Primero creás una cuenta cliente;
          el rol de organizador lo habilita la plataforma (o un admin).
        </p>

        <ul className="cuenta-roles-list">
          <li>
            <strong>Cliente</strong>
            <span>Registro libre. Comprás entradas y gestionás tus trámites.</span>
          </li>
          <li>
            <strong>Organizador</strong>
            <span>
              Se otorga sobre una cuenta ya existente. Después usás Gestión para
              tus eventos.
            </span>
          </li>
          <li>
            <strong>Administrador / Acceso</strong>
            <span>Solo alta interna de un admin (sin auto-registro).</span>
          </li>
        </ul>

        <div className="cuenta-roles-footer">
          <Link
            className="auth-box__roles-cta"
            to="/login?tab=register"
          >
            Registrarme en la plataforma
          </Link>
          <button
            type="button"
            className="auth-box__demo-link"
            onClick={() => reopenDemoAccountsToast()}
          >
            Ver cuentas demo (email · contraseña)
          </button>
        </div>
      </div>

      <DemoAccountsToast />
    </section>
  )
}

export default OrganizerAccountPage
