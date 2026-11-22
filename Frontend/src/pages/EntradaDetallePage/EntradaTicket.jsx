import { QRCodeSVG } from 'qrcode.react'
import { HiOutlineCalendar, HiOutlineLocationMarker } from 'react-icons/hi'
import { formatFecha } from '../../lib/format'
import { ENTRADA_ESTADOS, ENTRADA_ESTADO_LABELS } from '../../domain/entrada'
import EventCover from '../../components/ui/EventCover'
import Badge from '../../components/ui/Badge'

const ESTADO_TONE = {
  [ENTRADA_ESTADOS.VENDIDA]: 'success',
}

/**
 * Entrada con forma de ticket: la portada ocupa todo el lado izquierdo (con los datos de la
 * entrada arriba a la derecha y fecha y lugar bajo el título) y el talón con el QR queda a la
 * derecha, separado por una línea de corte. El N.º de compra va al pie del talón.
 */
const EntradaTicket = ({ entrada, evento, mostrarQr, cancelada }) => {
  const lugar = [entrada.lugarNombre || evento?.lugar?.nombre, entrada.lugarCiudad || evento?.lugar?.ciudad].filter(Boolean).join(' · ')
  const asiento =
    entrada.fila != null && entrada.asiento != null
      ? `Fila ${entrada.fila} · Asiento ${entrada.asiento}`
      : null
  const estadoLabel = ENTRADA_ESTADO_LABELS[entrada.estado] || entrada.estado

  return (
    <article className={`ticket${cancelada ? ' ticket--cancelada' : ''}`}>
      <div className="ticket__main">
        <div className="ticket__cover">
          <EventCover
            evento={{
              nombre: entrada.eventoNombre,
              tipo: entrada.eventoTipo || evento?.tipo,
              imagenUrl: evento?.imagenUrl,
            }}
            alt=""
          />
          <div className="ticket__shade" aria-hidden />
          <div className="ticket__badges">
            <Badge tone={cancelada ? 'danger' : ESTADO_TONE[entrada.estado] || 'neutral'} solid>
              {cancelada ? 'Evento cancelado' : estadoLabel}
            </Badge>
          </div>

          <dl className="ticket__data">
            <div>
              <dt>N.º de entrada</dt>
              <dd className="ticket__mono">{String(entrada.id).slice(0, 8)}</dd>
            </div>
            <div>
              <dt>Sector</dt>
              <dd>{entrada.sectorNombre || '—'}</dd>
            </div>
            {asiento && (
              <div>
                <dt>Asiento</dt>
                <dd>{asiento}</dd>
              </div>
            )}
          </dl>

          <div className="ticket__title">
            <p className="ticket__eyebrow">Entrada</p>
            <h2>{entrada.eventoNombre}</h2>
            <ul className="ticket__meta">
              <li>
                <HiOutlineCalendar aria-hidden />
                <span>{formatFecha(entrada.eventoFecha || evento?.fechaInicio)}</span>
              </li>
              <li>
                <HiOutlineLocationMarker aria-hidden />
                <span>{lugar || '—'}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div className="ticket__tear" aria-hidden />

      <div className="ticket__stub">
        {mostrarQr ? (
          <>
            <div className="ticket__qr" aria-label="Código QR de la entrada" role="img">
              <QRCodeSVG value={entrada.codigoQR} size={188} level="M" marginSize={0} />
            </div>
            <p className="ticket__hint">Mostrá este código en el ingreso</p>
            <p className="ticket__code" title={entrada.codigoQR}>
              {entrada.codigoQR}
            </p>
          </>
        ) : cancelada ? (
          <div className="ticket__noqr">
            <strong>Sin QR de ingreso</strong>
            <p>
              El evento se canceló. El reintegro lo gestiona la plataforma y te vamos a avisar
              cuando se acredite el dinero. El trámite está en Mis entradas → Ver devoluciones.
            </p>
          </div>
        ) : (
          <div className="ticket__noqr">
            <strong>Sin QR de ingreso</strong>
            <p>
              Esta entrada no tiene QR activo (estado {estadoLabel}
              {entrada.eventoEstado === 'CANCELADO' ? ', evento cancelado' : ''}).
            </p>
          </div>
        )}

        <p className="ticket__compra">
          Compra <span className="ticket__mono">{String(entrada.idCompra || '—').slice(0, 8)}</span>
        </p>
      </div>
    </article>
  )
}

export default EntradaTicket
