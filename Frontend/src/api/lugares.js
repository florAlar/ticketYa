import { http } from './http'
import { USE_MOCK } from './config'

/**
 * Lugares y sectores (LUG-01) — msvc-eventos. Panel de ADMIN.
 * No hay versión mock: sin backend real el feature queda deshabilitado (FEATURE.lugares).
 */
function sinMock() {
  const err = new Error('La gestión de lugares requiere el backend real (VITE_USE_MOCK=false)')
  err.status = 501
  throw err
}

/** GET /api/lugares[?ciudad=] */
export async function listLugares(filter = {}, opts = {}) {
  if (USE_MOCK) sinMock()
  const ciudad = String(filter.ciudad || '').trim()
  const qs = ciudad ? `?ciudad=${encodeURIComponent(ciudad)}` : ''
  return http(`/api/lugares${qs}`, { token: opts.token })
}

/** GET /api/lugares/{id} — incluye sectores */
export async function getLugar(id, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/lugares/${encodeURIComponent(id)}`, { token: opts.token })
}

/** POST /api/lugares */
export async function createLugar(body, opts = {}) {
  if (USE_MOCK) sinMock()
  return http('/api/lugares', {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** PUT /api/lugares/{id} */
export async function updateLugar(id, body, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/lugares/${encodeURIComponent(id)}`, {
    method: 'PUT',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** DELETE /api/lugares/{id} — 409 si el lugar tiene eventos */
export async function deleteLugar(id, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/lugares/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    token: opts.token,
  })
}

/** POST /api/lugares/{id}/sectores */
export async function createSector(idLugar, body, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/lugares/${encodeURIComponent(idLugar)}/sectores`, {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** PUT /api/sectores/{id} */
export async function updateSector(idSector, body, opts = {}) {
  if (USE_MOCK) sinMock()
  return http(`/api/sectores/${encodeURIComponent(idSector)}`, {
    method: 'PUT',
    token: opts.token,
    body: JSON.stringify(body),
  })
}
