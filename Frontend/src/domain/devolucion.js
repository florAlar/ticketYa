/**
 * Devolución / reclamo (ms-ventas o ms-entradas — dueño BE a definir).
 * Panel admin solo muestra pendientes de resolución (no todo el historial de compras).
 */

export const DEVOLUCION_ESTADOS = {
  PENDIENTE: 'PENDIENTE',
  APROBADA: 'APROBADA',
  RECHAZADA: 'RECHAZADA',
}

export const DEVOLUCION_ESTADO_LABELS = {
  [DEVOLUCION_ESTADOS.PENDIENTE]: 'Pendiente',
  [DEVOLUCION_ESTADOS.APROBADA]: 'Aprobada',
  [DEVOLUCION_ESTADOS.RECHAZADA]: 'Rechazada',
}

export const DEVOLUCION_TIPOS = {
  DEVOLUCION: 'DEVOLUCION',
  RECLAMO: 'RECLAMO',
}

export const DEVOLUCION_TIPO_LABELS = {
  [DEVOLUCION_TIPOS.DEVOLUCION]: 'Devolución',
  [DEVOLUCION_TIPOS.RECLAMO]: 'Reclamo',
}

/**
 * @typedef {object} Devolucion
 * @property {string} id
 * @property {string} tipo  DEVOLUCION | RECLAMO
 * @property {string} idEntrada
 * @property {string} [idCompra]
 * @property {string} idCliente
 * @property {string} [clienteEmail]
 * @property {string} idEvento
 * @property {string} [eventoNombre]
 * @property {string} fechaSolicitud
 * @property {string} [motivo]
 * @property {string} estado
 * @property {string|null} [idAdministrador]
 * @property {number} [monto]
 */
