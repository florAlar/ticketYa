/**
 * Store mock de entradas (JSON LS).
 * Simula ms-entradas: listado por idCliente.
 */
import { ENTRADA_ESTADOS, toEntradaListItem } from '../../domain/entrada'

const STORE_KEY = 'ticketya_mock_entradas'

function seedEntradas() {
  // Seeds demo cliente + entradas de evento cancelado (cola admin, no “todas las compras”)
  return [
    {
      id: 'ent_demo_001',
      codigoQR: 'QR-DEMO-001-NOCHE-INDIE',
      idCompra: 'cmp_demo_1',
      idCliente: 'u_demo',
      idEvento: 'e1',
      eventoNombre: 'Noche de Indie en el Lago',
      eventoFecha: '2026-11-15T21:00:00',
      eventoEstado: 'PUBLICADO',
      idSector: 'sec_gen',
      sectorNombre: 'General',
      fila: null,
      asiento: null,
      precio: 18000,
      estado: ENTRADA_ESTADOS.VENDIDA,
      fechaEmision: '2026-10-01T14:30:00.000Z',
      fechaIngreso: null,
      lugarNombre: 'Anfiteatro del Lago',
      lugarCiudad: 'Tandil',
    },
    {
      id: 'ent_demo_002',
      codigoQR: 'QR-DEMO-002-HAMLET-A12',
      idCompra: 'cmp_demo_2',
      idCliente: 'u_demo',
      idEvento: 'e2',
      eventoNombre: 'Hamlet — función especial',
      eventoFecha: '2026-11-20T20:30:00',
      eventoEstado: 'PUBLICADO',
      idSector: 'sec_platea',
      sectorNombre: 'Platea',
      fila: 'A',
      asiento: '12',
      precio: 12000,
      estado: ENTRADA_ESTADOS.VENDIDA,
      fechaEmision: '2026-10-05T10:00:00.000Z',
      fechaIngreso: null,
      lugarNombre: 'Teatro del Fuerte',
      lugarCiudad: 'Tandil',
    },
    {
      id: 'ent_demo_003',
      codigoQR: null,
      idCompra: 'cmp_demo_3',
      idCliente: 'u_demo',
      idEvento: 'e8',
      eventoNombre: 'Esperando la carroza',
      eventoFecha: '2026-09-01T21:00:00',
      eventoEstado: 'FINALIZADO',
      idSector: 'sec_gen',
      sectorNombre: 'General',
      fila: null,
      asiento: null,
      precio: 9000,
      estado: ENTRADA_ESTADOS.USADA,
      fechaEmision: '2026-08-20T12:00:00.000Z',
      fechaIngreso: '2026-09-01T20:55:00.000Z',
      lugarNombre: 'Teatro del Fuerte',
      lugarCiudad: 'Tandil',
    },
    {
      id: 'ent_cancel_001',
      codigoQR: null,
      idCompra: 'cmp_cancel_1',
      idCliente: 'u_demo',
      idEvento: 'e_cancel_demo',
      eventoNombre: 'Festival cancelado (demo admin)',
      eventoFecha: '2026-12-01T20:00:00',
      eventoEstado: 'CANCELADO',
      idSector: 'sec_campo',
      sectorNombre: 'Campo',
      fila: null,
      asiento: null,
      precio: 15000,
      estado: ENTRADA_ESTADOS.A_REINTEGRAR,
      fechaEmision: '2026-10-10T12:00:00.000Z',
      fechaIngreso: null,
      lugarNombre: 'Predio Municipal',
      lugarCiudad: 'Tandil',
    },
    {
      id: 'ent_cancel_002',
      codigoQR: null,
      idCompra: 'cmp_cancel_2',
      idCliente: 'u_org',
      idEvento: 'e_cancel_demo',
      eventoNombre: 'Festival cancelado (demo admin)',
      eventoFecha: '2026-12-01T20:00:00',
      eventoEstado: 'CANCELADO',
      idSector: 'sec_campo',
      sectorNombre: 'Campo',
      fila: null,
      asiento: null,
      precio: 15000,
      estado: ENTRADA_ESTADOS.A_REINTEGRAR,
      fechaEmision: '2026-10-11T12:00:00.000Z',
      fechaIngreso: null,
      lugarNombre: 'Predio Municipal',
      lugarCiudad: 'Tandil',
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
    // Migración barata: demo cancelado viejo (VENDIDA+QR) → A_REINTEGRAR sin QR
    let dirty = false
    const next = parsed.map((row) => {
      const e = { ...row }
      if (
        e.idEvento === 'e_cancel_demo' ||
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
