import { Link } from 'react-router-dom'
import {
  HiOutlineCalendar,
  HiOutlineLocationMarker,
  HiOutlineTicket,
  HiChevronRight,
} from 'react-icons/hi'
import { formatFecha, formatMoney } from '../../lib/format'
import {
  isEntradaQrExponible,
  isEntradaReintegroCancelacion,
  isEntradaEventoSuspendido,
} from '../../domain/entrada'
import { DEVOLUCION_TIPO_LABELS } from '../../domain/devolucion'
import EventCover from '../../components/ui/EventCover'
import Badge from '../../components/ui/Badge'

const DIA_MS = 24 * 60 * 60 * 1000

/** "Hoy", "Mañana", "Faltan 3 días" o null si ya pasó / no hay fecha. */
function faltante(fecha) {
  const t = fecha ? new Date(fecha).getTime() : NaN
  if (Number.isNaN(t)) return null
  const dias = Math.ceil((t - Date.now()) / DIA_MS)
  if (dias < 0) return null
  if (dias === 0) return 'Hoy'
  if (dias === 1) return 'Mañana'
  return `Faltan ${dias} días`
}

/** Entrada del cliente con forma de ticket: portada, datos y talón con la acción principal. */
export default function EntradaCard({
  entrada: e,
  pendiente,
  tramite,
  onVerDevoluciones,
}) {
  const cancelada = isEntradaReintegroCancelacion(e)
  const suspendida = isEntradaEventoSuspendido(e)
  const showQr = isEntradaQrExponible(e)
  const cuenta = cancelada ? null : faltante(e.eventoFecha)
  const asiento =
    e.fila != null && e.asiento != null ? `Fila ${e.fila} · Asiento ${e.asiento}` : null

  const clases = ['entrada-card']
  if (cancelada) clases.push('entrada-card--cancelada')
  if (suspendida) clases.push('entrada-card--suspendida')

  return (
    <li className={clases.join(' ')}>
      <div className="entrada-card__cover">
        <EventCover
          evento={{
            nombre: e.eventoNombre,
            tipo: e.eventoTipo,
            imagenUrl: e.eventoImagenUrl || e.imagenUrl,
          }}
          alt=""
        />
      </div>

      <div className="entrada-card__body">
        <div className="entrada-card__badges">
          {cancelada && <Badge tone="danger">Evento cancelado</Badge>}
          {suspendida && (
            <Badge
              tone="warning"
              title="El organizador pausó la venta nueva. Tu entrada sigue vigente."
            >
              Evento suspendido
            </Badge>
          )}
          {pendiente && !cancelada && (
            <Badge tone="info">
              {DEVOLUCION_TIPO_LABELS[pendiente.tipo] || pendiente.tipo} pendiente
            </Badge>
          )}
          {cuenta && <Badge tone="brand">{cuenta}</Badge>}
        </div>

        <h2 className="entrada-card__title">{e.eventoNombre}</h2>

        <ul className="entrada-card__meta">
          <li>
            <HiOutlineCalendar aria-hidden /> {formatFecha(e.eventoFecha)}
          </li>
          {e.lugarCiudad && (
            <li>
              <HiOutlineLocationMarker aria-hidden /> {e.lugarCiudad}
            </li>
          )}
          <li>
            <HiOutlineTicket aria-hidden /> {e.sectorNombre}
            {asiento ? ` · ${asiento}` : ''}
          </li>
        </ul>

        {cancelada && (
          <p className="entrada-card__aviso">
            Sin QR de ingreso. El reintegro lo gestiona la plataforma
            {tramite?.id ? (
              <>
                {' '}
                · Trámite <span className="mis-entradas-page__mono">{tramite.id}</span>
              </>
            ) : null}
            .
          </p>
        )}
      </div>

      <div className="entrada-card__stub">
        <span className="entrada-card__price">{formatMoney(e.precio)}</span>
        {cancelada ? (
          <button type="button" className="btn" onClick={onVerDevoluciones}>
            Ver trámite
          </button>
        ) : (
          <Link className="btn btn-primary" to={`/mis-entradas/${e.id}`}>
            {showQr ? 'Ver entrada' : 'Ver detalle'} <HiChevronRight aria-hidden />
          </Link>
        )}
        {pendiente && !cancelada && (
          <button type="button" className="entrada-card__link" onClick={onVerDevoluciones}>
            Ver solicitud
          </button>
        )}
      </div>
    </li>
  )
}
