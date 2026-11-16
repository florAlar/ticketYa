import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import {
  listarMisNotificaciones,
  marcarNotificacionLeida,
} from '../../api/notificaciones'
import {
  getNotificacionesPreferencias,
  putNotificacionesPreferencias,
} from '../../api/preferencias'
import { FEATURE } from '../../config/featureGates'
import {
  canalNotificacionLabel,
  estadoNotificacionLabel,
  formatFechaNotificacion,
  tipoNotificacionLabel,
} from '../../domain/notificacion'
import './NotificacionesPage.css'

const PAGE_SIZE = 20
const TABS = [
  { id: 'todas', label: 'Todas' },
  { id: 'no-leidas', label: 'No leídas' },
]

export default function NotificacionesPage() {
  const { token } = useAuth()
  const [tab, setTab] = useState('todas')
  const [items, setItems] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [noLeidas, setNoLeidas] = useState(0)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [loadingMore, setLoadingMore] = useState(false)
  const [markingAll, setMarkingAll] = useState(false)

  const [prefs, setPrefs] = useState(null)
  const [prefsStatus, setPrefsStatus] = useState('loading')
  const [prefsError, setPrefsError] = useState('')
  const [prefsMsg, setPrefsMsg] = useState('')
  const [savingPrefs, setSavingPrefs] = useState(false)

  const load = useCallback(
    async (nextPage = 0, append = false) => {
      if (!token) return
      if (append) setLoadingMore(true)
      else {
        setStatus('loading')
        setError('')
      }
      try {
        const data = await listarMisNotificaciones({
          token,
          page: nextPage,
          size: PAGE_SIZE,
          leida: tab === 'no-leidas' ? false : undefined,
        })
        const content = Array.isArray(data?.content) ? data.content : []
        setItems((prev) => (append ? [...prev, ...content] : content))
        setPage(Number(data?.page) || nextPage)
        setTotalPages(Number(data?.totalPages) || 1)
        setNoLeidas(Number(data?.noLeidas) || 0)
        setStatus('ok')
      } catch (err) {
        setError(err.message || 'No se pudieron cargar las notificaciones')
        if (!append) setStatus('error')
      } finally {
        if (append) setLoadingMore(false)
      }
    },
    [token, tab]
  )

  const loadPrefs = useCallback(async () => {
    if (!token) return
    setPrefsStatus('loading')
    setPrefsError('')
    try {
      const data = await getNotificacionesPreferencias({ token })
      setPrefs({
        push: data?.push !== false,
        email: data?.email !== false,
        tipos: Array.isArray(data?.tipos) ? data.tipos : [],
      })
      setPrefsStatus('ok')
    } catch (err) {
      setPrefsError(err.message || 'No se pudieron cargar las preferencias')
      setPrefsStatus('error')
    }
  }, [token])

  useEffect(() => {
    load(0, false)
  }, [load])

  useEffect(() => {
    loadPrefs()
  }, [loadPrefs])

  const markRead = async (notificacion) => {
    if (notificacion.leida) return
    try {
      await marcarNotificacionLeida(notificacion.id, { token })
      setItems((prev) =>
        tab === 'no-leidas'
          ? prev.filter((n) => n.id !== notificacion.id)
          : prev.map((n) =>
              n.id === notificacion.id ? { ...n, leida: true } : n
            )
      )
      setNoLeidas((prev) => Math.max(0, prev - 1))
    } catch (err) {
      setError(err.message || 'No se pudo marcar como leída')
    }
  }

  const markAllVisible = async () => {
    const pendientes = items.filter((n) => !n.leida)
    if (pendientes.length === 0) return
    setMarkingAll(true)
    setError('')
    try {
      await Promise.all(
        pendientes.map((n) => marcarNotificacionLeida(n.id, { token }))
      )
      setItems((prev) =>
        tab === 'no-leidas'
          ? []
          : prev.map((n) => ({ ...n, leida: true }))
      )
      setNoLeidas((prev) => Math.max(0, prev - pendientes.length))
    } catch (err) {
      setError(err.message || 'No se pudieron marcar las notificaciones')
    } finally {
      setMarkingAll(false)
    }
  }

  const toggleCanal = async (canal) => {
    if (!prefs || savingPrefs) return
    const next = { ...prefs, [canal]: !prefs[canal] }
    setPrefs(next)
    setSavingPrefs(true)
    setPrefsError('')
    setPrefsMsg('')
    try {
      const saved = await putNotificacionesPreferencias(
        { push: next.push, email: next.email },
        { token }
      )
      setPrefs({
        push: saved?.push !== false,
        email: saved?.email !== false,
        tipos: Array.isArray(saved?.tipos) ? saved.tipos : next.tipos,
      })
      setPrefsMsg('Preferencias guardadas')
    } catch (err) {
      setPrefsError(err.message || 'No se pudieron guardar las preferencias')
      loadPrefs()
    } finally {
      setSavingPrefs(false)
    }
  }

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

      <section className="notificaciones-prefs" aria-label="Preferencias de notificación">
        <div className="notificaciones-prefs__copy">
          <h2>Canales</h2>
          <p>
            Elegí cómo querés recibir los avisos. El push es simulado en esta
            demo; el email se envía de verdad a MailHog.
          </p>
          {prefs?.tipos?.length > 0 && (
            <p className="notificaciones-prefs__tipos">
              Tipos habilitados: {prefs.tipos.map(tipoNotificacionLabel).join(', ')}
            </p>
          )}
          {prefsStatus === 'loading' && (
            <p className="notificaciones-page__muted">Cargando preferencias…</p>
          )}
          {prefsError && <p className="notificaciones-page__error">{prefsError}</p>}
          {prefsMsg && <p className="notificaciones-page__ok">{prefsMsg}</p>}
        </div>

        <div className="notificaciones-prefs__channels">
          <label className="notificaciones-prefs__toggle">
            <input
              type="checkbox"
              checked={Boolean(prefs?.push)}
              disabled={prefsStatus !== 'ok' || savingPrefs}
              onChange={() => toggleCanal('push')}
            />
            <span>
              <strong>Push</strong>
              <small>Aviso dentro de la app</small>
            </span>
          </label>
          <label className="notificaciones-prefs__toggle">
            <input
              type="checkbox"
              checked={Boolean(prefs?.email)}
              disabled={prefsStatus !== 'ok' || savingPrefs}
              onChange={() => toggleCanal('email')}
            />
            <span>
              <strong>Email</strong>
              <small>Correo a tu cuenta</small>
            </span>
          </label>
        </div>
      </section>

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
        <p className="notificaciones-page__muted">Cargando notificaciones…</p>
      )}
      {status === 'error' && (
        <p className="notificaciones-page__error">{error}</p>
      )}

      {status === 'ok' && items.length === 0 && (
        <div className="notificaciones-empty">
          <p>
            {tab === 'no-leidas'
              ? 'No tenés notificaciones sin leer.'
              : 'Todavía no tenés notificaciones.'}
          </p>
          <Link className="btn btn-primary" to="/eventos">
            Ver eventos
          </Link>
        </div>
      )}

      {items.length > 0 && (
        <ul className="notificaciones-list">
          {items.map((n) => (
            <li
              key={n.id}
              className={`notificacion-card ${n.leida ? '' : 'is-unread'}`}
            >
              <div className="notificacion-card__top">
                <span className="notificacion-card__type">
                  {tipoNotificacionLabel(n.tipo)}
                </span>
                <span className={`notificacion-card__channel is-${String(n.canal).toLowerCase()}`}>
                  {canalNotificacionLabel(n.canal)}
                </span>
                <span className="notificacion-card__date">
                  {formatFechaNotificacion(n.fechaCreacion)}
                </span>
              </div>

              <h2 className="notificacion-card__subject">{n.asunto}</h2>
              <p className="notificacion-card__text">{n.contenido}</p>

              <div className="notificacion-card__foot">
                <span className="notificacion-card__state">
                  {estadoNotificacionLabel(n.estado)}
                  {n.fechaEnvio
                    ? ` · ${formatFechaNotificacion(n.fechaEnvio)}`
                    : ''}
                </span>
                <span className="notificacion-card__actions">
                  {n.idEventoOrigen && (
                    <Link to={`/eventos/${n.idEventoOrigen}`}>Ver evento</Link>
                  )}
                  {!n.leida && (
                    <button type="button" onClick={() => markRead(n)}>
                      Marcar leída
                    </button>
                  )}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}

      {status === 'ok' && page + 1 < totalPages && (
        <div className="notificaciones-more">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => load(page + 1, true)}
            disabled={loadingMore}
          >
            {loadingMore ? 'Cargando…' : 'Cargar más'}
          </button>
        </div>
      )}
    </section>
  )
}
