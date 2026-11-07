/**
 * Store mock compras/transacciones (ms-ventas admin).
 */
import { COMPRA_ESTADOS } from '../../domain/compra'

const STORE_KEY = 'ticketya_mock_compras'

function seed() {
  return [
    {
      id: 'f0000000-0000-4000-8000-000000000021',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'd0000000-0000-4000-8000-000000000001',
      eventoNombre: 'Metallica — M72 World Tour',
      montoTotal: 95000,
      fechaCompra: '2026-10-01T14:05:00.000Z',
      medioPago: 'MOCK_CARD',
      idTransaccionPasarela: 'mp_seed_tx_001',
      estado: COMPRA_ESTADOS.APROBADA,
      cantidadEntradas: 1,
    },
    {
      id: 'f0000000-0000-4000-8000-000000000043',
      idCliente: 'b0000000-0000-4000-8000-000000000001',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'd0000000-0000-4000-8000-000000000003',
      eventoNombre: 'Rock in Rio — día metal (cancelado)',
      montoTotal: 75000,
      fechaCompra: '2026-09-15T10:05:00.000Z',
      medioPago: 'MOCK_CARD',
      idTransaccionPasarela: 'mp_seed_tx_cancel_001',
      estado: COMPRA_ESTADOS.APROBADA,
      cantidadEntradas: 1,
    },
  ]
}

export function readCompras() {
  try {
    const raw = localStorage.getItem(STORE_KEY)
    if (!raw) {
      const s = seed()
      writeCompras(s)
      return s
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : seed()
  } catch {
    return seed()
  }
}

export function writeCompras(list) {
  localStorage.setItem(STORE_KEY, JSON.stringify(list))
}

export function getCompra(id) {
  return readCompras().find((c) => c.id === String(id)) || null
}

export function insertCompra(compra) {
  const list = readCompras()
  list.unshift(compra)
  writeCompras(list)
  return compra
}

export function patchCompraEstado(id, estado) {
  const list = readCompras()
  const idx = list.findIndex((c) => c.id === String(id))
  if (idx === -1) return null
  const nextEstado = String(estado || '').toUpperCase()
  if (!Object.values(COMPRA_ESTADOS).includes(nextEstado)) {
    const err = new Error('Estado de compra inválido')
    err.status = 400
    throw err
  }
  list[idx] = { ...list[idx], estado: nextEstado }
  writeCompras(list)
  return list[idx]
}
