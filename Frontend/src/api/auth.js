import { http } from './http'
import { USE_MOCK_AUTH } from './config'
import { DEMO_ACCOUNTS, ROLES, normalizeRol } from '../auth/roles'
import { readUsers, writeUsers } from './mock/usuariosStore'

const delay = (ms = 350) => new Promise((r) => setTimeout(r, ms))

function toProfileUser(user) {
  return {
    id: user.id,
    email: user.email,
    nombre: user.nombre || '',
    apellido: user.apellido || '',
    ciudad: user.ciudad || '',
    rol: normalizeRol(user.rol || ROLES.CLIENTE),
    activo: user.activo !== false,
  }
}

function toSession(user) {
  return {
    token: `mock.${btoa(unescape(encodeURIComponent(user.email)))}.${Date.now()}`,
    user: toProfileUser(user),
  }
}

/** Resuelve usuario mock desde token `mock.<emailB64>.<ts>`. */
function findUserByMockToken(token) {
  if (!token || typeof token !== 'string' || !token.startsWith('mock.')) {
    return null
  }
  try {
    const b64 = token.split('.')[1]
    const email = decodeURIComponent(escape(atob(b64))).trim().toLowerCase()
    return readUsers().find((u) => u.email?.toLowerCase() === email) || null
  } catch {
    return null
  }
}

/** POST /api/auth/login — msvc-usuarios vía gateway */
export async function login({ email, password }) {
  if (USE_MOCK_AUTH) {
    await delay()
    const users = readUsers()
    const found = users.find(
      (u) => u.email.toLowerCase() === String(email).trim().toLowerCase()
    )
    if (!found || found.password !== password) {
      const err = new Error('Email o contraseña incorrectos')
      err.status = 401
      throw err
    }
    if (found.activo === false) {
      const err = new Error('Cuenta desactivada. Contactá soporte.')
      err.status = 403
      throw err
    }
    return toSession(found)
  }

  return http('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

/** POST /api/auth/register — solo rol CLIENTE público */
export async function register({ nombre, email, password }) {
  if (USE_MOCK_AUTH) {
    await delay()
    const users = readUsers()
    const emailNorm = String(email).trim().toLowerCase()
    if (users.some((u) => u.email.toLowerCase() === emailNorm)) {
      const err = new Error('Ya existe una cuenta con ese email')
      err.status = 409
      throw err
    }
    const user = {
      id: `u_${Date.now()}`,
      nombre: String(nombre).trim(),
      email: emailNorm,
      password,
      rol: ROLES.CLIENTE,
      activo: true,
      fechaRegistro: new Date().toISOString(),
    }
    users.push(user)
    writeUsers(users)
    return toSession(user)
  }

  return http('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ nombre, email, password }),
  })
}

/** POST /api/auth/refresh */
export async function refreshSession(opts = {}) {
  if (USE_MOCK_AUTH) {
    return null
  }
  return http('/api/auth/refresh', {
    method: 'POST',
    token: opts.token,
  })
}

/** POST /api/auth/logout */
export async function logout(opts = {}) {
  if (USE_MOCK_AUTH) {
    return { ok: true }
  }
  return http('/api/auth/logout', {
    method: 'POST',
    token: opts.token,
  })
}

/** GET /api/usuarios/me — perfil autenticado */
export async function getMe(opts = {}) {
  if (USE_MOCK_AUTH) {
    await delay(200)
    const found = findUserByMockToken(opts.token)
    if (!found) {
      const err = new Error('No autenticado')
      err.status = 401
      throw err
    }
    if (found.activo === false) {
      const err = new Error('Cuenta desactivada')
      err.status = 403
      throw err
    }
    return toProfileUser(found)
  }
  return http('/api/usuarios/me', {
    method: 'GET',
    token: opts.token,
  })
}

/**
 * PATCH /api/usuarios/me
 * Body BE: { nombre?, apellido?, ciudad? } — email/rol no se editan acá.
 */
export async function patchMe(body = {}, opts = {}) {
  if (USE_MOCK_AUTH) {
    await delay()
    const found = findUserByMockToken(opts.token)
    if (!found) {
      const err = new Error('No autenticado')
      err.status = 401
      throw err
    }
    const users = readUsers()
    const idx = users.findIndex((u) => u.id === found.id)
    if (idx === -1) {
      const err = new Error('Usuario no encontrado')
      err.status = 404
      throw err
    }
    const next = { ...users[idx] }
    if (body.nombre != null && String(body.nombre).trim()) {
      next.nombre = String(body.nombre).trim()
    }
    if (body.apellido != null) {
      next.apellido = String(body.apellido).trim()
    }
    if (body.ciudad != null) {
      next.ciudad = String(body.ciudad).trim()
    }
    users[idx] = next
    writeUsers(users)
    return toProfileUser(next)
  }
  return http('/api/usuarios/me', {
    method: 'PATCH',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

export { DEMO_ACCOUNTS }
