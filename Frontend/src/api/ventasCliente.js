import { http } from './http'
import { USE_MOCK } from './config'
import { getEvento } from './eventos'
import { insertCompra } from './mock/comprasStore'
import { insertEntrada } from './mock/entradasStore'
import { COMPRA_ESTADOS } from '../domain/compra'
import { ENTRADA_ESTADOS, toEntradaListItem } from '../domain/entrada'
import { EVENTO_ESTADOS } from '../domain/evento'

const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms))

/**
 * Compra (simulacro: 1 sector, sin carrito).
 *
 * Contrato back completo: reserva → POST /api/compras (+ Idempotency-Key) → pagos.
 * Gateway real: POST /api/compras
 * Mock: un solo paso (pago OK + emite entrada).
 *
 * body mínimo mock/transitorio: { idEvento, idSector }
 * body back ideal: { idReserva } u homólogo que defina el BE.
 */
export async function comprarEntrada({ idEvento, idSector, idReserva }, user, opts = {}) {
  if (!user?.id) {
    const err = new Error('Tenés que iniciar sesión para comprar')
    err.status = 401
    throw err
  }

  if (USE_MOCK) {
    await delay()
    const evento = await getEvento(idEvento)
    if (!evento) {
      const err = new Error('Evento no encontrado')
      err.status = 404
      throw err
    }
    if (evento.estado !== EVENTO_ESTADOS.PUBLICADO && evento.estado !== 'PUBLICADO') {
      const err = new Error('El evento no está a la venta')
      err.status = 400
      throw err
    }

    const sectores = evento.sectores || evento.lugar?.sectores || []
    const sector =
      sectores.find((s) => s.id === String(idSector)) ||
      sectores.find((s) => s.nombre === String(idSector)) ||
      sectores[0]

    if (!sector) {
      const err = new Error('Tipo de entrada no disponible')
      err.status = 400
      throw err
    }

    const now = new Date().toISOString()
    const compraId = `cmp_${Date.now()}`
    const entradaId = `ent_${Date.now()}`
    const preferenciaId = `mp_pref_mock_${Date.now()}`

    const compra = insertCompra({
      id: compraId,
      idCliente: user.id,
      clienteEmail: user.email,
      idEvento: evento.id,
      eventoNombre: evento.nombre,
      montoTotal: Number(sector.precio) || 0,
      fechaCompra: now,
      medioPago: 'MERCADO_PAGO_MOCK',
      idTransaccionPasarela: preferenciaId,
      estado: COMPRA_ESTADOS.APROBADA,
      cantidadEntradas: 1,
      idSector: sector.id,
      sectorNombre: sector.nombre,
    })

    const entrada = insertEntrada(
      toEntradaListItem({
        id: entradaId,
        codigoQR: `QR-${entradaId.toUpperCase()}`,
        idCompra: compraId,
        idCliente: user.id,
        idEvento: evento.id,
        eventoNombre: evento.nombre,
        eventoFecha: evento.fechaInicio,
        idSector: sector.id,
        sectorNombre: sector.nombre,
        fila: null,
        asiento: null,
        precio: Number(sector.precio) || 0,
        estado: ENTRADA_ESTADOS.VENDIDA,
        fechaEmision: now,
        fechaIngreso: null,
        lugarNombre: evento.lugar?.nombre || '',
        lugarCiudad: evento.lugar?.ciudad || '',
      })
    )

    return {
      compra,
      entrada,
      mercadoPago: {
        preferenciaId,
        initPoint: `https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`,
        sandboxInitPoint: `https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`,
        mock: true,
        mensaje: 'Pago simulado OK — no hay cobro real.',
      },
    }
  }

  const body = idReserva
    ? { idReserva }
    : { idEvento, idSector }

  const headers = {}
  if (opts.idempotencyKey) {
    headers['Idempotency-Key'] = opts.idempotencyKey
  }

  return http('/api/compras', {
    method: 'POST',
    token: opts.token,
    headers,
    body: JSON.stringify(body),
  })
}

/** POST /api/reservas — TTL ~10 min */
export async function crearReserva(body, opts = {}) {
  if (USE_MOCK) {
    const err = new Error('Reservas: usar compra mock directa en simulacro')
    err.status = 501
    throw err
  }
  return http('/api/reservas', {
    method: 'POST',
    token: opts.token,
    headers: opts.idempotencyKey
      ? { 'Idempotency-Key': opts.idempotencyKey }
      : undefined,
    body: JSON.stringify(body),
  })
}

/** GET /api/compras — listar compras propias */
export async function listMisCompras(opts = {}) {
  if (USE_MOCK) {
    return []
  }
  return http('/api/compras', { token: opts.token })
}

/** GET /api/compras/{id} */
export async function getCompra(id, opts = {}) {
  if (USE_MOCK) {
    const err = new Error('getCompra mock no implementado')
    err.status = 501
    throw err
  }
  return http(`/api/compras/${id}`, { token: opts.token })
}
