import { Link } from 'react-router-dom'
import { formatEventoFecha, imagenEvento } from '../eventFormat'
import EventoCupoBadge from '../EventoCupoBadge'

/** Card densa — preferencias / recomendaciones */
const EventCardCompact = ({ evento }) => {
  if (!evento) return null
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--compact">
      <div className="event-card__media">
        <img src={imagenEvento(evento)} alt="" loading="lazy" />
        <EventoCupoBadge evento={evento} className="event-card__cupo" />
      </div>
      <div className="event-card__body">
        <h3 className="event-card__title">{evento.nombre}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
        {evento.motivo && (
          <p className="event-card__motivo">{evento.motivo}</p>
        )}
      </div>
    </Link>
  )
}

export default EventCardCompact
