import { Link, useParams, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { USE_MOCK } from '../../api/config'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { formatFecha } from '../../lib/format'
import useCheckout from './useCheckout'
import CheckoutTicket from './CheckoutTicket'
import CheckoutSuccess from './CheckoutSuccess'
import './CheckoutPage.css'

/**
 * Compra 1 tipo de entrada (sector) — sin carrito.
 * Sector general: 1 entrada por compra.
 * Sector numerado: hasta MAX_ASIENTOS_POR_COMPRA asientos.
 * Sin cupo comercial (disponibilidad) no se habilita el pago.
 */
const CheckoutPage = () => {
  const { eventoId } = useParams()
  const [params] = useSearchParams()
  const sectorParam = params.get('sector') || ''
  const { user, token, isAuthenticated } = useAuth()

  const checkout = useCheckout({
    eventoId,
    sectorParam,
    user,
    token,
    isAuthenticated,
  })
  const { status, loadError, reload, success, evento, sector, error } = checkout
  const { requiereFila, paseFila, filaPendiente } = checkout
  const linkFila = `/eventos/${eventoId}/fila${
    sector?.id || sectorParam
      ? `?sector=${encodeURIComponent(sector?.id || sectorParam)}`
      : ''
  }`

  return (
    <section className="checkout-page container-slim section-v-padding">
      <p>
        <Link to={`/eventos/${eventoId}`}>← Volver al evento</Link>
      </p>

      <h1>Comprar entrada</h1>
      {!USE_MOCK && (
        <p className="checkout-page__muted">
          Modo gateway: reserva + compra + pago mock del backend.
        </p>
      )}

      {status === 'loading' && <LoadingState label="Cargando evento…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {error && <p className="checkout-page__error">{error}</p>}

      {status === 'success' && filaPendiente && (
        <div className="checkout-fila">
          <p>
            <strong>Este evento usa fila virtual.</strong> Para confirmar la
            compra primero tenés que validar tu turno.
          </p>
          <Link className="btn btn-primary" to={linkFila}>
            Ir a la fila virtual
          </Link>
        </div>
      )}

      {status === 'success' && requiereFila && !filaPendiente && paseFila && (
        <p className="checkout-page__aviso">
          Pase de fila vigente hasta{' '}
          {formatFecha(paseFila.expiraEn, { timeStyle: 'short' })}. Tenés 10
          minutos para completar la compra.
        </p>
      )}

      {success?.eventoId === eventoId && <CheckoutSuccess success={success} />}

      {status === 'success' && evento && sector && success?.eventoId !== eventoId && (
        <CheckoutTicket
          checkout={checkout}
          isAuthenticated={isAuthenticated}
        />
      )}

      {status === 'success' && evento && !sector && (
        <p>
          Elegí un tipo de entrada desde el{' '}
          <Link to={`/eventos/${eventoId}`}>detalle del evento</Link>.
        </p>
      )}
    </section>
  )
}

export default CheckoutPage
