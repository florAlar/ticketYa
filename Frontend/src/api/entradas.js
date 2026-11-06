import { http } from './http'
import { USE_MOCK_ENTRADAS } from './config'
import {
  getByIdForCliente,
  listByClienteId,
  patchEntradaEstado,
  readEntradas,
} from './mock/entradasStore'
import { ENTRADA_ESTADOS, toEntradaListItem } from '../domain/entrada'

const delay = (ms = 280) => new Promise((r) => setTimeout(r, ms))

/** Normaliza DTO BE → shape FE (fechas ISO string). */
function normalizeEntrada(e) {
  if (!e) return e
  return toEntradaListItem({
    id: e.id,
    codigoQR: e.codigoQR,
    idCompra: e.idCompra,
    idCliente: e.idCliente,
    idEvento: e.idEvento,
    eventoNombre: e.eventoNombre,
    eventoFecha: e.eventoFecha || null,
    idSector: e.idSector,
    sectorNombre: e.sectorNombre,
    fila: e.fila ?? null,
    asiento: e.asiento ?? null,
    precio: e.precio ?? 0,
    estado: e.estado,
    fechaEmision: e.fechaEmision,
    fechaIngreso: e.fechaIngreso ?? null,
    lugarNombre: e.lugarNombre || '',
    lugarCiudad: e.lugarCiudad || '',
  })
}

/**
 * Entradas propias del JWT.
 * GET /api/entradas
 */
export async function listMisEntradas(user, opts = {}) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }

  if (USE_MOCK_ENTRADAS) {
    await delay()
    return listByClienteId(user.id)
  }

  const data = await http('/api/entradas', {
    method: 'GET',
    token: opts.token,
  })
  return Array.isArray(data) ? data.map(normalizeEntrada) : []
}

/**
 * GET /api/entradas/{id}
 */
export async function getEntrada(entradaId, user, opts = {}) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }

  if (USE_MOCK_ENTRADAS) {
    await delay()
    const found = getByIdForCliente(entradaId, user.id)
    if (!found) {
      const err = new Error('Entrada no encontrada')
      err.status = 404
      throw err
    }
    return found
  }

  const data = await http(`/api/entradas/${entradaId}`, {
    method: 'GET',
    token: opts.token,
  })
  return normalizeEntrada(data)
}

/**
 * GET /api/entradas/{id}/qr
 */
export async function getEntradaQr(entradaId, user, opts = {}) {
  if (USE_MOCK_ENTRADAS) {
    await delay(100)
    const found = getByIdForCliente(entradaId, user?.id)
    if (!found) {
      const err = new Error('Entrada no encontrada')
      err.status = 404
      throw err
    }
    return { id: found.id, codigoQR: found.codigoQR }
  }
  return http(`/api/entradas/${entradaId}/qr`, {
    method: 'GET',
    token: opts.token,
  })
}

/**
 * Validar ingreso en puerta (E2-ACC; mock local si no hay BE).
 * POST /api/eventos/{idEvento}/validaciones
 */
export async function validarIngreso(idEvento, payload, user, opts = {}) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }

  const codigo =
    payload?.codigoQR ||
    payload?.codigo ||
    payload?.idEntrada ||
    String(payload || '').trim()

  if (USE_MOCK_ENTRADAS) {
    await delay(300)
    if (!codigo) {
      const err = new Error('Código de entrada requerido')
      err.status = 400
      throw err
    }
    const all = readEntradas()
    const found =
      all.find(
        (e) =>
          e.codigoQR === codigo ||
          e.id === codigo ||
          String(e.id).toUpperCase() === String(codigo).toUpperCase()
      ) || null

    if (!found) {
      const err = new Error('Entrada no encontrada')
      err.status = 404
      throw err
    }
    if (idEvento && found.idEvento && String(found.idEvento) !== String(idEvento)) {
      const err = new Error('La entrada no corresponde a este evento')
      err.status = 400
      throw err
    }
    if (found.estado === ENTRADA_ESTADOS.USADA) {
      const err = new Error('Entrada ya utilizada')
      err.status = 409
      throw err
    }
    if (found.estado !== ENTRADA_ESTADOS.VENDIDA) {
      const err = new Error(`Entrada no válida para ingreso (${found.estado})`)
      err.status = 400
      throw err
    }

    const asignados = user.eventosAsignados
    if (
      Array.isArray(asignados) &&
      asignados.length > 0 &&
      !asignados.map(String).includes(String(found.idEvento))
    ) {
      const err = new Error('No tenés permiso para validar este evento')
      err.status = 403
      throw err
    }

    const updated = patchEntradaEstado(found.id, ENTRADA_ESTADOS.USADA, {
      fechaIngreso: new Date().toISOString(),
    })
    return {
      ok: true,
      entrada: updated || found,
      mensaje: 'Ingreso registrado (mock)',
    }
  }

  return http(`/api/eventos/${idEvento}/validaciones`, {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify(
      payload?.codigoQR || payload?.idEntrada ? payload : { codigoQR: codigo }
    ),
  })
}

/**
 * GET /api/eventos/{idEvento}/validaciones — org/admin
 */
export async function listValidacionesEvento(idEvento, opts = {}) {
  if (USE_MOCK_ENTRADAS) {
    await delay()
    return readEntradas().filter(
      (e) =>
        String(e.idEvento) === String(idEvento) &&
        e.estado === ENTRADA_ESTADOS.USADA
    )
  }
  return http(`/api/eventos/${idEvento}/validaciones`, {
    method: 'GET',
    token: opts.token,
  })
}
