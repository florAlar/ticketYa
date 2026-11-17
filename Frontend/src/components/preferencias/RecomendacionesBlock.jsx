import { Link } from 'react-router-dom'
import EventList from '../home/events/EventList'

/**
 * Bloque reutilizable de recomendaciones: título + lista controlada.
 * No muestra nada raro si todavía no hay datos: el padre decide si lo renderiza.
 */
const RecomendacionesBlock = ({
  title,
  subtitle,
  items = [],
  loading = false,
  error = '',
  linkToAll,
  layout = 'row',
  card = 'compact',
  className = 'event-section container recomendaciones-block',
  emptyText = 'No hay recomendaciones por ahora.',
}) => {
  return (
    <section className={className}>
      <div className="event-section__head">
        <div>
          <h2 className="event-section__title">{title}</h2>
          {subtitle && (
            <p className="recomendaciones-block__sub">{subtitle}</p>
          )}
        </div>
        {linkToAll && (
          <Link to={linkToAll} className="event-section__more">
            Ver todos
          </Link>
        )}
      </div>

      {loading && (
        <p className="event-list__status">Buscando eventos para vos…</p>
      )}
      {!loading && error && (
        <p className="event-list__status event-list__status--error">{error}</p>
      )}
      {!loading && !error && items.length === 0 && (
        <p className="event-list__status">{emptyText}</p>
      )}
      {!loading && !error && items.length > 0 && (
        <EventList items={items} layout={layout} card={card} />
      )}
    </section>
  )
}

export default RecomendacionesBlock
