import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento } from '../../api/eventos'
import { comprarEntrada } from '../../api/ventasCliente'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'
import './CheckoutPage.css'

/**
 * Compra 1 tipo de entrada (sector) — sin carrito.
 * Paso mock Mercado Pago → emite entrada → /mis-entradas.
 */
const CheckoutPage = () => {
  const { eventoId } = useParams()
  const [params] = useSearchParams()
  const sectorParam = params.get('sector') || ''
  const { user, token, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [evento, setEvento] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [paying, setPaying] = useState(false)
  const [mpPreview, setMpPreview] = useState(null)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await getEvento(eventoId)
      setEvento(data)
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar')
      setStatus('error')
    }
  }, [eventoId])

  useEffect(() => {
    load()
  }, [load])

  const sector = useMemo(() => {
    const list = evento?.sectores || evento?.lugar?.sectores || []
    if (!list.length) return null
    return (
      list.find((s) => s.id === sectorParam) ||
      list.find((s) => s.nombre === sectorParam) ||
      list[0]
    )
  }, [evento, sectorParam])

  const onPagarMock = async () => {
    if (!isAuthenticated || !sector) return
    setPaying(true)
    setError('')
    setMpPreview(null)
    try {
      // 1) Mostrar link ficticio MP
      const preferenciaId = `mp_pref_mock_${Date.now()}`
      const initPoint = `https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`
      setMpPreview({ preferenciaId, initPoint })

      // 2) Simular pago aprobado (sin carrito, 1 entrada)
      const result = await comprarEntrada(
        { idEvento: eventoId, idSector: sector.id },
        user,
        { token }
      )
      setMpPreview(result.mercadoPago || { preferenciaId, initPoint })
      // 3) Ir a mis entradas
      navigate(`/mis-entradas/${result.entrada.id}`, {
        replace: true,
        state: { bought: true },
      })
    } catch (err) {
      setError(err.message || 'No se pudo completar la compra')
      setPaying(false)
    }
  }

  return (
    <section className="checkout-page container-slim section-v-padding">
      <p>
        <Link to={`/eventos/${eventoId}`}>← Volver al evento</Link>
      </p>

      <h1>Comprar entrada</h1>

      {status === 'loading' && <p>Cargando…</p>}
      {error && <p className="checkout-page__error">{error}</p>}

      {status === 'ok' && evento && sector && (
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
              <strong>Precio:</strong> {formatPrecioDesde(sector.precio)}
            </p>
            <p className="checkout-page__muted">
              Sin carrito: una entrada por compra.
            </p>
          </div>

          <div className="checkout-mp">
            <p className="checkout-mp__label">Pago (mock Mercado Pago)</p>
            <p className="checkout-page__muted">
              Link ficticio de checkout. No hay cobro real; al confirmar se
              acredita la entrada en tu cuenta.
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
            disabled={paying}
            onClick={onPagarMock}
          >
            {paying
              ? 'Procesando pago mock…'
              : 'Pagar con Mercado Pago (mock)'}
          </button>
        </div>
      )}

      {status === 'ok' && evento && !sector && (
        <p>
          Elegí un tipo de entrada desde el{' '}
          <Link to={`/eventos/${eventoId}`}>detalle del evento</Link>.
        </p>
      )}
    </section>
  )
}

export default CheckoutPage
