import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHistorialCliente } from '../../api/preferencias'
import { formatPrecioDesde } from '../home/events/eventFormat'
import { formatFechaCorta } from '../../lib/format'
import useAsync from '../../hooks/useAsync'
import { ErrorState, LoadingState } from '../ui/AsyncState'

const PAGE_SIZE = 5
const MORE_VACIO = { key: '', items: [], page: 0, total: 0, loading: false, error: '' }

/** “Mi historial” — GET /api/clientes/{id}/historial (dueño o ADMIN). */
const HistorialCliente = ({ idCliente, token }) => {
  const enabled = Boolean(idCliente && token)
  const queryKey = `${idCliente || ''}|${token || ''}`

  const {
    data,
    status: queryStatus,
    error: loadError,
    reload,
  } = useAsync(
    (signal) =>
      getHistorialCliente(idCliente, {
        token,
        page: 0,
        size: PAGE_SIZE,
        signal,
      }),
    [idCliente, token],
    { enabled }
  )

  // Páginas extra (Ver más) se acumulan aparte de la página 0 del hook.
  const [more, setMore] = useState(MORE_VACIO)
  if (more.key !== queryKey) {
    setMore({ ...MORE_VACIO, key: queryKey })
  }

  const baseItems = useMemo(() => data?.content || [], [data])
  const items = useMemo(() => {
    if (more.items.length === 0) return baseItems
    const seen = new Set(baseItems.map((i) => i.id))
    return [...baseItems, ...more.items.filter((i) => !seen.has(i.id))]
  }, [baseItems, more.items])

  const total = Math.max(
    Number(data?.totalElements || 0),
    Number(more.total || 0)
  )
  const page = Math.max(Number(data?.page ?? 0), more.page)
  const status = enabled ? queryStatus : 'success'
  const error = more.error || ''

  const cargarMas = async () => {
    if (more.loading || !idCliente || !token) return
    setMore((prev) => ({ ...prev, loading: true, error: '' }))
    try {
      const nextPage = page + 1
      const next = await getHistorialCliente(idCliente, {
        token,
        page: nextPage,
        size: PAGE_SIZE,
      })
      const nuevos = Array.isArray(next?.content) ? next.content : []
      setMore((prev) => ({
        ...prev,
        items: [...prev.items, ...nuevos],
        page: nextPage,
        total: Number(next?.totalElements || prev.total),
        loading: false,
      }))
    } catch (err) {
      setMore((prev) => ({
        ...prev,
        loading: false,
        error: err.message || 'No se pudo cargar más historial',
      }))
    }
  }

  return (
    <article className="perfil-card pref-card">
      <h2 className="perfil-card__heading">Mi historial</h2>
      <p className="perfil-page__muted">
        Tus compras registradas. También alimentan las recomendaciones.
      </p>

      {status === 'loading' && <LoadingState label="Cargando historial…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {error && <p className="perfil-page__error">{error}</p>}

      {status === 'success' && items.length === 0 && (
        <p className="perfil-page__muted">
          Todavía no registramos compras tuyas. Cuando compres una entrada, va a
          aparecer acá.
        </p>
      )}

      {status === 'success' && items.length > 0 && (
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
                Compra: {formatFechaCorta(item.fechaCompra)}
                {item.fechaEvento
                  ? ` · Evento: ${formatFechaCorta(item.fechaEvento)}`
                  : ''}
              </p>
            </li>
          ))}
        </ul>
      )}

      {status === 'success' && items.length < total && (
        <div className="perfil-card__actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={cargarMas}
            disabled={more.loading}
          >
            {more.loading ? 'Cargando…' : 'Ver más'}
          </button>
        </div>
      )}
    </article>
  )
}

export default HistorialCliente
