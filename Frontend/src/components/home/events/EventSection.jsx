import { Link } from 'react-router-dom'
import EventList from './EventList'
import useAuth from '../../../hooks/useAuth'

/** Bloque título mínimo + lista (filter + card + layout). */
const EventSection = ({ title, filter, layout, card, linkToAll }) => {
  const { isAuthenticated } = useAuth()
  const moreTo =
    linkToAll && !isAuthenticated
      ? `/login?next=${encodeURIComponent(linkToAll)}`
      : linkToAll

  return (
    <section className="event-section container">
      <div className="event-section__head">
        {title && <h2 className="event-section__title">{title}</h2>}
        {moreTo && (
          <Link to={moreTo} className="event-section__more">
            Ver todos
          </Link>
        )}
      </div>
      <EventList filter={filter} layout={layout} card={card} />
    </section>
  )
}

export default EventSection
