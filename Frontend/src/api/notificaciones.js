import { http } from './http'

/**
 * Bandeja de notificaciones del usuario autenticado.
 * El gateway inyecta X-User-Id desde el JWT; el front solo manda el Bearer.
 */
export async function listarMisNotificaciones({ token, page = 0, size = 20, leida, signal } = {}) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  if (leida !== undefined && leida !== null) {
    params.set('leida', String(leida))
  }
  return http(`/api/notificaciones/me?${params.toString()}`, { token, signal })
}

export async function marcarNotificacionLeida(id, { token } = {}) {
  return http(`/api/notificaciones/me/${encodeURIComponent(id)}/leida`, {
    method: 'PATCH',
    token,
  })
}
