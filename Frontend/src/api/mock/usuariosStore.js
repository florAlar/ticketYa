/**
 * Store mock de usuarios admin — misma clave que auth (`ticketya_mock_users`).
 * Auth sigue siendo la fuente de login; acá operaciones admin.
 */
import { DEMO_ACCOUNTS, ROLES, normalizeRol } from '../../auth/roles'
import { toUsuarioAdminDto } from '../../domain/usuario'

export const MOCK_USERS_KEY = 'ticketya_mock_users'
const ASSIGN_KEY = 'ticketya_mock_acceso_eventos'

function ensureActivo(u) {
  return {
    ...u,
    activo: u.activo !== false,
    rol: normalizeRol(u.rol),
    eventosAsignados:
      normalizeRol(u.rol) === ROLES.ACCESO
        ? Array.isArray(u.eventosAsignados)
          ? u.eventosAsignados.map(String)
          : readAssignments()[u.id] || []
        : undefined,
  }
}

function seedUsers(users) {
  let next = Array.isArray(users) ? [...users] : []
  let dirty = false
  for (const demo of DEMO_ACCOUNTS) {
    const idx = next.findIndex(
      (u) => u.email?.toLowerCase() === demo.email.toLowerCase()
    )
    if (idx === -1) {
      next.push({
        ...demo,
        activo: true,
        fechaRegistro: '2026-01-01T00:00:00.000Z',
        eventosAsignados:
          demo.rol === ROLES.ACCESO ? ['e1', 'e2'] : undefined,
      })
      dirty = true
    } else {
      const cur = next[idx]
      next[idx] = ensureActivo({
        ...cur,
        ...demo,
        id: cur.id || demo.id,
        activo: cur.activo !== false,
        fechaRegistro: cur.fechaRegistro || '2026-01-01T00:00:00.000Z',
        eventosAsignados:
          demo.rol === ROLES.ACCESO
            ? cur.eventosAsignados || ['e1', 'e2']
            : undefined,
      })
    }
  }
  if (dirty) writeUsers(next)
  return next.map(ensureActivo)
}

export function readUsers() {
  try {
    const raw = JSON.parse(localStorage.getItem(MOCK_USERS_KEY) || '[]')
    return seedUsers(raw)
  } catch {
    return seedUsers([])
  }
}

export function writeUsers(users) {
  localStorage.setItem(MOCK_USERS_KEY, JSON.stringify(users))
}

function readAssignments() {
  try {
    return JSON.parse(localStorage.getItem(ASSIGN_KEY) || '{}')
  } catch {
    return {}
  }
}

function writeAssignments(map) {
  localStorage.setItem(ASSIGN_KEY, JSON.stringify(map))
}

export function listUsuariosAdmin() {
  return readUsers().map(toUsuarioAdminDto)
}

export function getUsuarioAdmin(id) {
  const u = readUsers().find((x) => x.id === String(id))
  return u ? toUsuarioAdminDto(u) : null
}

/**
 * Admin: solo rol + activo.
 * ACCESO no usa desactivar → hay que deleteUsuario.
 */
export function patchUsuarioAdmin(id, { rol, activo }) {
  const users = readUsers()
  const idx = users.findIndex((u) => u.id === String(id))
  if (idx === -1) return null
  const cur = users[idx]
  const nextRol =
    rol != null ? normalizeRol(rol) : normalizeRol(cur.rol)

  if (normalizeRol(cur.rol) === ROLES.ACCESO || nextRol === ROLES.ACCESO) {
    // Personal de acceso: no se desactiva; solo rol/asignaciones o borrado
    if (activo === false) {
      const err = new Error(
        'El personal de acceso no se desactiva: eliminá el usuario'
      )
      err.status = 400
      throw err
    }
  }

  users[idx] = {
    ...cur,
    rol: nextRol,
    activo:
      nextRol === ROLES.ACCESO
        ? true
        : activo === undefined
          ? cur.activo !== false
          : Boolean(activo),
    eventosAsignados:
      nextRol === ROLES.ACCESO
        ? cur.eventosAsignados || []
        : undefined,
  }
  writeUsers(users)
  return toUsuarioAdminDto(users[idx])
}

/** Alta personal de acceso + eventos asignados. */
export function createAccesoUser({
  nombre,
  email,
  password,
  eventosAsignados = [],
}) {
  const users = readUsers()
  const emailNorm = String(email || '')
    .trim()
    .toLowerCase()
  if (!emailNorm || !password || !nombre?.trim()) {
    const err = new Error('Nombre, email y contraseña son obligatorios')
    err.status = 400
    throw err
  }
  if (users.some((u) => u.email.toLowerCase() === emailNorm)) {
    const err = new Error('Ya existe un usuario con ese email')
    err.status = 409
    throw err
  }
  const ids = (eventosAsignados || []).map(String).filter(Boolean)
  const user = {
    id: `u_acc_${Date.now()}`,
    nombre: String(nombre).trim(),
    email: emailNorm,
    password: String(password),
    rol: ROLES.ACCESO,
    activo: true,
    eventosAsignados: ids,
    fechaRegistro: new Date().toISOString(),
  }
  users.push(user)
  writeUsers(users)
  const map = readAssignments()
  map[user.id] = ids
  writeAssignments(map)
  return toUsuarioAdminDto(user)
}

/** Set eventos de un ACCESO. */
export function setAccesoEventos(id, eventosAsignados) {
  const users = readUsers()
  const idx = users.findIndex((u) => u.id === String(id))
  if (idx === -1) return null
  if (normalizeRol(users[idx].rol) !== ROLES.ACCESO) {
    const err = new Error('Solo personal de acceso tiene eventos asignados')
    err.status = 400
    throw err
  }
  const ids = (eventosAsignados || []).map(String).filter(Boolean)
  users[idx] = { ...users[idx], eventosAsignados: ids, activo: true }
  writeUsers(users)
  const map = readAssignments()
  map[id] = ids
  writeAssignments(map)
  return toUsuarioAdminDto(users[idx])
}

/** Borrado (personal de acceso u otros si hiciera falta). */
export function deleteUsuario(id) {
  const users = readUsers()
  const next = users.filter((u) => u.id !== String(id))
  if (next.length === users.length) return false
  writeUsers(next)
  const map = readAssignments()
  delete map[id]
  writeAssignments(map)
  return true
}
