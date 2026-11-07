import { Link } from 'react-router-dom'
import { formatEventoFecha, formatPrecioDesde } from '../eventFormat'
import EventoCupoBadge from '../EventoCupoBadge'

/** Card grande para grilla de destacados */
const EventCardFeatured = ({ evento }) => {
  if (!evento) return null
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--featured">
      <div className="event-card__media">
        <img src={evento.imagenUrl} alt="" loading="lazy" />
        <EventoCupoBadge evento={evento} className="event-card__cupo" />
      </div>
      <div className="event-card__body">
        <h3 className="event-card__title">{evento.nombre}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
        <p className="event-card__meta">
          {evento.lugar?.nombre}
          {evento.lugar?.ciudad ? ` · ${evento.lugar.ciudad}` : ''}
        </p>
        {evento.precioDesde != null && (
          <p className="event-card__price">desde {formatPrecioDesde(evento.precioDesde)}</p>
        )}
      </div>
    </Link>
  )
}

export default EventCardFeatured
