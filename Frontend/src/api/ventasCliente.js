import { http } from './http'
import { USE_MOCK } from './config'
import { getEvento } from './eventos'
import { listMisEntradas } from './entradas'
import { insertCompra } from './mock/comprasStore'
import { insertEntrada } from './mock/entradasStore'
import { COMPRA_ESTADOS } from '../domain/compra'
import { MAX_ASIENTOS_POR_COMPRA } from '../domain/asiento'
import { ENTRADA_ESTADOS, toEntradaListItem } from '../domain/entrada'
import { EVENTO_ESTADOS } from '../domain/evento'

const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms))

function newIdempotencyKey() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

/**
 * Compra (1 sector, sin carrito).
 *
 * Mock: un paso (pago OK + emite entrada local).
 * Real (gateway): POST /reservas → POST /compras (+ Idempotency-Key) → POST /compras/{id}/pagos.
 *
 * body: { idEvento, idSector, asientos? } o { idReserva }
 *   - sector general: sin `asientos` → 1 entrada (cantidad 1).
 *   - sector numerado: `asientos: [{fila, numero}]` (1..MAX_ASIENTOS_POR_COMPRA) → 1 entrada por asiento.
 * opts: { token, idempotencyKey, resultadoPago: 'APROBAR'|'RECHAZAR' }
 */
export async function comprarEntrada(
  { idEvento, idSector, idReserva, asientos },
  user,
  opts = {}
) {
  if (!user?.id) {
    const err = new Error('Tenés que iniciar sesión para comprar')
    err.status = 401
    throw err
  }
  const seats = Array.isArray(asientos) ? asientos : []
  if (seats.length > MAX_ASIENTOS_POR_COMPRA) {
    const err = new Error(
      `Máximo ${MAX_ASIENTOS_POR_COMPRA} asientos por compra`
    )
    err.status = 400
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
    const preferenciaId = `mp_pref_mock_${Date.now()}`
    // Mock mínimo: sin asientos → 1 entrada; con asientos → 1 entrada por asiento (sin control de ocupación).
    const cantidad = seats.length || 1

    const compra = insertCompra({
      id: compraId,
      idCliente: user.id,
      clienteEmail: user.email,
      idEvento: evento.id,
      eventoNombre: evento.nombre,
      montoTotal: (Number(sector.precio) || 0) * cantidad,
      fechaCompra: now,
      medioPago: 'MERCADO_PAGO_MOCK',
      idTransaccionPasarela: preferenciaId,
      estado: COMPRA_ESTADOS.APROBADA,
      cantidadEntradas: cantidad,
      idSector: sector.id,
      sectorNombre: sector.nombre,
    })

    const entradas = []
    for (let i = 0; i < cantidad; i++) {
      const entradaId = `ent_${Date.now()}_${i}`
      entradas.push(
        insertEntrada(
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
            fila: seats[i]?.fila ?? null,
            asiento: seats[i]?.numero ?? null,
            precio: Number(sector.precio) || 0,
            estado: ENTRADA_ESTADOS.VENDIDA,
            fechaEmision: now,
            fechaIngreso: null,
            lugarNombre: evento.lugar?.nombre || '',
            lugarCiudad: evento.lugar?.ciudad || '',
          })
        )
      )
    }

    return {
      compra,
      entrada: entradas[0],
      entradas,
      mercadoPago: {
        preferenciaId,
        initPoint: `https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`,
        sandboxInitPoint: `https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=${preferenciaId}`,
        mock: true,
        mensaje: 'Pago simulado OK — no hay cobro real.',
      },
      mode: 'mock',
    }
  }

  // --- gateway real ---
  if (!opts.token) {
    const err = new Error('Falta token JWT (re-logueate contra el gateway)')
    err.status = 401
    throw err
  }

  let reservaId = idReserva
  let reserva = null

  if (!reservaId) {
    if (!idEvento || !idSector) {
      const err = new Error('idEvento e idSector son requeridos')
      err.status = 400
      throw err
    }
    reserva = await crearReserva(
      {
        idEvento,
        // numerado: asientos concretos (cantidad = asientos.length en el BE); general: cantidad 1
        items: [
          seats.length
            ? {
                idSector: String(idSector),
                asientos: seats.map((a) => ({
                  fila: String(a.fila),
                  numero: Number(a.numero),
                })),
              }
            : { idSector: String(idSector), cantidad: 1 },
        ],
      },
      {
        token: opts.token,
        idempotencyKey: opts.reservaIdempotencyKey || newIdempotencyKey(),
      }
    )
    reservaId = reserva.id
    // confirmación de intención (opcional pero alineada al contrato)
    try {
      reserva = await http(`/api/reservas/${reservaId}/confirmacion`, {
        method: 'POST',
        token: opts.token,
      })
    } catch {
      /* si ya CONFIRMADA u otro, seguimos con la creada */
    }
  }

  const compraKey = opts.idempotencyKey || newIdempotencyKey()
  const compra = await http('/api/compras', {
    method: 'POST',
    token: opts.token,
    headers: { 'Idempotency-Key': compraKey },
    body: JSON.stringify({ idReserva: reservaId }),
  })

  const resultadoPago = (opts.resultadoPago || 'APROBAR').toUpperCase()
  const pago = await http(`/api/compras/${compra.id}/pagos`, {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify({
      resultado: resultadoPago,
      proveedor: 'MOCK_PASARELA',
    }),
  })

  const compraFinal =
    (await getCompra(compra.id, { token: opts.token }).catch(() => compra)) || compra

  // Emisión la hace msvc-ventas → entradas; esperamos un instante y listamos
  let entradas = []
  try {
    for (let i = 0; i < 5; i++) {
      const all = await listMisEntradas(user, { token: opts.token })
      entradas = (all || []).filter((e) => String(e.idCompra) === String(compra.id))
      if (entradas.length) break
      await new Promise((r) => setTimeout(r, 300))
    }
  } catch {
    entradas = []
  }

  return {
    reserva: reserva || { id: reservaId },
    compra: compraFinal,
    pago,
    entrada: entradas[0] || null,
    entradas,
    mercadoPago: {
      preferenciaId: pago?.idTransaccionExterna || null,
      initPoint: null,
      mock: false,
      mensaje:
        pago?.estado === 'APROBADO'
          ? entradas.length
            ? 'Pago aprobado y entrada(s) emitidas.'
            : 'Pago aprobado. Si no ves QR, refrescá Mis entradas (emisión async).'
          : `Pago ${pago?.estado || 'desconocido'}.`,
    },
    mode: 'gateway',
  }
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
