import { useMemo } from 'react'
import useAsync from '../../../hooks/useAsync'
import { listEventos, getDisponibilidadBatch } from '../../../api/eventos'
import {
  normalizeDisponibilidadDto,
  withCupoResumen,
} from '../../../domain/evento'
import { resolveCard } from './cards/cardRegistry'

function filterKey(filter) {
  return filter ? JSON.stringify(filter) : ''
}

function mergeCupos(list, batch) {
  const map = new Map()
  const items = Array.isArray(batch?.items)
    ? batch.items
    : Array.isArray(batch)
      ? batch
      : []
  for (const raw of items) {
    const d = normalizeDisponibilidadDto(raw)
    if (d?.idEvento) map.set(String(d.idEvento), d)
  }
  return (list || []).map((ev) =>
    withCupoResumen(ev, map.get(String(ev.id)) || null)
  )
}

/**
 * Lista reutilizable: recibe filter (API) o items ya resueltos.
 * layout: 'grid' | 'row' | 'grid-4'
 * card: clave del registry (featured | standard | compact)
 * Tras listar: 1× batch disponibilidad → badge cupo en cards.
 */
const EventList = ({
  filter,
  items: itemsProp,
  layout = 'grid',
  card = 'standard',
}) => {
  const controlled = itemsProp !== undefined
  const key = useMemo(() => filterKey(filter), [filter])

  const {
    data: remote,
    status: remoteStatus,
    error: remoteError,
    reload,
  } = useAsync(
    async (signal) => {
      const data = await listEventos(filter || {}, { signal })
      const list = Array.isArray(data)
        ? data
        : data?.content || data?.items || []
      try {
        const ids = list.map((e) => e.id).filter(Boolean)
        if (!ids.length) return list
        const batch = await getDisponibilidadBatch(ids, { signal })
        return mergeCupos(list, batch)
      } catch (err) {
        if (err?.name === 'AbortError') throw err
        // El catálogo se muestra sin badge si la disponibilidad falla.
        return list
      }
    },
    [key],
    { enabled: !controlled }
  )

  const items = controlled ? itemsProp : remote || []
  const status = controlled ? 'success' : remoteStatus
  const error = remoteError?.message || ''
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
    return (
      <p className="event-list__status event-list__status--error">
        {error || 'Error al cargar eventos'}{' '}
        <button type="button" className="btn btn--ghost" onClick={reload}>
          Reintentar
        </button>
      </p>
    )
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
