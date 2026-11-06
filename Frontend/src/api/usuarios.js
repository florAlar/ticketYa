import { http } from './http'
import { USE_MOCK } from './config'
import {
  createAccesoUser,
  deleteUsuario,
  getUsuarioAdmin,
  listUsuariosAdmin,
  patchUsuarioAdmin,
  setAccesoEventos,
} from './mock/usuariosStore'
import { ROLES, normalizeRol } from '../auth/roles'

const delay = (ms = 280) => new Promise((r) => setTimeout(r, ms))

function requireAdmin(user) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }
  if (String(user.rol).toUpperCase() !== 'ADMIN') {
    const err = new Error('Solo administrador')
    err.status = 403
    throw err
  }
}

/** GET /api/usuarios — ADMIN */
export async function listUsuarios(user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    return listUsuariosAdmin()
  }
  return http('/api/usuarios', { token: opts.token })
}

/** GET /api/usuarios/{id} — ADMIN */
export async function getUsuario(id, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const u = getUsuarioAdmin(id)
    if (!u) {
      const err = new Error('Usuario no encontrado')
      err.status = 404
      throw err
    }
    return u
  }
  return http(`/api/usuarios/${id}`, { token: opts.token })
}

/**
 * Actualiza rol y/o activo según contrato back:
 * - activo → PATCH /api/usuarios/{id}/estado
 * - rol → PUT /api/usuarios/{id}/roles
 * Mock: un solo patch local.
 */
export async function updateUsuario(id, patch, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const u = patchUsuarioAdmin(id, patch)
    if (!u) {
      const err = new Error('Usuario no encontrado')
      err.status = 404
      throw err
    }
    return u
  }

  let last = null
  if (patch && Object.prototype.hasOwnProperty.call(patch, 'activo')) {
    last = await http(`/api/usuarios/${id}/estado`, {
      method: 'PATCH',
      token: opts.token,
      body: JSON.stringify({ activo: patch.activo }),
    })
  }
  if (patch?.rol) {
    const rol = normalizeRol(patch.rol)
    last = await http(`/api/usuarios/${id}/roles`, {
      method: 'PUT',
      token: opts.token,
      body: JSON.stringify({ roles: [rol] }),
    })
  }
  return last
}

/**
 * Alta personal de acceso — solo mock del simulacro.
 * Contrato back: no hay POST /usuarios/acceso; roles vía PUT .../roles.
 * Cuando el BE exponga alta de PERSONAL_ACCESO, cablear acá.
 */
export async function createPersonalAcceso(payload, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay(350)
    return createAccesoUser({
      ...payload,
      rol: ROLES.ACCESO,
    })
  }
  const err = new Error(
    'Alta de personal de acceso no está en el contrato back (usar roles cuando el BE lo defina)'
  )
  err.status = 501
  throw err
}

/**
 * Asignar eventos a PERSONAL_ACCESO — solo mock (no está en endpoints-back).
 */
export async function updateAccesoEventos(id, eventosAsignados, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const u = setAccesoEventos(id, eventosAsignados)
    if (!u) {
      const err = new Error('Usuario no encontrado')
      err.status = 404
      throw err
    }
    return u
  }
  const err = new Error(
    'eventosAsignados no está en el contrato back actual'
  )
  err.status = 501
  throw err
}

/**
 * Eliminar usuario — solo mock (back no documenta DELETE /usuarios/{id}).
 */
export async function removeUsuario(id, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const ok = deleteUsuario(id)
    if (!ok) {
      const err = new Error('Usuario no encontrado')
      err.status = 404
      throw err
    }
    return { ok: true }
  }
  const err = new Error('DELETE /usuarios/{id} no está en el contrato back')
  err.status = 501
  throw err
}

/** GET /api/usuarios/{id}/roles */
export async function getUsuarioRoles(id, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const u = getUsuarioAdmin(id)
    if (!u) {
      const err = new Error('Usuario no encontrado')
      err.status = 404
      throw err
    }
    return { roles: [normalizeRol(u.rol)] }
  }
  return http(`/api/usuarios/${id}/roles`, { token: opts.token })
}
