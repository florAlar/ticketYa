import { useMemo } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { FaStar } from 'react-icons/fa'
import useAuth from '../../hooks/useAuth'
import LoginForm from '../../components/auth/LoginForm'
import RegisterForm from '../../components/auth/RegisterForm'
import DemoAccountsToast, {
  reopenDemoAccountsToast,
} from '../../components/auth/DemoAccountsToast'
import heroCrowd from '../../assets/img/people-crowd-concert-audience-show-lights-stage-performance-event-entertainment-nightclub-performing-arts-live-performance-rock-concert-musical-theatre-1033034-3875179764.jpg'
import './LoginPage.css'

/**
 * Tabs: Ingresar | Crear cuenta (sin /register aparte).
 * Registro público = solo CLIENTE. Org/admin/acceso: guía aparte.
 */
const LoginPage = () => {
  const { isAuthenticated } = useAuth()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'register' ? 'register' : 'login'

  const title = useMemo(
    () => (tab === 'register' ? 'Crear cuenta' : 'Ingresar'),
    [tab]
  )

  const selectTab = (next) => {
    const nextParams = new URLSearchParams(params)
    if (next === 'register') nextParams.set('tab', 'register')
    else nextParams.delete('tab')
    setParams(nextParams)
  }

  if (isAuthenticated) {
    const next = params.get('next') || '/'
    return <Navigate to={next} replace />
  }

  return (
    <section className="auth-page section-v-padding">
      <div
        className="auth-page__bg"
        style={{ backgroundImage: `url(${heroCrowd})` }}
        aria-hidden
      />
      <div className="auth-page__scrim" aria-hidden />

      <div className="auth-box">
        <div className="auth-box__brand">
          <FaStar className="auth-box__star" aria-hidden />
          <span>TicketYa</span>
        </div>

        <h1 className="auth-box__title">{title}</h1>

        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'login'}
            className={tab === 'login' ? 'is-active' : ''}
            onClick={() => selectTab('login')}
          >
            Ingresar
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'register'}
            className={tab === 'register' ? 'is-active' : ''}
            onClick={() => selectTab('register')}
          >
            Crear cuenta
          </button>
        </div>

        {tab === 'login' ? <LoginForm /> : <RegisterForm />}

        <div className="auth-box__roles">
          <p className="auth-box__roles-title">¿Sos organizador?</p>
          <p className="auth-box__roles-note">
            Primero creás una cuenta y después lo gestionás desde el perfil de
            usuario.
          </p>
          <p className="auth-box__roles-note">
            Administrador y personal de acceso no se registran acá: un admin de
            la plataforma tiene que darte de alta.
          </p>
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

export default LoginPage
