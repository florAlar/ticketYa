/**
 * Store mock devoluciones / reclamos (cola admin).
 */
import {
  DEVOLUCION_ESTADOS,
  DEVOLUCION_TIPOS,
} from '../../domain/devolucion'

const STORE_KEY = 'ticketya_mock_devoluciones'

function seed() {
  return [
    {
      id: 'aa000000-0000-4000-8000-000000000011',
      tipo: DEVOLUCION_TIPOS.DEVOLUCION,
      idEntrada: 'aa000000-0000-4000-8000-000000000001',
      idCompra: 'f0000000-0000-4000-8000-000000000021',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'd0000000-0000-4000-8000-000000000001',
      eventoNombre: 'Metallica — M72 World Tour',
      fechaSolicitud: '2026-10-10T09:00:00.000Z',
      motivo: 'Cambio de planes (seed demo).',
      estado: DEVOLUCION_ESTADOS.PENDIENTE,
      idAdministrador: null,
      monto: 95000,
    },
    {
      id: 'dev_cancel_001',
      tipo: DEVOLUCION_TIPOS.CANCELACION_EVENTO,
      idEntrada: 'aa000000-0000-4000-8000-000000000002',
      idCompra: 'f0000000-0000-4000-8000-000000000043',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'd0000000-0000-4000-8000-000000000003',
      eventoNombre: 'Rock in Rio — día metal (cancelado)',
      fechaSolicitud: '2026-10-12T10:00:00.000Z',
      motivo:
        'Reintegro por cancelación del evento. Te avisamos cuando se acredite el dinero.',
      estado: DEVOLUCION_ESTADOS.EN_CURSO,
      idAdministrador: null,
      monto: 75000,
    },
  ]
}

export function readDevoluciones() {
  try {
    const raw = localStorage.getItem(STORE_KEY)
    if (!raw) {
      const s = seed()
      writeDevoluciones(s)
      return s
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return seed()
    // Si falta el trámite demo de cancelación, lo agregamos una vez
    if (!parsed.some((d) => d.id === 'dev_cancel_001')) {
      const cancelSeed = seed().find((d) => d.id === 'dev_cancel_001')
      if (cancelSeed) {
        const next = [cancelSeed, ...parsed]
        writeDevoluciones(next)
        return next
      }
    }
    return parsed
  } catch {
    return seed()
  }
}

export function writeDevoluciones(list) {
  localStorage.setItem(STORE_KEY, JSON.stringify(list))
}

/** Solo pendientes: lo que el admin debe atender. */
export function listDevolucionesPendientes() {
  return readDevoluciones().filter(
    (d) => d.estado === DEVOLUCION_ESTADOS.PENDIENTE
  )
}

export function listDevolucionesByCliente(idCliente) {
  if (!idCliente) return []
  return readDevoluciones().filter((d) => d.idCliente === String(idCliente))
}

export function listDevolucionesByEntrada(idEntrada) {
  return readDevoluciones().filter((d) => d.idEntrada === String(idEntrada))
}

export function hasPendienteForEntrada(idEntrada) {
  return listDevolucionesByEntrada(idEntrada).some(
    (d) => d.estado === DEVOLUCION_ESTADOS.PENDIENTE
  )
}

/**
 * Cliente crea devolución o reclamo sobre su entrada.
 */
export function insertDevolucion(payload) {
  const list = readDevoluciones()
  const idEntrada = String(payload.idEntrada || '')
  if (!idEntrada) {
    const err = new Error('Falta la entrada')
    err.status = 400
    throw err
  }
  if (hasPendienteForEntrada(idEntrada)) {
    const err = new Error('Ya hay un reclamo o devolución pendiente para esta entrada')
    err.status = 409
    throw err
  }
  const tipo = String(payload.tipo || DEVOLUCION_TIPOS.DEVOLUCION).toUpperCase()
  if (!Object.values(DEVOLUCION_TIPOS).includes(tipo)) {
    const err = new Error('Tipo inválido')
    err.status = 400
    throw err
  }
  const motivo = String(payload.motivo || '').trim()
  if (motivo.length < 5) {
    const err = new Error('El motivo debe tener al menos 5 caracteres')
    err.status = 400
    throw err
  }
  const item = {
    id: `${tipo === DEVOLUCION_TIPOS.RECLAMO ? 'rec' : tipo === DEVOLUCION_TIPOS.CANCELACION_EVENTO ? 'reint' : 'dev'}_${Date.now()}`,
    tipo,
    idEntrada,
    idCompra: payload.idCompra || null,
    idCliente: String(payload.idCliente),
    clienteEmail: payload.clienteEmail || null,
    idEvento: payload.idEvento || null,
    eventoNombre: payload.eventoNombre || null,
    fechaSolicitud: new Date().toISOString(),
    motivo,
    estado:
      tipo === DEVOLUCION_TIPOS.CANCELACION_EVENTO
        ? DEVOLUCION_ESTADOS.EN_CURSO
        : DEVOLUCION_ESTADOS.PENDIENTE,
    idAdministrador: null,
    monto: payload.monto != null ? Number(payload.monto) : null,
  }
  list.unshift(item)
  writeDevoluciones(list)
  return item
}

export function patchDevolucionEstado(id, estado, idAdministrador) {
  const list = readDevoluciones()
  const idx = list.findIndex((d) => d.id === String(id))
  if (idx === -1) return null
  const next = String(estado || '').toUpperCase()
  if (!Object.values(DEVOLUCION_ESTADOS).includes(next)) {
    const err = new Error('Estado de devolución inválido')
    err.status = 400
    throw err
  }
  list[idx] = {
    ...list[idx],
    estado: next,
    idAdministrador:
      next === DEVOLUCION_ESTADOS.EN_CURSO
        ? list[idx].idAdministrador
        : idAdministrador || list[idx].idAdministrador,
  }
  writeDevoluciones(list)
  return list[idx]
}
