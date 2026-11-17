import { Link } from 'react-router-dom'
import { formatPrecioDesde } from '../../components/home/events/eventFormat'

/** Resultado de una compra sin entrada inmediata. */
export default function CheckoutSuccess({ success }) {
  return (
    <div className="checkout-card">
      <h2>Compra {success.compra?.estado || 'OK'}</h2>
      <p>
        <strong>Id compra:</strong> {success.compra?.id}
      </p>
      <p>
        <strong>Total:</strong> {formatPrecioDesde(success.compra?.total)}
      </p>
      <p>
        <strong>Pago:</strong> {success.pago?.estado}{' '}
        {success.pago?.idTransaccionExterna
          ? `(${success.pago.idTransaccionExterna})`
          : ''}
      </p>
      {success.mensaje && (
        <p className="checkout-page__muted">{success.mensaje}</p>
      )}
      <p>
        <Link to="/mis-entradas">Ir a Mis entradas</Link>
        {' · '}
        <Link to="/eventos">Seguir mirando eventos</Link>
      </p>
    </div>
  )
}
