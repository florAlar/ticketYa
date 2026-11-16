import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { HiOutlineBell } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'
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
  const [noLeidas, setNoLeidas] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const wrapperRef = useRef(null)

  const loadCount = useCallback(async () => {
    if (!isAuthenticated || !token) return
    try {
      const data = await listarMisNotificaciones({ token, page: 0, size: 1 })
      setNoLeidas(Number(data?.noLeidas) || 0)
    } catch {
      /* silencioso: la campanita no debe romper el header */
    }
  }, [isAuthenticated, token])

  const loadItems = useCallback(async () => {
    if (!isAuthenticated || !token) return
    setStatus('loading')
    setError('')
    try {
      const data = await listarMisNotificaciones({
        token,
        page: 0,
        size: DROPDOWN_SIZE,
      })
      setItems(Array.isArray(data?.content) ? data.content : [])
      setNoLeidas(Number(data?.noLeidas) || 0)
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudieron cargar las notificaciones')
      setStatus('error')
    }
  }, [isAuthenticated, token])

  useEffect(() => {
    if (!isAuthenticated || !token) {
      setNoLeidas(0)
      return undefined
    }
    loadCount()
    const id = window.setInterval(loadCount, POLL_MS)
    return () => window.clearInterval(id)
  }, [isAuthenticated, token, loadCount])

  useEffect(() => {
    if (!open) return undefined
    loadItems()
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
  }, [open, loadItems])

  const markRead = async (notificacion) => {
    if (notificacion.leida) return
    try {
      await marcarNotificacionLeida(notificacion.id, { token })
      setItems((prev) =>
        prev.map((n) => (n.id === notificacion.id ? { ...n, leida: true } : n))
      )
      setNoLeidas((prev) => Math.max(0, prev - 1))
    } catch {
      /* se puede reintentar desde la bandeja */
    }
  }

  if (!FEATURE.notificaciones || !isAuthenticated) {
    return null
  }

  return (
    <div className="notification-bell" ref={wrapperRef}>
      <button
        type="button"
        className="notification-bell__button"
        aria-label={`Notificaciones${noLeidas ? `: ${noLeidas} sin leer` : ''}`}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
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

          {status === 'ok' && items.length === 0 && (
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
