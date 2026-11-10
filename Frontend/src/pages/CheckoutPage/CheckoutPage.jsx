import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento, getDisponibilidadEvento } from '../../api/eventos'
import { comprarEntrada } from '../../api/ventasCliente'
import { USE_MOCK_COMPRAS } from '../../api/config'
import {
  MAX_ASIENTOS_POR_COMPRA,
  asientosDeMensajeConflicto,
  codigoAsiento,
  sectorTieneLayout,
} from '../../domain/asiento'
import SeatPicker from '../../components/seats/SeatPicker'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'
import './CheckoutPage.css'

/**
 * Compra 1 tipo de entrada (sector) — sin carrito.
 * Sector general: 1 entrada por compra (flujo de siempre).
 * Sector numerado: el cliente elige hasta MAX_ASIENTOS_POR_COMPRA asientos (fila → asientos).
 * Mock: Mercado Pago ficticio + entrada(s) local(es).
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
  /** Asientos elegidos (solo sector numerado): [{fila, numero}] */
  const [seleccion, setSeleccion] = useState([])
  /** Aviso informativo (p. ej. asientos quitados por conflicto); no es un error fatal. */
  const [aviso, setAviso] = useState('')

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

  // Sector numerado con layout → selector de asientos. Sin layout: en gateway no se puede vender
  // (el BE lo rechaza); en mock (datos viejos en LS) se cae al flujo general.
  const esNumerado = Boolean(sector?.esNumerado)
  const tieneLayout = sectorTieneLayout(sector)
  const usaPicker = esNumerado && tieneLayout
  const numeradoSinLayout = esNumerado && !tieneLayout && !USE_MOCK_COMPRAS
  const ocupados = cupoSector?.asientosOcupados || []
  const cantidad = usaPicker ? seleccion.length : 1

  const canPay = useMemo(() => {
    if (!isAuthenticated || !sector || paying) return false
    if (numeradoSinLayout) return false
    if (usaPicker && (cantidad < 1 || cantidad > MAX_ASIENTOS_POR_COMPRA)) {
      return false
    }
    if (USE_MOCK_COMPRAS) return true
    if (dispError || !disponibilidad) return false
    if (!cupoSector) return false
    const disp = Number(cupoSector.cantidadDisponible)
    return Number.isFinite(disp) && disp >= cantidad
  }, [
    isAuthenticated,
    sector,
    paying,
    numeradoSinLayout,
    usaPicker,
    cantidad,
    dispError,
    disponibilidad,
    cupoSector,
  ])

  /**
   * Recarga la ocupación y quita de la selección SOLO los asientos que dejaron de estar libres
   * (los que ya figuran tomados y/o los que el BE informó en un 409); el resto se conserva.
   * @returns {string[]} códigos de los asientos quitados
   */
  const reconciliarSeleccion = async (codigosConflicto = []) => {
    let ocupadosFrescos = ocupados
    try {
      const disp = await getDisponibilidadEvento(eventoId, { token })
      setDisponibilidad(disp)
      setDispError('')
      const cupo = disp?.sectores?.find((c) => c.idSector === sector.id)
      ocupadosFrescos = cupo?.asientosOcupados || []
    } catch {
      /* sin refresco: nos quedamos con lo que informó el BE en el conflicto */
    }
    const tomados = new Set([
      ...ocupadosFrescos.map((a) => codigoAsiento(a.fila, a.numero)),
      ...codigosConflicto,
    ])
    const quitados = seleccion.filter((a) =>
      tomados.has(codigoAsiento(a.fila, a.numero))
    )
    if (quitados.length) {
      setSeleccion((prev) =>
        prev.filter((a) => !tomados.has(codigoAsiento(a.fila, a.numero)))
      )
    }
    return quitados.map((a) => codigoAsiento(a.fila, a.numero))
  }

  const onActualizarAsientos = async () => {
    setError('')
    const quitados = await reconciliarSeleccion()
    setAviso(
      quitados.length
        ? `Ya no estaban disponibles y se quitaron de tu selección: ${quitados.join(', ')}.`
        : 'Disponibilidad actualizada.'
    )
  }

  const onPagar = async () => {
    if (!canPay || !sector) return
    setPaying(true)
    setError('')
    setAviso('')
    setMpPreview(null)
    setSuccess(null)
    try {
      if (USE_MOCK_COMPRAS) {
        const preferenciaId = `mp_pref_mock_${Date.now()}`
        const initPoint = `https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`
        setMpPreview({ preferenciaId, initPoint })
      }

      const result = await comprarEntrada(
        {
          idEvento: eventoId,
          idSector: sector.id,
          asientos: usaPicker ? seleccion : undefined,
        },
        user,
        { token, resultadoPago: 'APROBAR' }
      )

      if (result.entrada?.id) {
        setMpPreview(result.mercadoPago || mpPreview)
        // varias entradas (asientos) → listado; una sola → su detalle
        navigate(
          result.entradas?.length > 1
            ? '/mis-entradas'
            : `/mis-entradas/${result.entrada.id}`,
          { replace: true, state: { bought: true } }
        )
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
      // 409 por asiento tomado: quitar solo los conflictivos y conservar el resto de la selección
      if (usaPicker && err.status === 409 && /asiento/i.test(err.message || '')) {
        const informados = asientosDeMensajeConflicto(err.message).map((a) =>
          codigoAsiento(a.fila, a.numero)
        )
        const quitados = await reconciliarSeleccion(informados)
        if (quitados.length) {
          setAviso(
            `Estos asientos ya no están disponibles y se quitaron de tu selección: ${quitados.join(', ')}. ` +
              'Revisá el resto y volvé a confirmar.'
          )
          return
        }
      }
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
              <strong>Precio{usaPicker ? ' por asiento' : ''}:</strong>{' '}
              {formatPrecioDesde(sector.precio)}
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
            {numeradoSinLayout && (
              <p className="checkout-page__error">
                Este sector es numerado pero todavía no tiene layout de
                asientos configurado.
              </p>
            )}
            {usaPicker ? (
              <>
                <p className="checkout-page__muted">
                  Elegí hasta {MAX_ASIENTOS_POR_COMPRA} asientos: primero la
                  fila y después los asientos.
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
                {!USE_MOCK_COMPRAS && (
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
