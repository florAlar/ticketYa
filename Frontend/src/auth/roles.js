/**
 * Roles de producto (JWT / msvc-usuarios).
 * Valor canónico back: PERSONAL_ACCESO (endpoints-back.md).
 * Alias legado ACCESO se normaliza al canónico.
 */
export const ROLES = {
  ADMIN: 'ADMIN',
  ORGANIZADOR: 'ORGANIZADOR',
  CLIENTE: 'CLIENTE',
  /** Canónico backend */
  ACCESO: 'PERSONAL_ACCESO',
}

export const ROLE_LABELS = {
  [ROLES.ADMIN]: 'Administrador',
  [ROLES.ORGANIZADOR]: 'Organizador',
  [ROLES.CLIENTE]: 'Cliente',
  [ROLES.ACCESO]: 'Personal de acceso',
  // por si llega el string corto en LS viejo
  ACCESO: 'Personal de acceso',
}

/** Cuentas seed solo mock — no se usan con gateway real. */
export const DEMO_ACCOUNTS = [
  {
    id: 'u_demo',
    nombre: 'Usuario Demo',
    email: 'demo@ticketya.com',
    password: 'demo1234',
    rol: ROLES.CLIENTE,
  },
  {
    id: 'u_org',
    nombre: 'Org Demo',
    email: 'org@ticketya.com',
    password: 'org1234',
    rol: ROLES.ORGANIZADOR,
  },
  {
    id: 'u_admin',
    nombre: 'Admin Demo',
    email: 'admin@ticketya.com',
    password: 'admin1234',
    rol: ROLES.ADMIN,
  },
  {
    id: 'u_acceso',
    nombre: 'Acceso Demo',
    email: 'acceso@ticketya.com',
    password: 'acceso1234',
    rol: ROLES.ACCESO,
  },
]

export function normalizeRol(rol) {
  const r = String(rol || ROLES.CLIENTE).toUpperCase()
  if (r === 'ACCESO' || r === 'PERSONAL_ACCESO') return ROLES.ACCESO
  return Object.values(ROLES).includes(r) ? r : ROLES.CLIENTE
}

export function hasRole(user, ...allowed) {
  if (!user) return false
  const rol = normalizeRol(user.rol)
  const allowedNorm = allowed.map((a) => normalizeRol(a))
  return allowedNorm.includes(rol)
}

/** Diseña / publica eventos (org) o también admin. */
export function canManageEvents(user) {
  return hasRole(user, ROLES.ADMIN, ROLES.ORGANIZADOR)
}

/**
 * Alcance mock (se reacomoda con BE):
 * - ORGANIZADOR → solo sus eventos
 * - ADMIN → todos los eventos + personal + transacciones de ventas
 */
export function canManageAllEvents(user) {
  return hasRole(user, ROLES.ADMIN)
}

/** Admin: personal de acceso y ops de ventas/devoluciones. */
export function canManageStaff(user) {
  return hasRole(user, ROLES.ADMIN)
}

export function canManageSalesOps(user) {
  return hasRole(user, ROLES.ADMIN)
}

/** Ops globales: cancelar/suspender ajenos, mantenimiento, etc. */
export function canOperatePlatform(user) {
  return hasRole(user, ROLES.ADMIN)
}

/** Ingreso al evento: validar QR — solo personal de acceso (no admin de plataforma). */
export function canScanAccess(user) {
  return hasRole(user, ROLES.ACCESO)
}

/** Compra y “mis entradas” (cliente; org/admin también pueden). */
export function canBuyTickets(user) {
  return hasRole(user, ROLES.CLIENTE, ROLES.ORGANIZADOR, ROLES.ADMIN)
}

/** Home post-login según rol (mock UX). */
export function homePathForRole(rol) {
  const r = normalizeRol(rol)
  if (r === ROLES.ACCESO) return '/acceso'
  if (r === ROLES.ADMIN || r === ROLES.ORGANIZADOR) return '/gestion'
  return '/'
}
