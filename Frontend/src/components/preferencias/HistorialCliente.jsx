import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHistorialCliente } from '../../api/preferencias'
import { formatPrecioDesde } from '../home/events/eventFormat'

const PAGE_SIZE = 5

function formatFecha(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(d)
}

/** “Mi historial” — GET /api/clientes/{id}/historial (dueño o ADMIN). */
const HistorialCliente = ({ idCliente, token }) => {
  const [items, setItems] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('loading')
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!idCliente || !token) {
      setItems([])
      setTotal(0)
      setStatus('ok')
      return undefined
    }
    let cancelled = false
    setStatus('loading')
    setError('')
    setItems([])
    setPage(0)
    getHistorialCliente(idCliente, { token, page: 0, size: PAGE_SIZE })
      .then((data) => {
        if (cancelled) return
        setItems(Array.isArray(data?.content) ? data.content : [])
        setTotal(Number(data?.totalElements || 0))
        setStatus('ok')
      })
      .catch((err) => {
        if (cancelled) return
        setError(err.message || 'No se pudo cargar tu historial')
        setStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [idCliente, token])

  const cargarMas = async () => {
    if (loadingMore || !idCliente || !token) return
    setLoadingMore(true)
    setError('')
    try {
      const nextPage = page + 1
      const data = await getHistorialCliente(idCliente, {
        token,
        page: nextPage,
        size: PAGE_SIZE,
      })
      const nuevos = Array.isArray(data?.content) ? data.content : []
      setItems((prev) => {
        const seen = new Set(prev.map((i) => i.id))
        return [...prev, ...nuevos.filter((i) => !seen.has(i.id))]
      })
      setTotal(Number(data?.totalElements || 0))
      setPage(nextPage)
    } catch (err) {
      setError(err.message || 'No se pudo cargar más historial')
    } finally {
      setLoadingMore(false)
    }
  }

  return (
    <article className="perfil-card pref-card">
      <h2 className="perfil-card__heading">Mi historial</h2>
      <p className="perfil-page__muted">
        Tus compras registradas. También alimentan las recomendaciones.
      </p>

      {status === 'loading' && (
        <p className="perfil-page__muted">Cargando historial…</p>
      )}
      {error && <p className="perfil-page__error">{error}</p>}

      {status === 'ok' && items.length === 0 && (
        <p className="perfil-page__muted">
          Todavía no registramos compras tuyas. Cuando compres una entrada, va a
          aparecer acá.
        </p>
      )}

      {status === 'ok' && items.length > 0 && (
        <ul className="pref-historial">
          {items.map((item) => (
            <li key={item.id || `${item.idEvento}-${item.fechaCompra}`} className="pref-historial__item">
              <div className="pref-historial__head">
                {item.idEvento ? (
                  <Link
                    to={`/eventos/${item.idEvento}`}
                    className="pref-historial__title"
                  >
                    {item.nombreEvento || 'Evento'}
                  </Link>
                ) : (
                  <span className="pref-historial__title">
                    {item.nombreEvento || 'Evento'}
                  </span>
                )}
                {item.monto != null && (
                  <span className="pref-historial__monto">
                    {formatPrecioDesde(item.monto)}
                  </span>
                )}
              </div>
              <p className="pref-historial__meta">
                {[item.categoria, item.ciudad].filter(Boolean).join(' · ')}
              </p>
              <p className="pref-historial__meta">
                Compra: {formatFecha(item.fechaCompra)}
                {item.fechaEvento
                  ? ` · Evento: ${formatFecha(item.fechaEvento)}`
                  : ''}
              </p>
            </li>
          ))}
        </ul>
      )}

      {status === 'ok' && items.length < total && (
        <div className="perfil-card__actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={cargarMas}
            disabled={loadingMore}
          >
            {loadingMore ? 'Cargando…' : 'Ver más'}
          </button>
        </div>
      )}
    </article>
  )
}

export default HistorialCliente
