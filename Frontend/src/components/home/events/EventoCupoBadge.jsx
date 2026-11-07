import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  cupoLabelDeEvento,
  isEventoAgotado,
} from '../../../domain/evento'

/**
 * Badge de estado de evento o cupo en cards.
 * Prioridad: Cancelado > Suspendido > cupo (Agotado / últimos / …).
 */
export default function EventoCupoBadge({ evento, className = '' }) {
  if (!evento) return null

  const estado = String(evento.estado || '').toUpperCase()
  let label = null
  let kind = 'aviso'

  if (estado === EVENTO_ESTADOS.CANCELADO || estado === 'CANCELADO') {
    label = EVENTO_ESTADO_LABELS.CANCELADO || 'Cancelado'
    kind = 'cancel'
  } else if (estado === EVENTO_ESTADOS.SUSPENDIDO || estado === 'SUSPENDIDO') {
    label = EVENTO_ESTADO_LABELS.SUSPENDIDO || 'Suspendido'
    kind = 'suspend'
  } else {
    label = cupoLabelDeEvento(evento)
    if (!label) return null
    kind = isEventoAgotado(evento) ? 'agotado' : 'aviso'
  }

  const classes = [
    'evento-cupo-badge',
    kind === 'cancel' || kind === 'agotado'
      ? 'evento-cupo-badge--agotado'
      : kind === 'suspend'
        ? 'evento-cupo-badge--suspend'
        : 'evento-cupo-badge--aviso',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <span className={classes} title={label}>
      {label}
    </span>
  )
}
