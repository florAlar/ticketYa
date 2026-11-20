import { HiOutlineBan, HiOutlinePause, HiOutlineFire } from 'react-icons/hi'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  cupoLabelDeEvento,
  isEventoAgotado,
} from '../../../domain/evento'
import Badge from '../../ui/Badge'

/**
 * Badge de estado de evento o cupo en cards.
 * Prioridad: Cancelado > Suspendido > cupo (Agotado / últimos / …).
 */
export default function EventoCupoBadge({ evento, className = '' }) {
  if (!evento) return null

  const estado = String(evento.estado || '').toUpperCase()

  if (estado === EVENTO_ESTADOS.CANCELADO) {
    return (
      <Badge tone="danger" solid icon={HiOutlineBan} className={className}>
        {EVENTO_ESTADO_LABELS.CANCELADO || 'Cancelado'}
      </Badge>
    )
  }
  if (estado === EVENTO_ESTADOS.SUSPENDIDO) {
    return (
      <Badge tone="warning" solid icon={HiOutlinePause} className={className}>
        {EVENTO_ESTADO_LABELS.SUSPENDIDO || 'Suspendido'}
      </Badge>
    )
  }

  const label = cupoLabelDeEvento(evento)
  if (!label) return null
  const agotado = isEventoAgotado(evento)
  return (
    <Badge
      tone={agotado ? 'danger' : 'warning'}
      solid
      icon={agotado ? HiOutlineBan : HiOutlineFire}
      className={className}
      title={label}
    >
      {label}
    </Badge>
  )
}
