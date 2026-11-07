import { useEffect, useMemo, useState } from 'react'
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

    ;(async () => {
      try {
        const data = await listEventos(filter || {})
        if (cancelled) return
        const list = Array.isArray(data)
          ? data
          : data?.content || data?.items || []
        let withCupo = list
        try {
          const ids = list.map((e) => e.id).filter(Boolean)
          if (ids.length) {
            const batch = await getDisponibilidadBatch(ids)
            if (cancelled) return
            withCupo = mergeCupos(list, batch)
          }
        } catch (err) {
          // catálogo igual se muestra sin badge si batch falla (jar/ruta vieja)
          if (typeof console !== 'undefined') {
            console.warn('[EventList] batch disponibilidad falló:', err?.message || err)
          }
          withCupo = list
        }
        if (cancelled) return
        setRemote((prev) =>
          prev.key !== activeKey
            ? prev
            : { key: activeKey, items: withCupo, status: 'success', error: '' }
        )
      } catch (err) {
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
      }
    })()

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
