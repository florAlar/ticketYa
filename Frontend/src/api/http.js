import { API_BASE_URL } from './config'

/**
 * Cliente HTTP mínimo → API Gateway.
 * @param {string} path - ej. /api/auth/login
 * @param {RequestInit & { token?: string }} options
 */
export async function http(path, options = {}) {
  const { token, headers: extraHeaders, ...rest } = options
  const headers = {
    Accept: 'application/json',
    ...(rest.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extraHeaders,
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers,
  })

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
    const err = new Error(message)
    err.status = res.status
    err.data = data
    throw err
  }

  return data
}
