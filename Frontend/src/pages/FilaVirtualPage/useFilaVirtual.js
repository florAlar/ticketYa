import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getEvento } from '../../api/eventos'
import {
  getEstadoFila,
  getMiPosicionFila,
  habilitarFila,
  ingresarFila,
  validarFila,
} from '../../api/filaVirtual'
import { isAbortError } from '../../api/http'
import { ROLES, hasRole } from '../../auth/roles'
import { EVENTO_ESTADOS } from '../../domain/evento'
import {
  FILA_ESTADOS,
  FILA_ERROR_CODES,
  clearFilaToken,
  codigoErrorFila,
  extrasErrorFila,
  getFilaToken,
  saveFilaToken,
} from '../../domain/filaVirtual'

const POLL_MS = 5000

function esPublicado(evento) {
  return (
    String(evento?.estado || '').toUpperCase() === EVENTO_ESTADOS.PUBLICADO
  )
}

/**
 * Estado de la pantalla de fila virtual: carga evento + estado público, consulta
 * el turno propio, hace polling y ejecuta ingreso / validación / habilitación.
 *
 * `sectorParam` se conserva para volver al checkout del sector elegido.
 */
export default function useFilaVirtual({ eventoId, token, user, sectorParam }) {
  const navigate = useNavigate()

  const [evento, setEvento] = useState(null)
  const [estado, setEstado] = useState(null)
  const [posicion, setPosicion] = useState(null)
  const [noIngresado, setNoIngresado] = useState(false)
  const [status, setStatus] = useState('loading')
  const [loadError, setLoadError] = useState(null)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [pollWarning, setPollWarning] = useState('')
  const [busy, setBusy] = useState(false)
  const [tokenLocal, setTokenLocal] = useState(() => getFilaToken(eventoId))
  const [hastaPosicion, setHastaPosicion] = useState('')
  const [reloadCount, setReloadCount] = useState(0)

  const reload = useCallback(() => {
    setReloadCount((n) => n + 1)
  }, [])

  const posicionRef = useRef(posicion)
  useEffect(() => {
    posicionRef.current = posicion
  }, [posicion])

  useEffect(() => {
    setTokenLocal(getFilaToken(eventoId))
  }, [eventoId])

  const puedeUsarFila = hasRole(user, ROLES.CLIENTE, ROLES.ADMIN)
  const esAdmin = hasRole(user, ROLES.ADMIN)
  const userId = user?.id || null
  const elegible = Boolean(evento?.esAltaDemanda) && esPublicado(evento)

  const cargar = useCallback(
    async (signal) => {
      const data = await getEvento(eventoId, { signal })
      const abierto = Boolean(data?.esAltaDemanda) && esPublicado(data)
      let estadoPublico = null
      let miPosicion = null
      let sinIngreso = false

      if (abierto) {
        try {
          estadoPublico = await getEstadoFila(eventoId, { signal })
        } catch (err) {
          if (isAbortError(err)) throw err
          estadoPublico = null
        }

        if (puedeUsarFila) {
          try {
            miPosicion = await getMiPosicionFila(eventoId, {
              token,
              userId,
              signal,
            })
          } catch (err) {
            if (isAbortError(err)) throw err
            if (codigoErrorFila(err) === FILA_ERROR_CODES.NO_INGRESADO) {
              sinIngreso = true
            } else if (
              codigoErrorFila(err) !== FILA_ERROR_CODES.EVENTO_NO_ELEGIBLE
            ) {
              throw err
            }
          }
        }
      }

      return {
        evento: data,
        estado: estadoPublico,
        posicion: miPosicion,
        noIngresado: sinIngreso,
      }
    },
    [eventoId, token, userId, puedeUsarFila]
  )

  useEffect(() => {
    let cancelled = false
    const controller = new AbortController()
    setStatus('loading')
    setLoadError(null)

    ;(async () => {
      try {
        const data = await cargar(controller.signal)
        if (cancelled) return
        setEvento(data.evento)
        setEstado(data.estado)
        setPosicion(data.posicion)
        setNoIngresado(data.noIngresado)
        setStatus('success')
      } catch (err) {
        if (cancelled || isAbortError(err)) return
        setLoadError(err)
        setStatus('error')
      }
    })()

    return () => {
      cancelled = true
      controller.abort()
    }
  }, [cargar, reloadCount])

  const refrescar = useCallback(async () => {
    if (!elegible) return
    try {
      const estadoPublico = await getEstadoFila(eventoId)
      setEstado(estadoPublico)
      setPollWarning('')
    } catch (err) {
      if (!isAbortError(err)) {
        setPollWarning(err.message || 'No se pudo actualizar el estado de la fila')
      }
    }

    if (!puedeUsarFila || !posicionRef.current) return
    try {
      const miPosicion = await getMiPosicionFila(eventoId, { token, userId })
      setPosicion(miPosicion)
      setNoIngresado(false)
      if (miPosicion.estado === FILA_ESTADOS.TURNO_USADO) {
        clearFilaToken(eventoId)
        setTokenLocal(null)
      }
    } catch (err) {
      if (isAbortError(err)) return
      if (codigoErrorFila(err) === FILA_ERROR_CODES.NO_INGRESADO) {
        setPosicion(null)
        setNoIngresado(true)
      }
    }
  }, [elegible, eventoId, token, userId, puedeUsarFila])

  useEffect(() => {
    if (status !== 'success' || !elegible) return undefined
    const id = setInterval(() => {
      refrescar()
    }, POLL_MS)
    return () => clearInterval(id)
  }, [status, elegible, refrescar])

  const entrar = useCallback(async () => {
    if (busy) return
    setBusy(true)
    setError('')
    setAviso('')
    try {
      const miPosicion = await ingresarFila(eventoId, { token, userId })
      setPosicion(miPosicion)
      setNoIngresado(false)
      setAviso(
        miPosicion.reingreso
          ? 'Ya estabas en la fila: conservás tu posición.'
          : 'Entraste a la fila. Te avisamos acá cuando sea tu turno.'
      )
      await refrescar()
    } catch (err) {
      const code = codigoErrorFila(err)
      if (code === FILA_ERROR_CODES.TURNO_USADO) {
        setNoIngresado(false)
        setError('Ya usaste tu turno para este evento.')
      } else if (code === FILA_ERROR_CODES.EVENTO_NO_ELEGIBLE) {
        setError('Este evento no tiene fila virtual habilitada.')
      } else {
        setError(err.message || 'No se pudo ingresar a la fila')
      }
    } finally {
      setBusy(false)
    }
  }, [busy, eventoId, token, userId, refrescar])

  const validar = useCallback(async () => {
    if (busy) return
    setBusy(true)
    setError('')
    setAviso('')
    try {
      const data = await validarFila(eventoId, { token, userId })
      const entry = saveFilaToken(eventoId, data)
      setTokenLocal(entry)
      const query = sectorParam
        ? `?sector=${encodeURIComponent(sectorParam)}`
        : ''
      navigate(`/eventos/${eventoId}/comprar${query}`)
    } catch (err) {
      const code = codigoErrorFila(err)
      if (code === FILA_ERROR_CODES.FUERA_DE_TANDA) {
        const extras = extrasErrorFila(err)
        setPosicion((prev) =>
          prev
            ? {
                ...prev,
                posicion: extras.posicion ?? prev.posicion,
                posicionHabilitadaHasta:
                  extras.posicionHabilitadaHasta ??
                  prev.posicionHabilitadaHasta,
                delante: extras.delante ?? prev.delante,
                estado: FILA_ESTADOS.EN_ESPERA,
              }
            : prev
        )
        setError(
          `Todavía no es tu turno: faltan ${extras.delante ?? 'unos'} turnos.`
        )
      } else if (code === FILA_ERROR_CODES.TURNO_USADO) {
        clearFilaToken(eventoId)
        setTokenLocal(null)
        setPosicion((prev) =>
          prev ? { ...prev, estado: FILA_ESTADOS.TURNO_USADO } : prev
        )
        setError('Ya usaste tu turno para este evento.')
      } else if (code === FILA_ERROR_CODES.NO_INGRESADO) {
        setPosicion(null)
        setNoIngresado(true)
        setError('Tu turno ya no está disponible. Volvé a ingresar a la fila.')
      } else {
        setError(err.message || 'No se pudo validar el turno')
      }
    } finally {
      setBusy(false)
    }
  }, [busy, eventoId, token, userId, sectorParam, navigate])

  const habilitar = useCallback(async () => {
    if (busy) return
    const hasta = Number(hastaPosicion)
    if (!Number.isInteger(hasta) || hasta < 1) {
      setError('Ingresá una posición entera mayor o igual a 1.')
      return
    }
    setBusy(true)
    setError('')
    setAviso('')
    try {
      const data = await habilitarFila(eventoId, hasta, { token })
      setEstado((prev) =>
        prev
          ? { ...prev, posicionHabilitadaHasta: data.posicionHabilitadaHasta }
          : prev
      )
      setPosicion((prev) =>
        prev
          ? {
              ...prev,
              delante: Math.max(
                0,
                prev.posicion - data.posicionHabilitadaHasta
              ),
              estado:
                prev.estado === FILA_ESTADOS.TURNO_USADO
                  ? prev.estado
                  : prev.posicion <= data.posicionHabilitadaHasta
                    ? FILA_ESTADOS.HABILITADO
                    : FILA_ESTADOS.EN_ESPERA,
            }
          : prev
      )
      setAviso(
        `Tanda habilitada hasta la posición ${data.posicionHabilitadaHasta}.`
      )
      setHastaPosicion('')
      await refrescar()
    } catch (err) {
      const code = codigoErrorFila(err)
      setError(
        code === FILA_ERROR_CODES.TANDA_NO_REDUCIBLE
          ? 'La tanda no puede retroceder.'
          : err.message || 'No se pudo habilitar la tanda'
      )
    } finally {
      setBusy(false)
    }
  }, [busy, hastaPosicion, eventoId, token, refrescar])

  const paseFila = tokenLocal || getFilaToken(eventoId)
  const puedeEntrar = elegible && puedeUsarFila && !posicion

  return {
    evento,
    estado,
    posicion,
    noIngresado,
    status,
    loadError,
    error,
    aviso,
    pollWarning,
    busy,
    tokenLocal,
    paseFila,
    hastaPosicion,
    setHastaPosicion,
    elegible,
    puedeUsarFila,
    esAdmin,
    puedeEntrar,
    entrar,
    validar,
    habilitar,
    refrescar,
    reload,
  }
}
