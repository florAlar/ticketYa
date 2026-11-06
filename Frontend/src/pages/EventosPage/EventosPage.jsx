import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import EventList from '../../components/home/events/EventList'
import '../../components/home/events/events.css'
import './EventosPage.css'

/**
 * Página de búsqueda / catálogo.
 * Query: q, tipo, ciudad, destacados, sort
 * Grilla 4 columnas; alto según cantidad de resultados.
 */
const EventosPage = () => {
  const [params] = useSearchParams()

  const filter = useMemo(() => {
    const next = {}
    const q = params.get('q')
    const tipo = params.get('tipo')
    const ciudad = params.get('ciudad')
    const sort = params.get('sort')
    const destacados = params.get('destacados')

    if (q) next.q = q
    if (tipo) next.tipo = tipo
    if (ciudad) next.ciudad = ciudad
    if (sort) next.sort = sort
    if (destacados === '1' || destacados === 'true') next.destacados = true

    return next
  }, [params])

  const subtitle = useMemo(() => {
    const parts = []
    if (filter.q) parts.push(`“${filter.q}”`)
    if (filter.tipo) parts.push(filter.tipo)
    if (filter.ciudad) parts.push(filter.ciudad)
    if (filter.destacados) parts.push('destacados')
    return parts.length ? parts.join(' · ') : 'Todos los eventos'
  }, [filter])

  return (
    <section className="eventos-page container section-v-padding">
      <header className="eventos-page__head">
        <h1 className="eventos-page__title">Eventos</h1>
        <p className="eventos-page__sub">{subtitle}</p>
      </header>

      <EventList filter={filter} layout="grid-4" card="standard" />
    </section>
  )
}

export default EventosPage
