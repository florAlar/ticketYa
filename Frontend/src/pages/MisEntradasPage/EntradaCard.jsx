import { Link } from 'react-router-dom'
import { formatFecha, formatMoney } from '../../lib/format'
import {
  ENTRADA_ESTADO_LABELS,
  ENTRADA_ESTADOS,
  isEntradaQrExponible,
  isEntradaReintegroCancelacion,
  isEntradaEventoSuspendido,
} from '../../domain/entrada'
import { DEVOLUCION_TIPO_LABELS } from '../../domain/devolucion'
import { FEATURE } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'

/** Card de una entrada del cliente. */
export default function EntradaCard({
  entrada: e,
  pendiente,
  tramite,
  onVerDevoluciones,
}) {
  const cancelada = isEntradaReintegroCancelacion(e)
  const suspendida = isEntradaEventoSuspendido(e)
  const puedeReclamo =
    !cancelada &&
    e.estado === ENTRADA_ESTADOS.VENDIDA &&
    !pendiente &&
    isEntradaQrExponible(e)
  const showQr = isEntradaQrExponible(e)

  return (
    <li
      className={
        cancelada
          ? 'mis-entradas-card mis-entradas-card--cancelada'
          : suspendida
            ? 'mis-entradas-card mis-entradas-card--suspendida'
            : 'mis-entradas-card'
      }
    >
      <div className="mis-entradas-card__body">
        <h2>{e.eventoNombre}</h2>
        <p>
          {formatFecha(e.eventoFecha)}
          {e.lugarCiudad ? ` · ${e.lugarCiudad}` : ''}
        </p>
        <p className="mis-entradas-page__muted">
          {e.sectorNombre}
          {e.fila != null && e.asiento != null
            ? ` · Fila ${e.fila} Asiento ${e.asiento}`
            : ''}
          {' · '}
          {formatMoney(e.precio)}
          {!cancelada && (
            <>
              {' · '}
              <strong>{ENTRADA_ESTADO_LABELS[e.estado] || e.estado}</strong>
            </>
          )}
        </p>

        {cancelada && (
          <>
            <p className="mis-entradas-page__badge mis-entradas-page__badge--cancel">
              Evento cancelado
            </p>
            <p className="mis-entradas-page__aviso">
              No hay QR de ingreso. El reintegro lo gestiona la plataforma: te
              vamos a avisar cuando se acredite el dinero.
              {tramite?.id ? (
                <>
                  {' '}
                  Trámite:{' '}
                  <strong className="mis-entradas-page__mono">{tramite.id}</strong>
                </>
              ) : null}
            </p>
          </>
        )}

        {suspendida && !cancelada && (
          <>
            <p className="mis-entradas-page__badge mis-entradas-page__badge--suspend">
              Evento suspendido
            </p>
            <p className="mis-entradas-page__aviso">
              El organizador pausó la venta de entradas nuevas. Tu entrada sigue
              vigente y el QR de ingreso se mantiene.
            </p>
          </>
        )}

        {pendiente && !cancelada && (
          <p className="mis-entradas-page__badge">
            {DEVOLUCION_TIPO_LABELS[pendiente.tipo] || pendiente.tipo} pendiente
          </p>
        )}
      </div>
      <div className="mis-entradas-card__actions">
        {cancelada ? (
          <button type="button" className="btn" onClick={onVerDevoluciones}>
            Ver trámite
          </button>
        ) : showQr ? (
          <Link className="btn btn-primary" to={`/mis-entradas/${e.id}`}>
            Ver QR
          </Link>
        ) : (
          <Link className="btn" to={`/mis-entradas/${e.id}`}>
            Ver detalle
          </Link>
        )}
        {puedeReclamo && (
          <GateLink
            enabled={FEATURE.devoluciones}
            className="btn"
            to={`/mis-entradas/${e.id}#reclamo`}
          >
            Reclamo / devolución
          </GateLink>
        )}
        {pendiente && !cancelada && (
          <button type="button" className="btn" onClick={onVerDevoluciones}>
            Ver solicitud
          </button>
        )}
      </div>
    </li>
  )
}
