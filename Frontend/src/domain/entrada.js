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
 * Política alineada al BE: QR solo si VENDIDA y evento no CANCELADO.
 * @param {{ estado?: string, eventoEstado?: string|null, codigoQR?: string|null }} entrada
 */
export function isEntradaQrExponible(entrada) {
  if (!entrada) return false
  if (entrada.estado !== ENTRADA_ESTADOS.VENDIDA) return false
  const evEst = String(entrada.eventoEstado || '').trim().toUpperCase()
  if (evEst === 'CANCELADO') return false
  return true
}

/** Entrada usable como ticket (QR). */
export function isEntradaActiva(entrada) {
  return isEntradaQrExponible(entrada)
}

/** Reintegro impuesto por cancelación de evento (sigue en la lista, sin QR). */
export function isEntradaReintegroCancelacion(entrada) {
  if (!entrada) return false
  if (entrada.estado === ENTRADA_ESTADOS.A_REINTEGRAR) return true
  const ev = String(entrada.eventoEstado || '').trim().toUpperCase()
  return ev === 'CANCELADO' && entrada.estado === ENTRADA_ESTADOS.VENDIDA
}

/** Evento pausado (SUSPENDIDO): entrada sigue válida; aviso en UI. */
export function isEntradaEventoSuspendido(entrada) {
  if (!entrada) return false
  if (isEntradaReintegroCancelacion(entrada)) return false
  const ev = String(entrada.eventoEstado || '').trim().toUpperCase()
  return ev === 'SUSPENDIDO' && entrada.estado === ENTRADA_ESTADOS.VENDIDA
}

/** Cards en Mis entradas: activas + cancelación + suspendidas. */
export function isEntradaVisibleEnListado(entrada) {
  return (
    isEntradaActiva(entrada) ||
    isEntradaReintegroCancelacion(entrada) ||
    isEntradaEventoSuspendido(entrada)
  )
}

/**
 * @typedef {object} Entrada
 * @property {string} id
 * @property {string|null} [codigoQR]
 * @property {string} idCompra
 * @property {string} idCliente
 * @property {string} idEvento
 * @property {string} [eventoNombre]
 * @property {string} [eventoFecha]
 * @property {string} [eventoEstado]
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
  const base = {
    id: e.id,
    codigoQR: e.codigoQR ?? null,
    idCompra: e.idCompra,
    idCliente: e.idCliente,
    idEvento: e.idEvento,
    eventoNombre: e.eventoNombre || 'Evento',
    eventoFecha: e.eventoFecha || null,
    eventoEstado: e.eventoEstado || null,
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
  // Defensa en profundidad FE: nunca devolver secreto si la política no aplica
  if (!isEntradaQrExponible(base)) {
    base.codigoQR = null
  }
  return base
}
