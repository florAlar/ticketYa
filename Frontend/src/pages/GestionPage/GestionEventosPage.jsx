import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { listEventosGestion, patchEventoEstado } from '../../api/eventos'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPO_LABELS,
} from '../../domain/evento'
import './GestionPage.css'

function formatFecha(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

const GestionEventosPage = () => {
  const { user, token, isAdmin, canManageAllEvents } = useAuth()
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await listEventosGestion(user, {
        isAdmin: canManageAllEvents,
        token,
      })
      setItems(Array.isArray(data) ? data : data?.content || data?.items || [])
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar')
      setStatus('error')
    }
  }, [user, token, canManageAllEvents])

  useEffect(() => {
    load()
  }, [load])

  const onEstado = async (id, estado) => {
    setBusyId(id)
    try {
      await patchEventoEstado(id, estado, user, {
        isAdmin: canManageAllEvents,
        token,
      })
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo actualizar')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="gestion-page container section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / eventos
        </p>
        <h1 className="gestion-page__title">Eventos</h1>
        <p className="gestion-page__sub">
          {isAdmin
            ? 'Admin: GET /api/eventos (alcance amplio).'
            : 'Organizador: GET /api/organizadores/me/eventos.'}
        </p>
      </header>

      <div className="gestion-card__actions" style={{ marginBottom: '1rem' }}>
        <Link className="btn btn-primary" to="/gestion/eventos/nuevo">
          Nuevo evento
        </Link>
        <button type="button" className="btn" onClick={load} disabled={status === 'loading'}>
          Actualizar
        </button>
      </div>

      {error && <p className="gestion-error">{error}</p>}
      {status === 'loading' && <p className="gestion-card__muted">Cargando…</p>}

      {status === 'ok' && items.length === 0 && (
        <div className="gestion-card">
          <p>No hay eventos en este alcance.</p>
          <Link className="btn btn-primary" to="/gestion/eventos/nuevo">
            Crear el primero
          </Link>
        </div>
      )}

      {items.length > 0 && (
        <ul className="gestion-event-list">
          {items.map((e) => (
            <li key={e.id} className="gestion-event-row">
              <div className="gestion-event-row__main">
                <h2>{e.nombre}</h2>
                <p>
                  {EVENTO_TIPO_LABELS[e.tipo] || e.tipo}
                  {' · '}
                  {formatFecha(e.fechaInicio || e.fechaHora)}
                  {' · '}
                  {e.lugar?.ciudad || '—'}
                </p>
                <p className="gestion-card__muted">
                  Estado:{' '}
                  <strong>
                    {EVENTO_ESTADO_LABELS[e.estado] || e.estado}
                  </strong>
                  {' · '}
                  org: {e.idOrganizador}
                  {e.precioDesde != null ? ` · desde $${e.precioDesde}` : ''}
                </p>
              </div>
              <div className="gestion-event-row__actions">
                {e.estado === EVENTO_ESTADOS.BORRADOR && (
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={busyId === e.id}
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.PUBLICADO)}
                  >
                    Publicar
                  </button>
                )}
                {e.estado === EVENTO_ESTADOS.PUBLICADO && (
                  <button
                    type="button"
                    className="btn"
                    disabled={busyId === e.id}
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.SUSPENDIDO)}
                  >
                    Suspender
                  </button>
                )}
                {e.estado !== EVENTO_ESTADOS.CANCELADO && (
                  <button
                    type="button"
                    className="btn"
                    disabled={busyId === e.id}
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.CANCELADO)}
                  >
                    Cancelar
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion">← Panel</Link>
        {' · '}
        <Link to="/eventos">Catálogo público</Link>
      </p>
    </section>
  )
}

export default GestionEventosPage
