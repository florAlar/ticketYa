import { http } from './http'
import { USE_MOCK_AUTH } from './config'
import { DEMO_ACCOUNTS, ROLES, normalizeRol } from '../auth/roles'
import { readUsers, writeUsers } from './mock/usuariosStore'

const delay = (ms = 350) => new Promise((r) => setTimeout(r, ms))

function toSession(user) {
  return {
    token: `mock.${btoa(unescape(encodeURIComponent(user.email)))}.${Date.now()}`,
    user: {
      id: user.id,
      email: user.email,
      nombre: user.nombre,
      rol: normalizeRol(user.rol || ROLES.CLIENTE),
      activo: user.activo !== false,
    },
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
    return null
  }
  return http('/api/usuarios/me', {
    method: 'GET',
    token: opts.token,
  })
}

/** PATCH /api/usuarios/me */
export async function patchMe(body, opts = {}) {
  if (USE_MOCK_AUTH) {
    const err = new Error('patchMe no disponible en mock')
    err.status = 501
    throw err
  }
  return http('/api/usuarios/me', {
    method: 'PATCH',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

export { DEMO_ACCOUNTS }
