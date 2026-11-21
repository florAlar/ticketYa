import { useMemo } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { FaStar } from 'react-icons/fa'
import useAuth from '../../hooks/useAuth'
import LoginForm from '../../components/auth/LoginForm'
import RegisterForm from '../../components/auth/RegisterForm'
import DemoAccountsToast from '../../components/auth/DemoAccountsToast'
import OrganizadorInfo from '../../components/auth/OrganizadorInfo'
import RouteFallback from '../../components/layout/RouteFallback'
import { reopenDemoAccountsToast } from '../../auth/demoToast'
import fondoLogin from '../../assets/img/login background.jpg'
import './LoginPage.css'
import './orgTab.css'

/**
 * Tabs: Ingresar | Crear cuenta (sin /register aparte).
 * Registro público = solo CLIENTE. Org/admin/acceso: guía aparte.
 */
const LoginPage = () => {
  const { isAuthenticated, initializing } = useAuth()
  const [params, setParams] = useSearchParams()
  const tabParam = params.get('tab')
  const tab = tabParam === 'register' || tabParam === 'organizador' ? tabParam : 'login'

  const title = useMemo(
    () =>
      tab === 'register' ? 'Crear cuenta' : tab === 'organizador' ? '¿Sos organizador?' : 'Ingresar',
    [tab]
  )

  const selectTab = (next) => {
    const nextParams = new URLSearchParams(params)
    if (next === 'register' || next === 'organizador') nextParams.set('tab', next)
    else nextParams.delete('tab')
    setParams(nextParams)
  }

  if (initializing) {
    return <RouteFallback />
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return (
    <section className="auth-page section-v-padding">
      <div
        className="auth-page__bg"
        style={{ backgroundImage: `url(${fondoLogin})` }}
        aria-hidden
      />
      <div className="auth-page__scrim" aria-hidden />

      <div className="auth-box">
        <div className="auth-box__brand">
          <FaStar className="auth-box__star" aria-hidden />
          <span>TicketYa</span>
        </div>

        <div className="auth-box__titlebar">
          <h1 className={`auth-box__title${tab === 'organizador' ? ' auth-box__title--org' : ''}`}>{title}</h1>
          {tab !== 'organizador' && (
            <button
              type="button"
              className="auth-box__org-btn"
              onClick={() => selectTab('organizador')}
            >
              ¿Sos organizador?
            </button>
          )}
        </div>

        {tab === 'organizador' ? (
          <>
            <button type="button" className="auth-box__back" onClick={() => selectTab('login')}>
              ← Volver a ingresar
            </button>
            <OrganizadorInfo
              onCrearCuenta={() => selectTab('register')}
              onVerDemo={reopenDemoAccountsToast}
            />
          </>
        ) : (
          <>
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
          </>
        )}
      </div>

      <DemoAccountsToast />
    </section>
  )
}

export default LoginPage
