import { useCallback, useEffect, useRef, useState } from 'react'
import { isAbortError } from '../api/http'

const LOADING = 'loading'
const SUCCESS = 'success'
const ERROR = 'error'
const IDLE = 'idle'

function initialState(enabled, initialData) {
  return {
    key: null,
    status: enabled ? LOADING : IDLE,
    data: initialData,
    error: null,
  }
}

/**
 * Carga asíncrona con estados consistentes, cancelación y recarga.
 *
 * - `fn(signal)` debe devolver una promesa. Recibe un AbortSignal para cancelar
 *   la request cuando cambian las dependencias o se desmonta el componente.
 * - `deps` identifica la consulta: si cambia, se vuelve a cargar y se descarta
 *   la respuesta anterior (evita condiciones de carrera).
 * - `reload()` fuerza una recarga.
 * - `setData()` permite actualizar el dato de forma optimista.
 *
 * No llama a setState de forma sincrónica dentro del effect: las transiciones
 * ocurren recién cuando la promesa resuelve o rechaza.
 *
 * @template T
 * @param {(signal: AbortSignal) => Promise<T>} fn
 * @param {unknown[]} [deps]
 * @param {{ enabled?: boolean, initialData?: T }} [options]
 */
export default function useAsync(fn, deps = [], options = {}) {
  const { enabled = true, initialData } = options
  const key = JSON.stringify(deps)

  const fnRef = useRef(fn)

  // Mantiene la última versión del callback sin convertirla en dependencia.
  useEffect(() => {
    fnRef.current = fn
  }, [fn])

  // `initialData` puede ser un objeto/array: se guarda en ref para no
  // disparar refetch si el consumidor pasa una identidad nueva en cada render.
  const initialDataRef = useRef(initialData)
  useEffect(() => {
    initialDataRef.current = initialData
  }, [initialData])

  const [state, setState] = useState(() => initialState(enabled, initialData))
  const [reloadCount, setReloadCount] = useState(0)

  // Ajuste de estado durante el render cuando cambian deps/enabled
  // (patrón recomendado por React para reiniciar una consulta).
  if (!enabled && state.status !== IDLE) {
    setState({ key, status: IDLE, data: initialData, error: null })
  } else if (enabled && (state.key !== key || state.status === IDLE)) {
    setState({ key, status: LOADING, data: initialData, error: null })
  }

  useEffect(() => {
    if (!enabled) return undefined

    let cancelled = false
    const controller = new AbortController()

    ;(async () => {
      try {
        const data = await fnRef.current(controller.signal)
        if (!cancelled) {
          setState({ key, status: SUCCESS, data, error: null })
        }
      } catch (error) {
        if (cancelled || isAbortError(error)) return
        setState({
          key,
          status: ERROR,
          data: initialDataRef.current,
          error,
        })
      }
    })()

    return () => {
      cancelled = true
      controller.abort()
    }
  }, [key, enabled, reloadCount])

  const reload = useCallback(() => {
    setState((prev) => ({ ...prev, status: LOADING, error: null }))
    setReloadCount((n) => n + 1)
  }, [])

  const setData = useCallback((updater) => {
    setState((prev) => ({
      ...prev,
      data:
        typeof updater === 'function' ? updater(prev.data) : updater,
    }))
  }, [])

  return {
    status: state.status,
    data: state.data,
    error: state.error,
    isIdle: state.status === IDLE,
    isLoading: state.status === LOADING,
    isSuccess: state.status === SUCCESS,
    isError: state.status === ERROR,
    reload,
    setData,
  }
}

export { IDLE, LOADING, SUCCESS, ERROR }
