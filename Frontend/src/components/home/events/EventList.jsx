import { useEffect, useMemo, useState } from 'react'
import { listEventos } from '../../../api/eventos'
import { resolveCard } from './cards/cardRegistry'

function filterKey(filter) {
  return filter ? JSON.stringify(filter) : ''
}

/**
 * Lista reutilizable: recibe filter (API) o items ya resueltos.
 * layout: 'grid' | 'row'
 * card: clave del registry (featured | standard | compact)
 */
const EventList = ({
  filter,
  items: itemsProp,
  layout = 'grid',
  card = 'standard',
}) => {
  const controlled = itemsProp !== undefined
  const key = useMemo(() => filterKey(filter), [filter])

  const [remote, setRemote] = useState({
    key,
    items: [],
    status: 'loading',
    error: '',
  })

  // Reset al cambiar el filtro (patrón React: ajustar estado en render)
  if (!controlled && remote.key !== key) {
    setRemote({ key, items: [], status: 'loading', error: '' })
  }

  useEffect(() => {
    if (controlled) return undefined

    let cancelled = false
    const activeKey = key

    listEventos(filter || {})
      .then((data) => {
        if (cancelled) return
        const list = Array.isArray(data)
          ? data
          : data?.content || data?.items || []
        setRemote((prev) =>
          prev.key !== activeKey
            ? prev
            : { key: activeKey, items: list, status: 'success', error: '' }
        )
      })
      .catch((err) => {
        if (cancelled) return
        setRemote((prev) =>
          prev.key !== activeKey
            ? prev
            : {
                key: activeKey,
                items: [],
                status: 'error',
                error: err.message || 'Error al cargar eventos',
              }
        )
      })

    return () => {
      cancelled = true
    }
  }, [controlled, key, filter])

  const items = controlled ? itemsProp : remote.items
  const status = controlled ? 'success' : remote.status
  const error = remote.error
  const Card = resolveCard(card)
  const layoutClass =
    layout === 'row'
      ? 'event-list event-list--row'
      : layout === 'grid-4'
        ? 'event-list event-list--grid-4'
        : 'event-list event-list--grid'

  if (status === 'loading') {
    return <p className="event-list__status">Cargando…</p>
  }
  if (status === 'error') {
    return <p className="event-list__status event-list__status--error">{error}</p>
  }
  if (!items?.length) {
    return <p className="event-list__status">No hay eventos.</p>
  }

  return (
    <ul className={layoutClass}>
      {items.map((evento) => (
        <li key={evento.id} className="event-list__item">
          <Card evento={evento} />
        </li>
      ))}
    </ul>
  )
}

export default EventList
