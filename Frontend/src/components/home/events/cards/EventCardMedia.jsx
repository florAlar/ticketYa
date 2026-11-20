import { HiOutlineLightningBolt } from 'react-icons/hi'
import EventCover from '../../../ui/EventCover'
import Badge from '../../../ui/Badge'
import EventoCupoBadge from '../EventoCupoBadge'
import { EVENTO_TIPO_LABELS } from '../../../../domain/evento'
import { partesFecha } from '../eventFormat'

/** Fondo de toda card: portada a pleno, badges (alta demanda / cupo) y chip de fecha. */
const EventCardMedia = ({ evento, mostrarFecha = true }) => {
  const f = partesFecha(evento.fechaInicio)
  const categoria = EVENTO_TIPO_LABELS[evento.tipo] || evento.tipo
  return (
    <div className="event-card__media">
      <EventCover evento={evento} position="center 25%" />
      <div className="event-card__shade" aria-hidden />
      <div className="event-card__badges">
        {evento.esAltaDemanda && (
          <Badge
            hot
            icon={HiOutlineLightningBolt}
            size="sm"
            title="Alta demanda: se compra con fila virtual"
          >
            Alta demanda
          </Badge>
        )}
        {categoria && <span className="event-card__artist">{categoria}</span>}
        <EventoCupoBadge evento={evento} />
      </div>
      {mostrarFecha && f && (
        <div className="event-card__date" aria-hidden>
          <strong>{f.dia}</strong>
          <span>{f.mes}</span>
        </div>
      )}
    </div>
  )
}

export default EventCardMedia
