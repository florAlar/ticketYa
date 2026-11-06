import { useMemo } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { FaStar } from 'react-icons/fa'
import useAuth from '../../hooks/useAuth'
import LoginForm from '../../components/auth/LoginForm'
import RegisterForm from '../../components/auth/RegisterForm'
import DemoAccountsToast from '../../components/auth/DemoAccountsToast'
import heroCrowd from '../../assets/img/people-crowd-concert-audience-show-lights-stage-performance-event-entertainment-nightclub-performing-arts-live-performance-rock-concert-musical-theatre-1033034-3875179764.jpg'
import './LoginPage.css'

/**
 * Tabs: Ingresar | Registrarse (sin /register aparte).
 * Mock local por defecto; gateway :8080 con VITE_USE_MOCK=false.
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
        <p className="auth-box__lead">
          {tab === 'login'
            ? 'Accedé a tus entradas y compras.'
            : 'Registrate para comprar y gestionar entradas.'}
        </p>

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
            Registrarse
          </button>
        </div>

        {tab === 'login' ? <LoginForm /> : <RegisterForm />}
      </div>

      <DemoAccountsToast />
    </section>
  )
}

export default LoginPage
