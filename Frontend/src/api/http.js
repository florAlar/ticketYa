import { API_BASE_URL } from './config'

const DEFAULT_TIMEOUT_MS = 15000
export const AUTH_EXPIRED_EVENT = 'ticketya:auth-expired'
const AUTH_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh']

/** Error uniforme de la capa HTTP. Conserva `status`/`data` por compatibilidad. */
export class ApiError extends Error {
  constructor(message, { status = 0, data = null, code = null, cause } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
    this.code = code
    if (cause) this.cause = cause
  }
}

/** Detecta cancelaciones (timeout manual o AbortSignal). */
export function isAbortError(error) {
  return error?.name === 'AbortError' || error?.code === 'ABORTED'
}

function shouldNotifyAuthExpired(path, token) {
  if (!token) return false
  return (
    typeof window !== 'undefined' &&
    !AUTH_PATHS.some((authPath) => path.startsWith(authPath))
  )
}

/**
 * Cliente HTTP → API Gateway.
 *
 * @param {string} path - ej. /api/auth/login
 * @param {RequestInit & {
 *   token?: string,
 *   timeoutMs?: number,
 *   signal?: AbortSignal,
 * }} [options]
 */
export async function http(path, options = {}) {
  const {
    token,
    headers: extraHeaders,
    timeoutMs = DEFAULT_TIMEOUT_MS,
    signal,
    ...rest
  } = options

  const controller = new AbortController()
  let timedOut = false

  const timer =
    Number(timeoutMs) > 0
      ? setTimeout(() => {
          timedOut = true
          controller.abort()
        }, Number(timeoutMs))
      : null

  const onAbort = () => controller.abort()
  if (signal) {
    if (signal.aborted) controller.abort()
    else signal.addEventListener('abort', onAbort, { once: true })
  }

  const isFormData = typeof FormData !== 'undefined' && rest.body instanceof FormData
  const headers = {
    Accept: 'application/json',
    ...(rest.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extraHeaders,
  }

  let res
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers,
      signal: controller.signal,
    })
  } catch (e) {
    if (e?.name === 'AbortError') {
      throw new ApiError(
        timedOut
          ? 'La solicitud tardó demasiado. Revisá tu conexión e intentá de nuevo.'
          : 'Solicitud cancelada',
        { status: 0, code: timedOut ? 'TIMEOUT' : 'ABORTED', cause: e }
      )
    }
    throw new ApiError(
      'No se pudo conectar con el API Gateway. ¿Está levantado en ' +
        `${API_BASE_URL}? (${e?.message || 'failed to fetch'})`,
      { status: 0, code: 'NETWORK', cause: e }
    )
  } finally {
    if (timer) clearTimeout(timer)
    if (signal) signal.removeEventListener('abort', onAbort)
  }

  if (res.status === 401 && shouldNotifyAuthExpired(path, token)) {
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
  }

  const text = await res.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!res.ok) {
    const message = data?.message || data?.error || `Error ${res.status}`
    throw new ApiError(message, {
      status: res.status,
      data,
      code: data?.code || null,
    })
  }

  return data
}
