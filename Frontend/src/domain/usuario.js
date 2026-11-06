/**
 * Usuario (referencia ms-usuarios).
 * Admin mock: solo cambia rol + activo (no nombre/email).
 * Personal ACCESO: no se “desactiva”; se elimina. Asignado a eventos.
 */

import { ROLES, normalizeRol } from '../auth/roles'

export { ROLES }

/**
 * @typedef {object} UsuarioAdmin
 * @property {string} id
 * @property {string} nombre
 * @property {string} email
 * @property {string} rol
 * @property {boolean} activo
 * @property {string[]} [eventosAsignados]  // solo ACCESO: idEvento[]
 * @property {string} [fechaRegistro]
 */

/** Vista admin sin password. */
export function toUsuarioAdminDto(u) {
  const rol = normalizeRol(u.rol)
  return {
    id: u.id,
    nombre: u.nombre,
    email: u.email,
    rol,
    activo: u.activo !== false,
    eventosAsignados:
      rol === ROLES.ACCESO
        ? Array.isArray(u.eventosAsignados)
          ? u.eventosAsignados.map(String)
          : []
        : undefined,
    fechaRegistro: u.fechaRegistro || null,
  }
}
