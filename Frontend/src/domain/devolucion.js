/**
 * Devolución / reclamo / reintegro por cancelación (ms-entradas).
 * Panel admin: solo PENDIENTE (voluntarias). Cancelación → EN_CURSO (no cola admin).
 */

export const DEVOLUCION_ESTADOS = {
  PENDIENTE: 'PENDIENTE',
  APROBADA: 'APROBADA',
  RECHAZADA: 'RECHAZADA',
  /** Reintegro por cancelación de evento (sistema); acreditación pendiente de notif. */
  EN_CURSO: 'EN_CURSO',
}

export const DEVOLUCION_ESTADO_LABELS = {
  [DEVOLUCION_ESTADOS.PENDIENTE]: 'Pendiente',
  [DEVOLUCION_ESTADOS.APROBADA]: 'Aprobada',
  [DEVOLUCION_ESTADOS.RECHAZADA]: 'Rechazada',
  [DEVOLUCION_ESTADOS.EN_CURSO]: 'Reintegro en curso',
}

export const DEVOLUCION_TIPOS = {
  DEVOLUCION: 'DEVOLUCION',
  RECLAMO: 'RECLAMO',
  /** Impuesto por cancelación de evento (no lo pidió el cliente). */
  CANCELACION_EVENTO: 'CANCELACION_EVENTO',
}

export const DEVOLUCION_TIPO_LABELS = {
  [DEVOLUCION_TIPOS.DEVOLUCION]: 'Devolución',
  [DEVOLUCION_TIPOS.RECLAMO]: 'Reclamo',
  [DEVOLUCION_TIPOS.CANCELACION_EVENTO]: 'Cancelación de evento',
}

/** Prefijo de motivo BE para reintegros automáticos por cancelación. */
export const MOTIVO_CANCELACION_PREFIX = 'Reintegro por cancelación del evento'

/**
 * @typedef {object} Devolucion
 * @property {string} id
 * @property {string} tipo  DEVOLUCION | RECLAMO | CANCELACION_EVENTO
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
 * @property {boolean} [sintetico] true si el FE lo armó sin fila BE
 */

export function isDevolucionPorCancelacion(d) {
  if (!d) return false
  if (d.tipo === DEVOLUCION_TIPOS.CANCELACION_EVENTO) return true
  const m = String(d.motivo || '')
  return m.startsWith(MOTIVO_CANCELACION_PREFIX) || /cancelaci[oó]n del evento/i.test(m)
}

/** Normaliza DTO BE (sin tipo) → shape FE. */
export function normalizeDevolucion(d) {
  if (!d) return d
  const motivo = d.motivo || ''
  let tipo = d.tipo || DEVOLUCION_TIPOS.DEVOLUCION
  if (!d.tipo && isDevolucionPorCancelacion({ ...d, tipo: null, motivo })) {
    tipo = DEVOLUCION_TIPOS.CANCELACION_EVENTO
  }
  return {
    id: d.id,
    tipo,
    idEntrada: d.idEntrada,
    idCompra: d.idCompra || null,
    idCliente: d.idCliente,
    clienteEmail: d.clienteEmail || null,
    idEvento: d.idEvento || null,
    eventoNombre: d.eventoNombre || null,
    fechaSolicitud: d.fechaSolicitud,
    motivo: motivo || null,
    estado: d.estado,
    idAdministrador: d.idAdministrador ?? null,
    monto: d.monto != null ? Number(d.monto) : null,
    sintetico: !!d.sintetico,
  }
}

/**
 * Trámite sintético para entrada A_REINTEGRAR sin fila de devolución.
 * Unifica “Ver devoluciones” cuando el BE aún no creó el registro.
 */
export function tramiteFromEntradaCancelada(entrada) {
  if (!entrada?.id) return null
  return normalizeDevolucion({
    id: `reint-cancel-${entrada.id}`,
    tipo: DEVOLUCION_TIPOS.CANCELACION_EVENTO,
    idEntrada: entrada.id,
    idCompra: entrada.idCompra,
    idCliente: entrada.idCliente,
    idEvento: entrada.idEvento,
    eventoNombre: entrada.eventoNombre,
    fechaSolicitud: entrada.fechaEmision || new Date().toISOString(),
    motivo: `${MOTIVO_CANCELACION_PREFIX}. Te avisamos cuando se acredite el dinero.`,
    estado: DEVOLUCION_ESTADOS.EN_CURSO,
    monto: entrada.precio,
    sintetico: true,
  })
}

/**
 * Une solicitudes reales + huecos A_REINTEGRAR sin trámite.
 * @param {Devolucion[]} solicitudes
 * @param {import('./entrada').Entrada[]} entradas
 */
export function mergeTramitesConCancelaciones(solicitudes, entradas) {
  const list = (Array.isArray(solicitudes) ? solicitudes : []).map(normalizeDevolucion)
  const covered = new Set(
    list.map((s) => String(s.idEntrada || '')).filter(Boolean)
  )
  const extra = []
  for (const e of entradas || []) {
    if (e.estado !== 'A_REINTEGRAR') continue
    if (covered.has(String(e.id))) continue
    const t = tramiteFromEntradaCancelada(e)
    if (t) {
      extra.push(t)
      covered.add(String(e.id))
    }
  }
  return [...list, ...extra]
}
