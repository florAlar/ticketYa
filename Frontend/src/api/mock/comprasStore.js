/**
 * Store mock compras/transacciones (ms-ventas admin).
 */
import { COMPRA_ESTADOS } from '../../domain/compra'

const STORE_KEY = 'ticketya_mock_compras'

function seed() {
  return [
    {
      id: 'cmp_demo_1',
      idCliente: 'u_demo',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'e1',
      eventoNombre: 'Noche de Indie en el Lago',
      montoTotal: 18000,
      fechaCompra: '2026-10-01T14:30:00.000Z',
      medioPago: 'MOCK_CARD',
      idTransaccionPasarela: 'pay_mock_1',
      estado: COMPRA_ESTADOS.APROBADA,
      cantidadEntradas: 1,
    },
    {
      id: 'cmp_demo_2',
      idCliente: 'u_demo',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'e2',
      eventoNombre: 'Hamlet — función especial',
      montoTotal: 12000,
      fechaCompra: '2026-10-05T10:00:00.000Z',
      medioPago: 'MOCK_CARD',
      idTransaccionPasarela: 'pay_mock_2',
      estado: COMPRA_ESTADOS.APROBADA,
      cantidadEntradas: 1,
    },
    {
      id: 'cmp_demo_3',
      idCliente: 'u_demo',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'e8',
      eventoNombre: 'Esperando la carroza',
      montoTotal: 9000,
      fechaCompra: '2026-08-20T12:00:00.000Z',
      medioPago: 'MOCK_CARD',
      idTransaccionPasarela: 'pay_mock_3',
      estado: COMPRA_ESTADOS.REINTEGRADA,
      cantidadEntradas: 1,
    },
    {
      id: 'cmp_pend_1',
      idCliente: 'u_demo',
      clienteEmail: 'demo@ticketya.com',
      idEvento: 'e3',
      eventoNombre: 'Festival de Rock del Sur',
      montoTotal: 25000,
      fechaCompra: '2026-10-20T18:00:00.000Z',
      medioPago: 'MOCK_CARD',
      idTransaccionPasarela: 'pay_mock_4',
      estado: COMPRA_ESTADOS.PENDIENTE,
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
