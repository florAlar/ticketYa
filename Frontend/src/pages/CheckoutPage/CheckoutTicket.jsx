import { Link } from 'react-router-dom'
import { HiOutlineCalendar, HiOutlineLocationMarker } from 'react-icons/hi'
import { USE_MOCK } from '../../api/config'
import { MAX_ASIENTOS_POR_COMPRA, codigoAsiento } from '../../domain/asiento'
import SeatPicker from '../../components/seats/SeatPicker'
import EventCover from '../../components/ui/EventCover'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'

/**
 * Pantalla de compra de una sola vista (sin scroll en escritorio):
 * a la izquierda los asientos (o la entrada única) y a la derecha el resumen con el pago.
 */
export default function CheckoutTicket({ checkout, isAuthenticated }) {
  const {
    evento,
    disponibilidad,
    dispError,
    sector,
    cupoSector,
    usaPicker,
    numeradoSinLayout,
    ocupados,
    seleccion,
    canPay,
    paying,
    mpPreview,
    aviso,
    setSeleccion,
    setAviso,
    onActualizarAsientos,
    onPagar,
  } = checkout

  const cantidad = usaPicker ? seleccion.length : 1
  const total = Number(sector.precio) * cantidad

  const avisos = [
    !USE_MOCK && dispError && dispError,
    !USE_MOCK && !dispError && disponibilidad && !cupoSector
      ? 'Este tipo de entrada no tiene cupo comercial abierto.'
      : null,
    !USE_MOCK && cupoSector && Number(cupoSector.cantidadDisponible) < 1
      ? 'Sin stock disponible.'
      : null,
    numeradoSinLayout
      ? 'Este sector es numerado pero todavía no tiene layout de asientos configurado.'
      : null,
  ].filter(Boolean)

  const quitar = (a) => {
    if (paying) return
    setAviso('')
    setSeleccion(
      seleccion.filter(
        (x) => codigoAsiento(x.fila, x.numero) !== codigoAsiento(a.fila, a.numero)
      )
    )
  }

  return (
    <div className="checkout-app">
      <div className="checkout-main">
        {avisos.map((m) => (
          <p key={m} className="checkout-page__error">
            {m}
          </p>
        ))}
        {aviso && <p className="checkout-page__aviso">{aviso}</p>}

        {usaPicker ? (
          <SeatPicker
            filas={Number(sector.filas)}
            asientosPorFila={Number(sector.asientosPorFila)}
            ocupados={ocupados}
            seleccion={seleccion}
            onChange={(sel) => {
              setAviso('')
              setSeleccion(sel)
            }}
            max={MAX_ASIENTOS_POR_COMPRA}
            disabled={paying}
            acciones={
              !USE_MOCK ? (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={onActualizarAsientos}
                  disabled={paying}
                >
                  Actualizar asientos
                </button>
              ) : null
            }
          />
        ) : (
          <div className="checkout-single">
            <p className="checkout-single__kicker">Tu entrada</p>
            <h2>{sector.nombre}</h2>
            <p className="checkout-single__detalle">Una entrada por compra</p>
            {!USE_MOCK && !dispError && cupoSector && (
              <p className="checkout-page__muted">
                Quedan {cupoSector.cantidadDisponible} de {cupoSector.capacidadTotal}
              </p>
            )}
          </div>
        )}
      </div>

      <aside className="checkout-summary">
        <div className="checkout-summary__cover">
          <EventCover evento={evento} alt="" position="center 30%" />
          <div className="checkout-summary__shade" aria-hidden />
          <h2>{evento.nombre}</h2>
        </div>

        <div className="checkout-summary__body">
          <ul className="checkout-summary__facts">
            <li>
              <HiOutlineCalendar aria-hidden /> {formatEventoFecha(evento.fechaInicio)}
            </li>
            {(evento.lugar?.nombre || evento.lugar?.ciudad) && (
              <li>
                <HiOutlineLocationMarker aria-hidden />{' '}
                {[evento.lugar?.nombre, evento.lugar?.ciudad].filter(Boolean).join(' · ')}
              </li>
            )}
          </ul>

          <dl className="checkout-summary__lines">
            <div>
              <dt>Entrada</dt>
              <dd>{sector.nombre}</dd>
            </div>
            <div>
              <dt>Precio{usaPicker ? ' por asiento' : ''}</dt>
              <dd>{formatPrecioDesde(sector.precio)}</dd>
            </div>
          </dl>

          {usaPicker && (
            <div className="checkout-summary__seats">
              <p>
                Asientos ({seleccion.length}/{MAX_ASIENTOS_POR_COMPRA})
              </p>
              {seleccion.length === 0 ? (
                <span className="checkout-page__muted">Elegí en el mapa</span>
              ) : (
                <ul>
                  {seleccion.map((a) => (
                    <li key={codigoAsiento(a.fila, a.numero)}>
                      <button
                        type="button"
                        className="checkout-chip"
                        onClick={() => quitar(a)}
                        disabled={paying}
                        title="Quitar"
                      >
                        {codigoAsiento(a.fila, a.numero)} ×
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        <div className="checkout-summary__pay">
          <div className="checkout-summary__total">
            <span>Total</span>
            <strong>{formatPrecioDesde(total)}</strong>
          </div>
          {usaPicker && seleccion.length > 0 && (
            <span className="checkout-page__muted">
              {seleccion.length} × {formatPrecioDesde(sector.precio)}
            </span>
          )}

          <button
            type="button"
            className="btn btn-primary btn-lg btn-block"
            disabled={!canPay}
            onClick={onPagar}
          >
            {paying ? 'Procesando…' : 'Confirmar compra'}
          </button>
          <p className="checkout-summary__note">
            Pago simulado: no se cobra dinero real.
          </p>
          {mpPreview?.initPoint && (
            <p className="checkout-summary__note">Link de pago: {mpPreview.initPoint} (simulado)</p>
          )}
          {!isAuthenticated && (
            <p className="checkout-page__muted">
              Tenés que <Link to="/login">iniciar sesión</Link>.
            </p>
          )}
        </div>
      </aside>
    </div>
  )
}
