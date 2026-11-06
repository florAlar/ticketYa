/**
 * Forma de dominio — referencia entidad ms-entradas (MySQL).
 * Se reacomoda cuando el BE exponga DTOs reales.
 */

export const ENTRADA_ESTADOS = {
  VENDIDA: 'VENDIDA',
  USADA: 'USADA',
  DEVUELTA: 'DEVUELTA',
  A_REINTEGRAR: 'A_REINTEGRAR',
  TRANSFERIDA: 'TRANSFERIDA',
}

export const ENTRADA_ESTADO_LABELS = {
  [ENTRADA_ESTADOS.VENDIDA]: 'Válida',
  [ENTRADA_ESTADOS.USADA]: 'Usada',
  [ENTRADA_ESTADOS.DEVUELTA]: 'Devuelta',
  [ENTRADA_ESTADOS.A_REINTEGRAR]: 'A reintegrar',
  [ENTRADA_ESTADOS.TRANSFERIDA]: 'Transferida',
}

/**
 * @typedef {object} Entrada
 * @property {string} id
 * @property {string} codigoQR
 * @property {string} idCompra
 * @property {string} idCliente
 * @property {string} idEvento
 * @property {string} [eventoNombre]
 * @property {string} [eventoFecha]
 * @property {string} [idSector]
 * @property {string} [sectorNombre]
 * @property {string|null} [fila]
 * @property {string|null} [asiento]
 * @property {number} precio
 * @property {string} estado
 * @property {string} fechaEmision
 * @property {string|null} [fechaIngreso]
 * @property {string} [lugarNombre]
 * @property {string} [lugarCiudad]
 */

export function toEntradaListItem(e) {
  return {
    id: e.id,
    codigoQR: e.codigoQR,
    idCompra: e.idCompra,
    idCliente: e.idCliente,
    idEvento: e.idEvento,
    eventoNombre: e.eventoNombre || 'Evento',
    eventoFecha: e.eventoFecha || null,
    idSector: e.idSector,
    sectorNombre: e.sectorNombre || 'General',
    fila: e.fila ?? null,
    asiento: e.asiento ?? null,
    precio: e.precio ?? 0,
    estado: e.estado,
    fechaEmision: e.fechaEmision,
    fechaIngreso: e.fechaIngreso ?? null,
    lugarNombre: e.lugarNombre || '',
    lugarCiudad: e.lugarCiudad || '',
  }
}
