/**
 * Store mock de entradas (JSON LS).
 * Simula ms-entradas: listado por idCliente.
 */
import { ENTRADA_ESTADOS, toEntradaListItem } from '../../domain/entrada'

const STORE_KEY = 'ticketya_mock_entradas'

function seedEntradas() {
  return [
    {
      id: 'aa000000-0000-4000-8000-000000000001',
      codigoQR: 'QR-SEED-METALLICA-001',
      idCompra: 'f0000000-0000-4000-8000-000000000021',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-000000000001',
      eventoNombre: 'Metallica — M72 World Tour',
      eventoFecha: '2027-03-15T21:00:00',
      eventoEstado: 'PUBLICADO',
      idSector: 'c0000000-0000-4000-8000-000000000011',
      sectorNombre: 'Campo',
      fila: null,
      asiento: null,
      precio: 95000,
      estado: ENTRADA_ESTADOS.VENDIDA,
      fechaEmision: '2026-10-01T14:06:00.000Z',
      fechaIngreso: null,
      lugarNombre: 'Estadio Más Monumental',
      lugarCiudad: 'Buenos Aires',
    },
    {
      id: 'aa000000-0000-4000-8000-000000000002',
      codigoQR: null,
      idCompra: 'f0000000-0000-4000-8000-000000000043',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      idEvento: 'd0000000-0000-4000-8000-000000000003',
      eventoNombre: 'Rock in Rio — día metal (cancelado)',
      eventoFecha: '2027-09-18T16:00:00',
      eventoEstado: 'CANCELADO',
      idSector: 'c0000000-0000-4000-8000-000000000011',
      sectorNombre: 'Campo',
      fila: null,
      asiento: null,
      precio: 75000,
      estado: ENTRADA_ESTADOS.A_REINTEGRAR,
      fechaEmision: '2026-09-15T10:06:00.000Z',
      fechaIngreso: null,
      lugarNombre: 'Estadio Más Monumental',
      lugarCiudad: 'Buenos Aires',
    },
  ].map(toEntradaListItem)
}

export function readEntradas() {
  try {
    const raw = localStorage.getItem(STORE_KEY)
    if (!raw) {
      const seed = seedEntradas()
      writeEntradas(seed)
      return seed
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return seedEntradas()
    let dirty = false
    const next = parsed.map((row) => {
      const e = { ...row }
      if (
        e.idEvento === 'd0000000-0000-4000-8000-000000000003' ||
        String(e.id || '').startsWith('ent_cancel_')
      ) {
        if (
          e.estado !== ENTRADA_ESTADOS.A_REINTEGRAR ||
          e.codigoQR ||
          e.eventoEstado !== 'CANCELADO'
        ) {
          dirty = true
          e.estado = ENTRADA_ESTADOS.A_REINTEGRAR
          e.codigoQR = null
          e.eventoEstado = 'CANCELADO'
        }
      }
      return toEntradaListItem(e)
    })
    if (dirty) writeEntradas(next)
    return next
  } catch {
    return seedEntradas()
  }
}

export function writeEntradas(list) {
  localStorage.setItem(STORE_KEY, JSON.stringify(list))
}

/** Entradas de un cliente (dueño = idCliente). QR ya sanitizado. */
export function listByClienteId(idCliente) {
  if (!idCliente) return []
  return readEntradas()
    .filter((e) => e.idCliente === String(idCliente))
    .map((e) => toEntradaListItem(e))
}

export function listByEventoIds(eventoIds) {
  const set = new Set((eventoIds || []).map(String))
  if (!set.size) return []
  return readEntradas().filter((e) => set.has(String(e.idEvento)))
}

export function patchEntradaEstado(id, estado, extra = {}) {
  const list = readEntradas()
  const idx = list.findIndex((e) => e.id === String(id))
  if (idx === -1) return null
  list[idx] = { ...list[idx], ...extra, estado }
  writeEntradas(list)
  return list[idx]
}

export function getById(id) {
  return readEntradas().find((e) => e.id === String(id)) || null
}

/** Solo el dueño (o admin en el futuro) puede ver el detalle. */
export function getByIdForCliente(id, idCliente) {
  const e = getById(id)
  if (!e) return null
  if (e.idCliente !== String(idCliente)) return null
  return toEntradaListItem(e)
}

/** Emite entrada post-pago mock (sin carrito: 1 click = 1 entrada). */
export function insertEntrada(entrada) {
  const list = readEntradas()
  const item = toEntradaListItem(entrada)
  list.unshift(item)
  writeEntradas(list)
  return item
}
