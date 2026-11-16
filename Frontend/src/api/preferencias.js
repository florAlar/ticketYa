import { http } from './http'

/** Preferencias de notificación (msvc-preferencias, Mongo). */
export async function getNotificacionesPreferencias({ token } = {}) {
  return http('/api/preferencias/me/notificaciones', { token })
}

export async function putNotificacionesPreferencias(body, { token } = {}) {
  return http('/api/preferencias/me/notificaciones', {
    method: 'PUT',
    token,
    body: JSON.stringify(body),
  })
}
