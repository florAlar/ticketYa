import { http } from './http'
import { USE_MOCK } from './config'

/**
 * Catálogo de participantes (PAR-01) — msvc-eventos. Panel de ADMIN.
 * Alta: ORGANIZADOR o ADMIN. Editar y eliminar: solo ADMIN.
 * No hay versión mock: sin backend real el feature queda deshabilitado (FEATURE.participantes).
 */
function sinMock() {
  const err = new Error('La gestión de participantes requiere el backend real (VITE_USE_MOCK=false)')
  err.status = 501
  throw err
}

/** GET /api/participantes[?q=&tipo=] */
export async function listarParticipantes(filter = {}, opts = {}) {
  if (USE_MOCK) sinMock()
  const params = new URLSearchParams()
  if (String(filter.q || '').trim()) params.set('q', String(filter.q).trim())
  if (filter.tipo) params.set('tipo', filter.tipo)
  const qs = params.toString()
  const data = await http(`/api/participantes${qs ? `?${qs}` : ''}`, { token: opts.token })
  return Array.isArray(data) ? data : []
}

/** POST /api/participantes — 409 si el nombre ya existe */
export async function crearParticipanteAdmin(body, opts = {}) {
  if (USE_MOCK) sinMock()
  return http('/api/participantes', { method: 'POST', token: opts.token, body: JSON.stringify(body) })
}

/** PUT /api/participantes/{id} — ADMIN; 409 si el nombre lo tiene otro */
export async function actualizarParticipante(id, body, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/participantes/${encodeURIComponent(id)}`, {
    method: 'PUT',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** DELETE /api/participantes/{id} — ADMIN; 409 si participa en eventos */
export async function eliminarParticipante(id, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/participantes/${encodeURIComponent(id)}`, { method: 'DELETE', token: opts.token })
}
