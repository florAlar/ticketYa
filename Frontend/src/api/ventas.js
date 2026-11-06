import { http } from './http'
import { USE_MOCK } from './config'
import {
  listDevolucionesPendientes,
  patchDevolucionEstado,
} from './mock/devolucionesStore'
import { listEventosCancelados } from './mock/eventosStore'
import {
  listByEventoIds,
  patchEntradaEstado,
} from './mock/entradasStore'
import { ENTRADA_ESTADOS } from '../domain/entrada'
import { DEVOLUCION_ESTADOS } from '../domain/devolucion'

const delay = (ms = 280) => new Promise((r) => setTimeout(r, ms))

function requireAdmin(user) {
  if (!user?.id) {
    const err = new Error('No autenticado')
    err.status = 401
    throw err
  }
  if (String(user.rol).toUpperCase() !== 'ADMIN') {
    const err = new Error('Solo administrador')
    err.status = 403
    throw err
  }
}

/**
 * Cola operativa admin (UI).
 * Mock: compone stores locales.
 * Gateway (contrato back):
 *   - pendientes → GET /api/devoluciones
 *   - cancelados: no hay cola única; mock rellena; real deja arrays vacíos
 *     o el BE los agrega después.
 */
export async function listColaAdminVentas(user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const devoluciones = listDevolucionesPendientes()
    const cancelados = listEventosCancelados()
    const entradasCancelados = listByEventoIds(cancelados.map((e) => e.id)).filter(
      (e) =>
        e.estado === ENTRADA_ESTADOS.VENDIDA ||
        e.estado === ENTRADA_ESTADOS.A_REINTEGRAR
    )
    return {
      devoluciones,
      eventosCancelados: cancelados.map((e) => ({
        id: e.id,
        nombre: e.nombre,
        fechaInicio: e.fechaInicio || e.fechaHora,
        estado: e.estado,
      })),
      entradasEventosCancelados: entradasCancelados,
    }
  }

  const devoluciones = await http('/api/devoluciones', { token: opts.token })
  return {
    devoluciones: Array.isArray(devoluciones)
      ? devoluciones
      : devoluciones?.content || devoluciones?.items || [],
    eventosCancelados: [],
    entradasEventosCancelados: [],
  }
}

/**
 * Resolver devolución admin.
 * APROBADA → POST /api/devoluciones/{id}/aprobacion
 * RECHAZADA → POST /api/devoluciones/{id}/rechazo
 */
export async function resolverDevolucion(id, estado, user, opts = {}) {
  requireAdmin(user)
  const next = String(estado || '').toUpperCase()

  if (USE_MOCK) {
    await delay()
    const d = patchDevolucionEstado(id, next, user.id)
    if (!d) {
      const err = new Error('Solicitud no encontrada')
      err.status = 404
      throw err
    }
    if (next === DEVOLUCION_ESTADOS.APROBADA && d.idEntrada) {
      patchEntradaEstado(d.idEntrada, ENTRADA_ESTADOS.DEVUELTA)
    }
    return d
  }

  if (next === DEVOLUCION_ESTADOS.APROBADA || next === 'APROBADA') {
    return http(`/api/devoluciones/${id}/aprobacion`, {
      method: 'POST',
      token: opts.token,
    })
  }
  if (next === DEVOLUCION_ESTADOS.RECHAZADA || next === 'RECHAZADA') {
    return http(`/api/devoluciones/${id}/rechazo`, {
      method: 'POST',
      token: opts.token,
    })
  }
  const err = new Error('Estado de resolución inválido')
  err.status = 400
  throw err
}

/**
 * Reintegro por cancelación de evento — solo mock UI.
 * No hay path homólogo en endpoints-back; cancelación dispara side-effects en BE.
 */
export async function marcarReintegroEntradaCancelada(entradaId, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK) {
    await delay()
    const e = patchEntradaEstado(entradaId, ENTRADA_ESTADOS.A_REINTEGRAR)
    if (!e) {
      const err = new Error('Entrada no encontrada')
      err.status = 404
      throw err
    }
    return e
  }
  const err = new Error(
    'Reintegro por cancelación lo resuelve el BE al cancelar el evento (sin endpoint front dedicado)'
  )
  err.status = 501
  throw err
}
