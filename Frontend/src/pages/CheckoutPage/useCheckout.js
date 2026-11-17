import { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useAsync from '../../hooks/useAsync'
import { getEvento, getDisponibilidadEvento } from '../../api/eventos'
import { isAbortError } from '../../api/http'
import { comprarEntrada } from '../../api/ventasCliente'
import { USE_MOCK } from '../../api/config'
import {
  MAX_ASIENTOS_POR_COMPRA,
  asientosDeMensajeConflicto,
  codigoAsiento,
  sectorTieneLayout,
} from '../../domain/asiento'

/** Carga de evento/cupo, selección de asientos y pago. */
export default function useCheckout({ eventoId, sectorParam, user, token, isAuthenticated }) {
  const navigate = useNavigate()

  const [paying, setPaying] = useState(false)
  const [mpPreview, setMpPreview] = useState(null)
  const [success, setSuccess] = useState(null)
  const [seleccion, setSeleccion] = useState([])
  const [aviso, setAviso] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(
    async (signal) => {
      const data = await getEvento(eventoId, { signal })
      if (USE_MOCK) {
        return { evento: data, disponibilidad: null, dispError: '' }
      }
      try {
        const disp = await getDisponibilidadEvento(eventoId, { token, signal })
        return { evento: data, disponibilidad: disp, dispError: '' }
      } catch (err) {
        if (isAbortError(err)) throw err
        return {
          evento: data,
          disponibilidad: null,
          dispError:
            err.message ||
            'Sin stock comercial para este evento (¿se publicó con cupos?)',
        }
      }
    },
    [eventoId, token]
  )

  const {
    data: loaded,
    status: loadStatus,
    error: loadError,
    reload,
    setData: setLoaded,
  } = useAsync(load, [eventoId, token], { enabled: Boolean(eventoId) })

  const evento = loaded?.evento || null
  const disponibilidad = loaded?.disponibilidad || null
  const dispError = loaded?.dispError || ''
  const status =
    loadStatus === 'success' ? 'success' : loadStatus === 'error' ? 'error' : 'loading'

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

  const esNumerado = Boolean(sector?.esNumerado)
  const tieneLayout = sectorTieneLayout(sector)
  const usaPicker = esNumerado && tieneLayout
  const numeradoSinLayout = esNumerado && !tieneLayout && !USE_MOCK
  const ocupados = cupoSector?.asientosOcupados || []
  const cantidad = usaPicker ? seleccion.length : 1

  const canPay = useMemo(() => {
    if (!isAuthenticated || !sector || paying) return false
    if (numeradoSinLayout) return false
    if (usaPicker && (cantidad < 1 || cantidad > MAX_ASIENTOS_POR_COMPRA)) {
      return false
    }
    if (USE_MOCK) return true
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
   * Recarga la ocupación y quita de la selección SOLO los asientos que dejaron
   * de estar libres (los tomados y/o los informados en un 409).
   */
  const reconciliarSeleccion = async (codigosConflicto = []) => {
    let ocupadosFrescos = ocupados
    try {
      const disp = await getDisponibilidadEvento(eventoId, { token })
      setLoaded((prev) => ({
        ...(prev || {}),
        evento: prev?.evento || evento,
        disponibilidad: disp,
        dispError: '',
      }))
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
      if (USE_MOCK) {
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
        navigate(
          result.entradas?.length > 1
            ? '/mis-entradas'
            : `/mis-entradas/${result.entrada.id}`,
          { replace: true, state: { bought: true } }
        )
        return
      }

      setSuccess({
        eventoId,
        compra: result.compra,
        pago: result.pago,
        mensaje: result.mercadoPago?.mensaje,
      })
      setMpPreview(result.mercadoPago)
      try {
        const disp = await getDisponibilidadEvento(eventoId, { token })
        setLoaded((prev) => ({
          ...(prev || {}),
          evento: prev?.evento || evento,
          disponibilidad: disp,
          dispError: '',
        }))
      } catch {
        /* ignore */
      }
    } catch (err) {
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

  return {
    evento,
    disponibilidad,
    dispError,
    status,
    loadError,
    reload,
    sector,
    cupoSector,
    usaPicker,
    numeradoSinLayout,
    ocupados,
    seleccion,
    canPay,
    paying,
    mpPreview,
    success,
    aviso,
    error,
    setSeleccion,
    setAviso,
    onActualizarAsientos,
    onPagar,
  }
}
