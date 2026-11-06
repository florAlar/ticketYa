import { Link } from 'react-router-dom'
import { formatEventoFecha, formatPrecioDesde } from '../eventFormat'

/** Card estándar (filas horizontales / listados) */
const EventCardStandard = ({ evento }) => {
  if (!evento) return null
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--standard">
      <div className="event-card__media">
        <img src={evento.imagenUrl} alt="" loading="lazy" />
      </div>
      <div className="event-card__body">
        <p className="event-card__tipo">{evento.tipo}</p>
        <h3 className="event-card__title">{evento.nombre}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
        <p className="event-card__meta">
          {evento.lugar?.ciudad || evento.lugar?.nombre}
        </p>
        {evento.precioDesde != null && (
          <p className="event-card__price">{formatPrecioDesde(evento.precioDesde)}</p>
        )}
      </div>
    </Link>
  )
}

export default EventCardStandard
