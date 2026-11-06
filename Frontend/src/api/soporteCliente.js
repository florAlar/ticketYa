import { http } from './http'
import { USE_MOCK } from './config'
import { getByIdForCliente } from './mock/entradasStore'
import {
  insertDevolucion,
  listDevolucionesByCliente,
  listDevolucionesByEntrada,
} from './mock/devolucionesStore'
import { ENTRADA_ESTADOS } from '../domain/entrada'
import { DEVOLUCION_TIPOS } from '../domain/devolucion'

const delay = (ms = 280) => new Promise((r) => setTimeout(r, ms))

/**
 * Solicitudes del cliente (devoluciones).
 * Contrato back: no hay /mias explícito — GET /api/devoluciones con JWT de cliente
 * (el BE filtra por dueño) o listado admin de pendientes.
 */
export async function listMisSolicitudes(user, opts = {}) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }
  if (USE_MOCK) {
    await delay()
    return listDevolucionesByCliente(user.id)
  }
  return http('/api/devoluciones', { token: opts.token })
}

/**
 * POST /api/entradas/{id}/devoluciones
 * body: { tipo?, motivo }
 */
export async function crearSolicitudEntrada(
  { idEntrada, tipo, motivo },
  user,
  opts = {}
) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }

  if (USE_MOCK) {
    await delay(350)
    const entrada = getByIdForCliente(idEntrada, user.id)
    if (!entrada) {
      const err = new Error('Entrada no encontrada o no es tuya')
      err.status = 404
      throw err
    }
    if (entrada.estado !== ENTRADA_ESTADOS.VENDIDA) {
      const err = new Error(
        'Solo se puede reclamar o devolver una entrada válida (no usada ni ya devuelta)'
      )
      err.status = 400
      throw err
    }

    const t = String(tipo || DEVOLUCION_TIPOS.DEVOLUCION).toUpperCase()
    if (t === DEVOLUCION_TIPOS.DEVOLUCION && entrada.eventoFecha) {
      const eventoMs = new Date(entrada.eventoFecha).getTime()
      const limit = eventoMs - 72 * 60 * 60 * 1000
      if (Number.isFinite(eventoMs) && Date.now() > limit) {
        const err = new Error(
          'La devolución solo se puede pedir hasta 72 h antes del evento'
        )
        err.status = 400
        throw err
      }
    }

    return insertDevolucion({
      tipo: t,
      idEntrada: entrada.id,
      idCompra: entrada.idCompra,
      idCliente: user.id,
      clienteEmail: user.email,
      idEvento: entrada.idEvento,
      eventoNombre: entrada.eventoNombre,
      motivo,
      monto: entrada.precio,
    })
  }

  return http(`/api/entradas/${idEntrada}/devoluciones`, {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify({
      tipo: tipo || DEVOLUCION_TIPOS.DEVOLUCION,
      motivo,
    }),
  })
}

/**
 * Solicitudes de una entrada (UI detalle).
 * Mock: store local. Real: GET /api/devoluciones y filtro client-side,
 * o GET /api/devoluciones/{id} si ya se conoce.
 */
export async function listSolicitudesEntrada(entradaId, user, opts = {}) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }
  if (USE_MOCK) {
    await delay(120)
    const entrada = getByIdForCliente(entradaId, user.id)
    if (!entrada) {
      const err = new Error('Entrada no encontrada')
      err.status = 404
      throw err
    }
    return listDevolucionesByEntrada(entradaId)
  }

  const all = await http('/api/devoluciones', { token: opts.token })
  const list = Array.isArray(all) ? all : all?.content || all?.items || []
  return list.filter((d) => String(d.idEntrada) === String(entradaId))
}

/** GET /api/devoluciones/{id} */
export async function getDevolucion(id, opts = {}) {
  if (USE_MOCK) {
    const err = new Error('getDevolucion: usar listados mock')
    err.status = 501
    throw err
  }
  return http(`/api/devoluciones/${id}`, { token: opts.token })
}
