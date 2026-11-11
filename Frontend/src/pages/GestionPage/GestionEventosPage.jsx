import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { listEventosGestion, patchEventoEstado } from '../../api/eventos'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPO_LABELS,
} from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import GateButton from '../../components/ui/GateButton'
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
  const [status, setStatus] = useState(
    FEATURE.eventosGestion ? 'loading' : 'gated'
  )
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    if (!FEATURE.eventosGestion) {
      setStatus('gated')
      setItems([])
      return
    }
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
    const next = String(estado || '').toUpperCase()
    if (next === EVENTO_ESTADOS.CANCELADO || next === 'CANCELADO') {
      if (!FEATURE.eventosCancelar || !isAdmin) return
    } else if (next === EVENTO_ESTADOS.SUSPENDIDO || next === 'SUSPENDIDO') {
      if (!FEATURE.eventosSuspender) return
    } else if (next === EVENTO_ESTADOS.PUBLICADO || next === 'PUBLICADO') {
      if (!FEATURE.eventosGestion) return
    } else if (!FEATURE.eventosGestion) {
      return
    }
    setBusyId(id)
    setError('')
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
            ? 'Admin: listado amplio + publicar / suspender / reanudar / cancelar.'
            : 'Organizador: tus eventos. Publicar, suspender y volver a publicar; cancelar solo admin.'}
          {!FEATURE.eventosGestion && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <div className="gestion-card__actions" style={{ marginBottom: '1rem' }}>
        <GateLink
          enabled={FEATURE.eventosGestion}
          className="btn btn-primary"
          to="/gestion/eventos/nuevo"
        >
          Nuevo evento
        </GateLink>
        <GateButton
          enabled={FEATURE.eventosGestion}
          type="button"
          className="btn"
          disabled={status === 'loading'}
          onClick={load}
        >
          Actualizar
        </GateButton>
      </div>

      {error && <p className="gestion-error">{error}</p>}
      {status === 'loading' && <p className="gestion-card__muted">Cargando…</p>}
      {status === 'gated' && (
        <div className="gestion-card">
          <p>Gestión de eventos no cableada al backend en esta build.</p>
          <p className="gestion-card__muted">{GATE_HINT}</p>
        </div>
      )}

      {status === 'ok' && items.length === 0 && (
        <div className="gestion-card">
          <p>No hay eventos en este alcance.</p>
          <GateLink
            enabled={FEATURE.eventosGestion}
            className="btn btn-primary"
            to="/gestion/eventos/nuevo"
          >
            Crear el primero
          </GateLink>
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
                  {e.lugar?.ciudad || e.lugarCiudad || '—'}
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
                {e.estado !== EVENTO_ESTADOS.BORRADOR && (
                  <GateLink
                    enabled={FEATURE.reportes}
                    className="btn"
                    to={`/gestion/eventos/${e.id}/reportes`}
                  >
                    Reportes
                  </GateLink>
                )}
                {e.estado === EVENTO_ESTADOS.BORRADOR && (
                  <GateLink
                    enabled={FEATURE.eventosGestion}
                    className="btn"
                    to={`/gestion/eventos/${e.id}/editar`}
                  >
                    Editar
                  </GateLink>
                )}
                {e.estado === EVENTO_ESTADOS.BORRADOR && (
                  <GateButton
                    enabled={FEATURE.eventosGestion}
                    type="button"
                    className="btn btn-primary"
                    disabled={busyId === e.id}
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.PUBLICADO)}
                  >
                    Publicar
                  </GateButton>
                )}
                {e.estado === EVENTO_ESTADOS.PUBLICADO && (
                  <GateButton
                    enabled={FEATURE.eventosSuspender}
                    type="button"
                    className="btn"
                    disabled={busyId === e.id}
                    title={
                      FEATURE.eventosSuspender
                        ? undefined
                        : GATE_HINT
                    }
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.SUSPENDIDO)}
                  >
                    Suspender
                  </GateButton>
                )}
                {e.estado === EVENTO_ESTADOS.SUSPENDIDO && (
                  <GateButton
                    enabled={FEATURE.eventosGestion}
                    type="button"
                    className="btn btn-primary"
                    disabled={busyId === e.id}
                    title={
                      FEATURE.eventosGestion
                        ? 'Reanuda ventas: vuelve el evento a PUBLICADO'
                        : GATE_HINT
                    }
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.PUBLICADO)}
                  >
                    Volver a publicar
                  </GateButton>
                )}
                {e.estado !== EVENTO_ESTADOS.CANCELADO &&
                  e.estado !== EVENTO_ESTADOS.FINALIZADO && (
                  <GateButton
                    enabled={FEATURE.eventosCancelar && isAdmin}
                    type="button"
                    className="btn"
                    disabled={busyId === e.id}
                    title={
                      !isAdmin
                        ? 'Solo administrador puede cancelar un evento'
                        : !FEATURE.eventosCancelar
                          ? GATE_HINT
                          : undefined
                    }
                    onClick={() => onEstado(e.id, EVENTO_ESTADOS.CANCELADO)}
                  >
                    Cancelar
                  </GateButton>
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
