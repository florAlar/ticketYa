import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { isEntradaReintegroCancelacion } from '../../domain/entrada'
import EntradaCard from './EntradaCard'
import DevolucionesPanel from './DevolucionesPanel'
import useMisEntradas from './useMisEntradas'
import '../PerfilPage/PerfilPage.css'
import './MisEntradasPage.css'

const MisEntradasPage = () => {
  const { user, token } = useAuth()
  const [tab, setTab] = useState('proximas')
  const {
    status,
    error,
    reload,
    listado,
    solicitudes,
    pendientePorEntrada,
    tramitePorEntrada,
  } = useMisEntradas({ user, token })

  const { proximas, canceladas } = useMemo(
    () => ({
      proximas: listado.filter((e) => !isEntradaReintegroCancelacion(e)),
      canceladas: listado.filter((e) => isEntradaReintegroCancelacion(e)),
    }),
    [listado]
  )

  const tabs = [
    { id: 'proximas', label: 'Próximas', count: proximas.length },
    ...(canceladas.length > 0
      ? [{ id: 'canceladas', label: 'Canceladas', count: canceladas.length }]
      : []),
    { id: 'devoluciones', label: 'Devoluciones', count: solicitudes.length },
  ]
  const activa = tabs.some((t) => t.id === tab) ? tab : 'proximas'
  const items = activa === 'canceladas' ? canceladas : proximas

  return (
    <section className="mis-entradas-page container-slim section-v-padding">
      <header className="mis-entradas-page__head">
        <h1 className="mis-entradas-page__title">Mis entradas</h1>
        <p className="mis-entradas-page__sub">
          Tus entradas{user?.email ? ` (${user.email})` : ''}. El QR está en el
          detalle de cada una.
        </p>
      </header>

      {status === 'loading' && <LoadingState label="Cargando entradas…" />}

      {status === 'error' && <ErrorState error={error} onRetry={reload} />}

      {status === 'success' && (
        <>
          <div className="perfil-tabs" role="tablist" aria-label="Mis entradas">
            {tabs.map((t) => (
              <button
                key={t.id}
                type="button"
                role="tab"
                aria-selected={activa === t.id}
                className={`perfil-tabs__tab${activa === t.id ? ' is-active' : ''}`}
                onClick={() => setTab(t.id)}
              >
                {t.label}
                {t.count > 0 ? ` (${t.count})` : ''}
              </button>
            ))}
          </div>

          {activa === 'devoluciones' ? (
            <DevolucionesPanel solicitudes={solicitudes} />
          ) : items.length === 0 ? (
            <EmptyState
              title="No tenés entradas para mostrar."
              action={
                <Link className="btn btn-primary" to="/eventos">
                  Ver eventos
                </Link>
              }
            >
              Cuando compres, aparecen acá. Las usadas quedan fuera del listado.
            </EmptyState>
          ) : (
            <ul className="mis-entradas-list">
              {items.map((e) => (
                <EntradaCard
                  key={e.id}
                  entrada={e}
                  pendiente={pendientePorEntrada.get(e.id)}
                  tramite={tramitePorEntrada.get(e.id)}
                  onVerDevoluciones={() => setTab('devoluciones')}
                />
              ))}
            </ul>
          )}
        </>
      )}

      <p className="mis-entradas-page__foot">
        <Link to="/soporte">Soporte</Link>
        {' · '}
        <button type="button" className="mis-entradas-page__linkish" onClick={reload}>
          Actualizar
        </button>
      </p>
    </section>
  )
}

export default MisEntradasPage
