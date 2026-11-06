import { Link } from 'react-router-dom'
import { formatEventoFecha } from '../eventFormat'

/** Card densa — preferencias / recomendaciones */
const EventCardCompact = ({ evento }) => {
  if (!evento) return null
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--compact">
      <div className="event-card__media">
        <img src={evento.imagenUrl} alt="" loading="lazy" />
      </div>
      <div className="event-card__body">
        <h3 className="event-card__title">{evento.nombre}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
      </div>
    </Link>
  )
}

export default EventCardCompact
