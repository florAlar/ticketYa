import { http } from './http'

/** Preferencias de notificación (msvc-preferencias, Mongo). */
export async function getNotificacionesPreferencias({ token, signal } = {}) {
  return http('/api/preferencias/me/notificaciones', { token, signal })
}

export async function putNotificacionesPreferencias(body, { token } = {}) {
  return http('/api/preferencias/me/notificaciones', {
    method: 'PUT',
    token,
    body: JSON.stringify(body),
  })
}

/** Preferencias explícitas del cliente: categorías, ciudades, artistas y rango. */
export async function getPreferencias({ token, signal } = {}) {
  return http('/api/preferencias/me', { token, signal })
}

export async function putPreferencias(body, { token } = {}) {
  return http('/api/preferencias/me', {
    method: 'PUT',
    token,
    body: JSON.stringify(body),
  })
}

/** Atajo: solo artistas favoritos. */
export async function putArtistasFavoritos(artistas, { token } = {}) {
  return http('/api/preferencias/me/artistas', {
    method: 'PUT',
    token,
    body: JSON.stringify({ artistas }),
  })
}

/** Recomendaciones personalizadas (requiere JWT). */
export async function getRecomendacionesMe({ token, limit = 10, signal } = {}) {
  const data = await http(
    `/api/recomendaciones/me?limit=${encodeURIComponent(limit)}`,
    { token, signal }
  )
  return Array.isArray(data) ? data : []
}

/** Eventos similares a uno dado (público). */
export async function getRecomendacionesEvento(eventoId, { limit = 6, signal } = {}) {
  const data = await http(
    `/api/eventos/${encodeURIComponent(eventoId)}/recomendaciones?limit=${encodeURIComponent(limit)}`,
    { signal }
  )
  return Array.isArray(data) ? data : []
}

/** Historial de compras del cliente (dueño o ADMIN). */
export async function getHistorialCliente(
  idCliente,
  { token, page = 0, size = 10, signal } = {}
) {
  return http(
    `/api/clientes/${encodeURIComponent(idCliente)}/historial?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`,
    { token, signal }
  )
}
