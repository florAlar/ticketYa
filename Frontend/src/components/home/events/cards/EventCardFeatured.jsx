import { Link } from 'react-router-dom'
import { HiOutlineLocationMarker } from 'react-icons/hi'
import { formatEventoFecha, partesNombre } from '../eventFormat'
import EventCardMedia from './EventCardMedia'

/** Card grande para grilla de destacados */
const EventCardFeatured = ({ evento }) => {
  if (!evento) return null
  const { artista, titulo } = partesNombre(evento.nombre)
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--featured">
      <EventCardMedia evento={evento} />
      <div className="event-card__body">
        {artista && <p className="event-card__tipo">{artista}</p>}
        <h3 className="event-card__title">{titulo}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
        <p className="event-card__meta event-card__meta--lugar">
          <HiOutlineLocationMarker aria-hidden />{' '}
          <span>
            {evento.lugar?.nombre}
            {evento.lugar?.ciudad ? ` · ${evento.lugar.ciudad}` : ''}
          </span>
        </p>
      </div>
    </Link>
  )
}

export default EventCardFeatured
