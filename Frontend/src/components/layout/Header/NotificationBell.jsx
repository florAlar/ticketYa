import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { HiOutlineBell } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'
import useAsync from '../../../hooks/useAsync'
import { FEATURE } from '../../../config/featureGates'
import {
  listarMisNotificaciones,
  marcarNotificacionLeida,
} from '../../../api/notificaciones'
import {
  formatFechaNotificacion,
  tipoNotificacionLabel,
} from '../../../domain/notificacion'
import './NotificationBell.css'

const POLL_MS = 45000
const DROPDOWN_SIZE = 6

export default function NotificationBell() {
  const { isAuthenticated, token } = useAuth()
  const [open, setOpen] = useState(false)
  const wrapperRef = useRef(null)
  const enabled = FEATURE.notificaciones && isAuthenticated && Boolean(token)

  const countQuery = useAsync(
    async (signal) => {
      const data = await listarMisNotificaciones({ token, page: 0, size: 1, signal })
      return Number(data?.noLeidas) || 0
    },
    [token],
    { enabled }
  )

  const itemsQuery = useAsync(
    async (signal) => {
      const data = await listarMisNotificaciones({
        token,
        page: 0,
        size: DROPDOWN_SIZE,
        signal,
      })
      return {
        items: Array.isArray(data?.content) ? data.content : [],
        noLeidas: Number(data?.noLeidas) || 0,
      }
    },
    [token],
    { enabled: enabled && open }
  )

  // Polling del contador sin setState sincrónico en el effect: la carga inicial
  // la hace useAsync y el intervalo solo dispara recargas.
  const reloadCount = countQuery.reload
  useEffect(() => {
    if (!enabled) return undefined
    const id = window.setInterval(() => reloadCount(), POLL_MS)
    return () => window.clearInterval(id)
  }, [enabled, reloadCount])

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (event) => {
      if (!wrapperRef.current?.contains(event.target)) setOpen(false)
    }
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const markRead = async (notificacion) => {
    if (notificacion.leida) return
    try {
      await marcarNotificacionLeida(notificacion.id, { token })
      itemsQuery.setData((prev) =>
        prev
          ? {
              ...prev,
              items: prev.items.map((n) =>
                n.id === notificacion.id ? { ...n, leida: true } : n
              ),
            }
          : prev
      )
      countQuery.setData((prev) => Math.max(0, Number(prev || 0) - 1))
    } catch {
      /* se puede reintentar desde la bandeja */
    }
  }

  if (!enabled) {
    return null
  }

  const items = itemsQuery.data?.items || []
  const noLeidas = Number(
    countQuery.data ?? itemsQuery.data?.noLeidas ?? 0
  )
  const status = open ? itemsQuery.status : 'idle'
  const error = itemsQuery.error?.message || ''

  return (
    <div className="notification-bell" ref={wrapperRef}>
      <button
        type="button"
        className="notification-bell__button"
        aria-label={`Notificaciones${noLeidas ? `: ${noLeidas} sin leer` : ''}`}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <HiOutlineBell aria-hidden />
        {noLeidas > 0 && (
          <span className="notification-bell__badge" aria-hidden>
            {noLeidas > 99 ? '99+' : noLeidas}
          </span>
        )}
      </button>

      {open && (
        <div className="notification-bell__panel" role="dialog" aria-label="Notificaciones">
          <div className="notification-bell__head">
            <strong>Notificaciones</strong>
            <Link to="/notificaciones" onClick={() => setOpen(false)}>
              Ver todas
            </Link>
          </div>

          {status === 'loading' && (
            <p className="notification-bell__muted">Cargando…</p>
          )}
          {status === 'error' && (
            <p className="notification-bell__error">{error}</p>
          )}

          {status === 'success' && items.length === 0 && (
            <p className="notification-bell__muted">
              No tenés notificaciones todavía.
            </p>
          )}

          {items.length > 0 && (
            <ul className="notification-bell__list">
              {items.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={`notification-bell__item ${
                      n.leida ? '' : 'is-unread'
                    }`}
                    onClick={() => markRead(n)}
                  >
                    <span className="notification-bell__item-top">
                      <span className="notification-bell__type">
                        {tipoNotificacionLabel(n.tipo)}
                      </span>
                      <span className="notification-bell__date">
                        {formatFechaNotificacion(n.fechaCreacion)}
                      </span>
                    </span>
                    <span className="notification-bell__subject">{n.asunto}</span>
                    <span className="notification-bell__text">{n.contenido}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div className="notification-bell__foot">
            <Link to="/notificaciones" onClick={() => setOpen(false)}>
              Abrir bandeja
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
