/**
 * Compra / transacción (referencia ms-ventas).
 * Admin: administración de transacciones (listado + estados).
 */

export const COMPRA_ESTADOS = {
  APROBADA: 'APROBADA',
  REINTEGRADA: 'REINTEGRADA',
  CANCELADA: 'CANCELADA',
  PENDIENTE: 'PENDIENTE',
}

export const COMPRA_ESTADO_LABELS = {
  [COMPRA_ESTADOS.APROBADA]: 'Aprobada',
  [COMPRA_ESTADOS.REINTEGRADA]: 'Reintegrada',
  [COMPRA_ESTADOS.CANCELADA]: 'Cancelada',
  [COMPRA_ESTADOS.PENDIENTE]: 'Pendiente',
}

/**
 * @typedef {object} Compra
 * @property {string} id
 * @property {string} idCliente
 * @property {string} [clienteEmail]
 * @property {string} idEvento
 * @property {string} [eventoNombre]
 * @property {number} montoTotal
 * @property {string} fechaCompra
 * @property {string} [medioPago]
 * @property {string} [idTransaccionPasarela]
 * @property {string} estado
 * @property {number} [cantidadEntradas]
 */
