import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento, getDisponibilidadEvento } from '../../api/eventos'
import { comprarEntrada } from '../../api/ventasCliente'
import { USE_MOCK_COMPRAS } from '../../api/config'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'
import './CheckoutPage.css'

/**
 * Compra 1 tipo de entrada (sector) — sin carrito.
 * Mock: Mercado Pago ficticio + entrada local.
 * Real: reserva → compra → pago mock BE.
 * Sin cupo comercial (disponibilidad) no se habilita el pago.
 */
const CheckoutPage = () => {
  const { eventoId } = useParams()
  const [params] = useSearchParams()
  const sectorParam = params.get('sector') || ''
  const { user, token, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [evento, setEvento] = useState(null)
  const [disponibilidad, setDisponibilidad] = useState(null)
  const [dispError, setDispError] = useState('')
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [paying, setPaying] = useState(false)
  const [mpPreview, setMpPreview] = useState(null)
  const [success, setSuccess] = useState(null)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    setDispError('')
    setSuccess(null)
    try {
      const data = await getEvento(eventoId)
      setEvento(data)
      if (!USE_MOCK_COMPRAS) {
        try {
          const disp = await getDisponibilidadEvento(eventoId, { token })
          setDisponibilidad(disp)
          setDispError('')
        } catch (err) {
          setDisponibilidad(null)
          setDispError(
            err.message ||
              'Sin stock comercial para este evento (¿se publicó con cupos?)'
          )
        }
      } else {
        setDisponibilidad(null)
        setDispError('')
      }
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar')
      setStatus('error')
    }
  }, [eventoId, token])

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

  const cupoSector = useMemo(() => {
    if (!disponibilidad?.sectores || !sector) return null
    return disponibilidad.sectores.find((c) => c.idSector === sector.id) || null
  }, [disponibilidad, sector])

  const canPay = useMemo(() => {
    if (!isAuthenticated || !sector || paying) return false
    if (USE_MOCK_COMPRAS) return true
    if (dispError || !disponibilidad) return false
    if (!cupoSector) return false
    const disp = Number(cupoSector.cantidadDisponible)
    return Number.isFinite(disp) && disp >= 1
  }, [
    isAuthenticated,
    sector,
    paying,
    dispError,
    disponibilidad,
    cupoSector,
  ])

  const onPagar = async () => {
    if (!canPay || !sector) return
    setPaying(true)
    setError('')
    setMpPreview(null)
    setSuccess(null)
    try {
      if (USE_MOCK_COMPRAS) {
        const preferenciaId = `mp_pref_mock_${Date.now()}`
        const initPoint = `https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`
        setMpPreview({ preferenciaId, initPoint })
      }

      const result = await comprarEntrada(
        { idEvento: eventoId, idSector: sector.id },
        user,
        { token, resultadoPago: 'APROBAR' }
      )

      if (result.entrada?.id) {
        setMpPreview(result.mercadoPago || mpPreview)
        navigate(`/mis-entradas/${result.entrada.id}`, {
          replace: true,
          state: { bought: true },
        })
        return
      }

      setSuccess({
        compra: result.compra,
        pago: result.pago,
        mensaje: result.mercadoPago?.mensaje,
      })
      setMpPreview(result.mercadoPago)
      try {
        const disp = await getDisponibilidadEvento(eventoId, { token })
        setDisponibilidad(disp)
        setDispError('')
      } catch {
        /* ignore */
      }
    } catch (err) {
      setError(err.message || 'No se pudo completar la compra')
    } finally {
      setPaying(false)
    }
  }

  return (
    <section className="checkout-page container-slim section-v-padding">
      <p>
        <Link to={`/eventos/${eventoId}`}>← Volver al evento</Link>
      </p>

      <h1>Comprar entrada</h1>
      {!USE_MOCK_COMPRAS && (
        <p className="checkout-page__muted">
          Modo gateway: reserva + compra + pago mock del backend.
        </p>
      )}

      {status === 'loading' && <p>Cargando…</p>}
      {error && <p className="checkout-page__error">{error}</p>}

      {success && (
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
      )}

      {status === 'ok' && evento && sector && !success && (
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
            {!USE_MOCK_COMPRAS && dispError && (
              <p className="checkout-page__error">{dispError}</p>
            )}
            {!USE_MOCK_COMPRAS && !dispError && cupoSector && (
              <p className="checkout-page__muted">
                Disponibles: {cupoSector.cantidadDisponible} /{' '}
                {cupoSector.capacidadTotal} (vendidas{' '}
                {cupoSector.cantidadVendida}, reservadas{' '}
                {cupoSector.cantidadReservada})
              </p>
            )}
            {!USE_MOCK_COMPRAS &&
              !dispError &&
              disponibilidad &&
              !cupoSector && (
                <p className="checkout-page__error">
                  Este tipo de entrada no tiene cupo comercial abierto.
                </p>
              )}
            {!USE_MOCK_COMPRAS &&
              cupoSector &&
              Number(cupoSector.cantidadDisponible) < 1 && (
                <p className="checkout-page__error">Sin stock disponible.</p>
              )}
            <p className="checkout-page__muted">
              Sin carrito: una entrada por compra.
            </p>
          </div>

          <div className="checkout-mp">
            <p className="checkout-mp__label">
              {USE_MOCK_COMPRAS
                ? 'Pago (mock Mercado Pago local)'
                : 'Pago (pasarela mock del BE)'}
            </p>
            <p className="checkout-page__muted">
              {USE_MOCK_COMPRAS
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
              : USE_MOCK_COMPRAS
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
