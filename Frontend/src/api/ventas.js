import { http } from './http'
import { USE_MOCK, USE_MOCK_ENTRADAS } from './config'
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
 * Devoluciones: siguen flag de entradas (mismo msvc).
 * Cancelados/reintegro: mock hasta E1-EVE cancel + lista.
 */
export async function listColaAdminVentas(user, opts = {}) {
  requireAdmin(user)

  if (USE_MOCK_ENTRADAS) {
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

  // Gateway real: pendientes de msvc-entradas
  const devoluciones = await http('/api/devoluciones', { token: opts.token })
  const list = Array.isArray(devoluciones)
    ? devoluciones
    : devoluciones?.content || devoluciones?.items || []

  // Normalizar shape UI (tipo opcional en BE)
  const normalized = list.map((d) => ({
    ...d,
    tipo: d.tipo || 'DEVOLUCION',
    eventoNombre: d.eventoNombre || d.idEvento || '—',
  }))

  return {
    devoluciones: normalized,
    eventosCancelados: [],
    entradasEventosCancelados: [],
  }
}

/**
 * Resolver devolución admin.
 * APROBADA → POST /api/devoluciones/{id}/aprobacion
 * RECHAZADA → POST /api/devoluciones/{id}/rechazo
 *
 * Importante: el id es el de la **devolución**, no el de la entrada.
 */
export async function resolverDevolucion(id, estado, user, opts = {}) {
  requireAdmin(user)
  const next = String(estado || '').toUpperCase()
  const devId = String(id || '').trim()
  if (!devId) {
    const err = new Error('Id de devolución requerido')
    err.status = 400
    throw err
  }

  if (USE_MOCK_ENTRADAS) {
    await delay()
    const d = patchDevolucionEstado(devId, next, user.id)
    if (!d) {
      const err = new Error('Solicitud no encontrada')
      err.status = 404
      throw err
    }
    if (next === DEVOLUCION_ESTADOS.APROBADA && d.idEntrada) {
      const e = patchEntradaEstado(d.idEntrada, ENTRADA_ESTADOS.DEVUELTA)
      if (!e) {
        // devolución mock ok pero entrada era de otro store (p.ej. BE) — no fallar duro
        console.warn('Mock: entrada no estaba en store local', d.idEntrada)
      }
    }
    return d
  }

  if (next === DEVOLUCION_ESTADOS.APROBADA || next === 'APROBADA') {
    return http(`/api/devoluciones/${devId}/aprobacion`, {
      method: 'POST',
      token: opts.token,
    })
  }
  if (next === DEVOLUCION_ESTADOS.RECHAZADA || next === 'RECHAZADA') {
    return http(`/api/devoluciones/${devId}/rechazo`, {
      method: 'POST',
      token: opts.token,
    })
  }
  const err = new Error('Estado de resolución inválido')
  err.status = 400
  throw err
}

/**
 * Reintegro por cancelación de evento.
 * Real: POST /api/eventos/{idEvento}/reintegro-cancelacion (marca todas).
 * UI legacy pasaba idEntrada — si parece UUID de evento, usamos ese path.
 */
export async function marcarReintegroEntradaCancelada(entradaId, user, opts = {}) {
  requireAdmin(user)
  if (USE_MOCK_ENTRADAS) {
    await delay()
    const e = patchEntradaEstado(entradaId, ENTRADA_ESTADOS.A_REINTEGRAR)
    if (!e) {
      const err = new Error('Entrada no encontrada')
      err.status = 404
      throw err
    }
    return e
  }

  // Si nos pasan idEvento (desde fila de evento cancelado), endpoint de contrato E1-ENT-05
  if (opts.idEvento) {
    return http(`/api/eventos/${opts.idEvento}/reintegro-cancelacion`, {
      method: 'POST',
      token: opts.token,
    })
  }

  const err = new Error(
    'Reintegro por entrada suelta no está en BE. Usá cancelación de evento (reintegro masivo) o devolución.'
  )
  err.status = 501
  throw err
}
