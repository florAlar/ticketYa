import { useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { FEATURE } from '../../config/featureGates'
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/AsyncState'
import NotificacionesPrefs from './NotificacionesPrefs'
import NotificacionCard from './NotificacionCard'
import { useNotificaciones } from './useNotificaciones'
import './NotificacionesPage.css'

const TABS = [
  { id: 'todas', label: 'Todas' },
  { id: 'no-leidas', label: 'No leídas' },
]

export default function NotificacionesPage() {
  const { token } = useAuth()
  const [tab, setTab] = useState('todas')
  const enabled = Boolean(token) && FEATURE.notificaciones
  const {
    items,
    status,
    error,
    noLeidas,
    page,
    totalPages,
    loadingMore,
    markingAll,
    cargarMas,
    markRead,
    markAllVisible,
    reload,
  } = useNotificaciones({ token, tab, enabled })

  if (!FEATURE.notificaciones) {
    return (
      <section className="notificaciones-page container-slim section-v-padding">
        <h1 className="notificaciones-page__title">Notificaciones</h1>
        <p className="notificaciones-page__muted">
          Este flujo no está disponible en modo demo.
        </p>
      </section>
    )
  }

  return (
    <section className="notificaciones-page container-slim section-v-padding">
      <header className="notificaciones-page__head">
        <h1 className="notificaciones-page__title">Notificaciones</h1>
        <p className="notificaciones-page__sub">
          Avisos de compras, cambios de eventos y artistas que seguís.
        </p>
      </header>

      <NotificacionesPrefs token={token} enabled={enabled} />

      <div className="notificaciones-toolbar">
        <div className="notificaciones-tabs" role="tablist" aria-label="Filtro">
          {TABS.map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={tab === t.id}
              className={tab === t.id ? 'is-active' : ''}
              onClick={() => setTab(t.id)}
            >
              {t.label}
              {t.id === 'no-leidas' && noLeidas > 0 ? ` (${noLeidas})` : ''}
            </button>
          ))}
        </div>
        <button
          type="button"
          className="btn btn-secondary notificaciones-toolbar__readall"
          onClick={markAllVisible}
          disabled={markingAll || !items.some((n) => !n.leida)}
        >
          {markingAll ? 'Marcando…' : 'Marcar leídas'}
        </button>
      </div>

      {status === 'loading' && (
        <LoadingState label="Cargando notificaciones…" />
      )}
      {status === 'error' && <ErrorState message={error} onRetry={reload} />}
      {status === 'success' && error && (
        <p className="notificaciones-page__error">{error}</p>
      )}

      {status === 'success' && items.length === 0 && (
        <EmptyState
          action={
            <Link className="btn btn-primary" to="/eventos">
              Ver eventos
            </Link>
          }
        >
          {tab === 'no-leidas'
            ? 'No tenés notificaciones sin leer.'
            : 'Todavía no tenés notificaciones.'}
        </EmptyState>
      )}

      {items.length > 0 && (
        <ul className="notificaciones-list">
          {items.map((n) => (
            <NotificacionCard key={n.id} notificacion={n} onMarkRead={markRead} />
          ))}
        </ul>
      )}

      {status === 'success' && page + 1 < totalPages && (
        <div className="notificaciones-more">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={cargarMas}
            disabled={loadingMore}
          >
            {loadingMore ? 'Cargando…' : 'Cargar más'}
          </button>
        </div>
      )}
    </section>
  )
}
