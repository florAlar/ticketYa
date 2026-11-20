import { Link } from 'react-router-dom'
import { formatEventoFecha, partesNombre } from '../eventFormat'
import EventCardMedia from './EventCardMedia'

/** Card densa — preferencias / recomendaciones */
const EventCardCompact = ({ evento }) => {
  if (!evento) return null
  const { artista, titulo } = partesNombre(evento.nombre)
  return (
    <Link to={`/eventos/${evento.id}`} className="event-card event-card--compact">
      <EventCardMedia evento={evento} mostrarFecha={false} />
      <div className="event-card__body">
        {artista && <p className="event-card__tipo">{artista}</p>}
        <h3 className="event-card__title">{titulo}</h3>
        <p className="event-card__meta">{formatEventoFecha(evento.fechaInicio)}</p>
        {evento.motivo && <p className="event-card__motivo">{evento.motivo}</p>}
      </div>
    </Link>
  )
}

export default EventCardCompact
