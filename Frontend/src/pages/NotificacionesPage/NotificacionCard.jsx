import { Link } from 'react-router-dom'
import {
  canalNotificacionLabel,
  estadoNotificacionLabel,
  formatFechaNotificacion,
  tipoNotificacionLabel,
} from '../../domain/notificacion'

/** Card de una notificación de la bandeja. */
export default function NotificacionCard({ notificacion, onMarkRead }) {
  const n = notificacion
  const canales =
    Array.isArray(n.canales) && n.canales.length > 0 ? n.canales : [n.canal]

  return (
    <li className={`notificacion-card ${n.leida ? '' : 'is-unread'}`}>
      <div className="notificacion-card__top">
        <span className="notificacion-card__type">
          {tipoNotificacionLabel(n.tipo)}
        </span>
        <span className={`notificacion-card__channel is-${String(n.canal).toLowerCase()}`}>
          {canales.map(canalNotificacionLabel).join(' + ')}
        </span>
        <span className="notificacion-card__date">
          {formatFechaNotificacion(n.fechaCreacion)}
        </span>
      </div>

      <h2 className="notificacion-card__subject">{n.asunto}</h2>
      <p className="notificacion-card__text">{n.contenido}</p>

      <div className="notificacion-card__foot">
        <span className="notificacion-card__state">
          {estadoNotificacionLabel(n.estado)}
          {n.fechaEnvio ? ` · ${formatFechaNotificacion(n.fechaEnvio)}` : ''}
        </span>
        <span className="notificacion-card__actions">
          {n.idEventoOrigen && (
            <Link to={`/eventos/${n.idEventoOrigen}`}>Ver evento</Link>
          )}
          {!n.leida && (
            <button type="button" onClick={() => onMarkRead(n)}>
              Marcar leída
            </button>
          )}
        </span>
      </div>
    </li>
  )
}
