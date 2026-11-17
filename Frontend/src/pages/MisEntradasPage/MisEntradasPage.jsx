import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/AsyncState'
import EntradaCard from './EntradaCard'
import DevolucionesPanel from './DevolucionesPanel'
import useMisEntradas from './useMisEntradas'
import './MisEntradasPage.css'

const MisEntradasPage = () => {
  const { user, token } = useAuth()
  const [showDevoluciones, setShowDevoluciones] = useState(false)
  const {
    status,
    error,
    reload,
    listado,
    solicitudes,
    pendientePorEntrada,
    tramitePorEntrada,
  } = useMisEntradas({ user, token })

  const revealDevoluciones = useCallback(() => {
    setShowDevoluciones(true)
    requestAnimationFrame(() => {
      document.getElementById('mis-devoluciones')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    })
  }, [])

  return (
    <section className="mis-entradas-page container-slim section-v-padding">
      <header className="mis-entradas-page__head">
        <h1 className="mis-entradas-page__title">Mis entradas</h1>
        <p className="mis-entradas-page__sub">
          Entradas de tu cuenta
          {user?.email ? ` (${user.email})` : ''}. El QR solo en tickets válidos.
          Si el evento se suspende, tu entrada sigue vigente (con aviso). Si se
          cancela, queda acá sin QR y el reintegro en devoluciones.
        </p>
      </header>

      {status === 'loading' && <LoadingState label="Cargando entradas…" />}

      {status === 'error' && <ErrorState error={error} onRetry={reload} />}

      {status === 'success' && listado.length === 0 && (
        <EmptyState
          title="No tenés entradas para mostrar."
          action={
            <Link className="btn btn-primary" to="/eventos">
              Ver eventos
            </Link>
          }
        >
          Cuando compres, aparecen acá. Las usadas quedan fuera del listado
          principal.
        </EmptyState>
      )}

      {listado.length > 0 && (
        <ul className="mis-entradas-list">
          {listado.map((e) => (
            <EntradaCard
              key={e.id}
              entrada={e}
              pendiente={pendientePorEntrada.get(e.id)}
              tramite={tramitePorEntrada.get(e.id)}
              onVerDevoluciones={revealDevoluciones}
            />
          ))}
        </ul>
      )}

      <div className="mis-entradas-devo-toggle">
        <button
          type="button"
          className="btn"
          onClick={() => setShowDevoluciones((v) => !v)}
          aria-expanded={showDevoluciones}
        >
          {showDevoluciones ? 'Ocultar devoluciones' : 'Ver devoluciones'}
          {solicitudes.length > 0 ? ` (${solicitudes.length})` : ''}
        </button>
      </div>

      {showDevoluciones && <DevolucionesPanel solicitudes={solicitudes} />}

      <p className="mis-entradas-page__foot">
        <Link to="/soporte">Soporte</Link>
        {' · '}
        <button
          type="button"
          className="mis-entradas-page__linkish"
          onClick={reload}
        >
          Actualizar
        </button>
      </p>
    </section>
  )
}

export default MisEntradasPage
