import { Link } from 'react-router-dom'
import { HiOutlineLocationMarker } from 'react-icons/hi'
import { formatEventoFecha, partesNombre } from '../eventFormat'
import EventCardMedia from './EventCardMedia'

/** Card estándar (grillas y listados) */
const EventCardStandard = ({ evento }) => {
  if (!evento) return null
  const { artista, titulo } = partesNombre(evento.nombre)
  const lugar = evento.lugar?.nombre
    ? `${evento.lugar.nombre}${evento.lugar.ciudad ? ` · ${evento.lugar.ciudad}` : ''}`
    : evento.lugar?.ciudad
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--standard">
      <EventCardMedia evento={evento} />
      <div className="event-card__body">
        {artista && <p className="event-card__tipo">{artista}</p>}
        <h3 className="event-card__title">{titulo}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
        {lugar && (
          <p className="event-card__meta event-card__meta--lugar">
            <HiOutlineLocationMarker aria-hidden /> <span>{lugar}</span>
          </p>
        )}
      </div>
    </Link>
  )
}

export default EventCardStandard
