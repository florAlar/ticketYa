import { useMemo } from 'react'
import useAsync from '../../../hooks/useAsync'
import { listEventos, getDisponibilidadBatch } from '../../../api/eventos'
import {
  normalizeDisponibilidadDto,
  withCupoResumen,
} from '../../../domain/evento'
import { resolveCard } from './cards/cardRegistry'
import EventCardSkeleton from './EventCardSkeleton'
import EventRow from './EventRow'
import { EmptyState } from '../../ui/AsyncState'

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
    return (
      <ul className={layoutClass} aria-busy="true" aria-label="Cargando eventos">
        {Array.from({ length: layout === 'row' ? 4 : 8 }, (_, i) => (
          <li key={i} className="event-list__item">
            <EventCardSkeleton />
          </li>
        ))}
      </ul>
    )
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
    return (
      <EmptyState title="No encontramos eventos">
        Probá con otros filtros o volvé más tarde: se publican nuevos todo el
        tiempo.
      </EmptyState>
    )
  }

  const cards = items.map((evento) => (
    <li key={evento.id} className="event-list__item">
      <Card evento={evento} />
    </li>
  ))

  if (layout === 'row') {
    return <EventRow className={layoutClass}>{cards}</EventRow>
  }
  return <ul className={layoutClass}>{cards}</ul>
}

export default EventList
