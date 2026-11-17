import { Link } from 'react-router-dom'
import { USE_MOCK } from '../../api/config'
import { MAX_ASIENTOS_POR_COMPRA } from '../../domain/asiento'
import SeatPicker from '../../components/seats/SeatPicker'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'

/** Card de compra: evento, cupo, asientos y botón de pago. */
export default function CheckoutTicket({ checkout, eventoId, isAuthenticated }) {
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

  return (
    <div className="checkout-card">
      <h2>{evento.nombre}</h2>
      <p className="checkout-page__muted">
        {formatEventoFecha(evento.fechaInicio)}
        {evento.lugar?.ciudad ? ` · ${evento.lugar.ciudad}` : ''}
      </p>

      <div className="checkout-card__ticket">
        <p>
          <strong>Tipo:</strong> {sector.nombre}
        </p>
        <p>
          <strong>Precio{usaPicker ? ' por asiento' : ''}:</strong>{' '}
          {formatPrecioDesde(sector.precio)}
        </p>
        {!USE_MOCK && dispError && (
          <p className="checkout-page__error">{dispError}</p>
        )}
        {!USE_MOCK && !dispError && cupoSector && (
          <p className="checkout-page__muted">
            Disponibles: {cupoSector.cantidadDisponible} /{' '}
            {cupoSector.capacidadTotal} (vendidas {cupoSector.cantidadVendida},
            reservadas {cupoSector.cantidadReservada})
          </p>
        )}
        {!USE_MOCK && !dispError && disponibilidad && !cupoSector && (
          <p className="checkout-page__error">
            Este tipo de entrada no tiene cupo comercial abierto.
          </p>
        )}
        {!USE_MOCK && cupoSector && Number(cupoSector.cantidadDisponible) < 1 && (
          <p className="checkout-page__error">Sin stock disponible.</p>
        )}
        {numeradoSinLayout && (
          <p className="checkout-page__error">
            Este sector es numerado pero todavía no tiene layout de asientos
            configurado.
          </p>
        )}
        {usaPicker ? (
          <>
            <p className="checkout-page__muted">
              Elegí hasta {MAX_ASIENTOS_POR_COMPRA} asientos: primero la fila y
              después los asientos.
            </p>
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
            />
            {!USE_MOCK && (
              <button
                type="button"
                className="btn"
                onClick={onActualizarAsientos}
                disabled={paying}
              >
                Actualizar asientos
              </button>
            )}
            <p>
              <strong>Total:</strong>{' '}
              {formatPrecioDesde(Number(sector.precio) * seleccion.length)}
              {seleccion.length
                ? ` (${seleccion.length} × ${formatPrecioDesde(sector.precio)})`
                : ''}
            </p>
          </>
        ) : (
          <p className="checkout-page__muted">
            Sin carrito: una entrada por compra.
          </p>
        )}
        {aviso && <p className="checkout-page__aviso">{aviso}</p>}
      </div>

      <div className="checkout-mp">
        <p className="checkout-mp__label">
          {USE_MOCK
            ? 'Pago (mock Mercado Pago local)'
            : 'Pago (pasarela mock del BE)'}
        </p>
        <p className="checkout-page__muted">
          {USE_MOCK
            ? 'Link ficticio de checkout. No hay cobro real; al confirmar se acredita la entrada en tu cuenta (LS).'
            : 'Se crea reserva TTL, compra con Idempotency-Key y pago APROBAR en msvc-ventas.'}
        </p>
        {mpPreview?.initPoint && (
          <p className="checkout-mp__link">
            <a
              href={mpPreview.initPoint}
              target="_blank"
              rel="noreferrer"
              onClick={(e) => e.preventDefault()}
            >
              {mpPreview.initPoint}
            </a>
            <span className="checkout-page__muted"> (simulado)</span>
          </p>
        )}
      </div>

      <button
        type="button"
        className="btn btn-primary"
        disabled={!canPay}
        onClick={onPagar}
      >
        {paying
          ? 'Procesando…'
          : USE_MOCK
            ? 'Pagar con Mercado Pago (mock)'
            : 'Confirmar compra (gateway)'}
      </button>
      {!isAuthenticated && (
        <p className="checkout-page__muted">
          Tenés que{' '}
          <Link
            to={`/login?next=${encodeURIComponent(
              `/eventos/${eventoId}/comprar?sector=${sector.id}`
            )}`}
          >
            iniciar sesión
          </Link>
          .
        </p>
      )}
    </div>
  )
}
