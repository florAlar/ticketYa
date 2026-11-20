/**
 * Fila virtual (msvc-ventas, Redis) — fachada única del SPA.
 *
 * Paths vía gateway:
 *   GET  /api/eventos/{id}/fila/estado       (público)
 *   POST /api/eventos/{id}/fila/ingreso      (CLIENTE)
 *   GET  /api/eventos/{id}/fila/posicion     (CLIENTE)
 *   POST /api/eventos/{id}/fila/validacion   (CLIENTE)
 *   POST /api/eventos/{id}/fila/habilitacion (ADMIN)
 *
 * `estado` es público; el resto exige JWT. En mock se simula el estado efímero
 * en localStorage (ver `mock/filaStore.js`).
 */
import { http } from './http'
import { USE_MOCK } from './config'
import { readGestionEventos } from './mock/eventosStore'
import {
  filaError,
  mockConsumirTurno,
  mockEstadoPublico,
  mockHabilitar,
  mockIngreso,
  mockPosicion,
  mockValidacion,
} from './mock/filaStore'
import { FILA_ERROR_CODES } from '../domain/filaVirtual'

const delay = (ms = 220) => new Promise((r) => setTimeout(r, ms))

function findMockEvento(idEvento) {
  return (
    readGestionEventos().find((e) => e.id === String(idEvento)) || null
  )
}

function mockEventoElegible(idEvento) {
  const evento = findMockEvento(idEvento)
  if (!evento) {
    throw filaError(404, null, 'Evento no encontrado')
  }
  const abierta =
    String(evento.estado || '').toUpperCase() === 'PUBLICADO' &&
    Boolean(evento.esAltaDemanda)
  if (!abierta) {
    throw filaError(
      409,
      FILA_ERROR_CODES.EVENTO_NO_ELEGIBLE,
      'El evento no está PUBLICADO o no es de alta demanda'
    )
  }
  return evento
}

function requireUser(userId) {
  if (!userId) {
    throw filaError(401, null, 'Falta identidad del cliente')
  }
}

/** GET /api/eventos/{id}/fila/estado — público. */
export async function getEstadoFila(idEvento, opts = {}) {
  if (USE_MOCK) {
    await delay(120)
    const evento = findMockEvento(idEvento)
    if (!evento) throw filaError(404, null, 'Evento no encontrado')
    const abierta =
      String(evento.estado || '').toUpperCase() === 'PUBLICADO' &&
      Boolean(evento.esAltaDemanda)
    return mockEstadoPublico(String(idEvento), abierta)
  }
  return http(`/api/eventos/${idEvento}/fila/estado`, {
    signal: opts.signal,
  })
}

/** POST /api/eventos/{id}/fila/ingreso — 201 la primera vez, 200 reingreso. */
export async function ingresarFila(idEvento, opts = {}) {
  if (USE_MOCK) {
    await delay(320)
    mockEventoElegible(idEvento)
    requireUser(opts.userId)
    return mockIngreso(String(idEvento), opts.userId)
  }
  return http(`/api/eventos/${idEvento}/fila/ingreso`, {
    method: 'POST',
    token: opts.token,
  })
}

/** GET /api/eventos/{id}/fila/posicion — turno propio. */
export async function getMiPosicionFila(idEvento, opts = {}) {
  if (USE_MOCK) {
    await delay(180)
    mockEventoElegible(idEvento)
    requireUser(opts.userId)
    return mockPosicion(String(idEvento), opts.userId)
  }
  return http(`/api/eventos/${idEvento}/fila/posicion`, {
    token: opts.token,
    signal: opts.signal,
  })
}

/** POST /api/eventos/{id}/fila/validacion — emite/devuelve el token. */
export async function validarFila(idEvento, opts = {}) {
  if (USE_MOCK) {
    await delay(320)
    mockEventoElegible(idEvento)
    requireUser(opts.userId)
    return mockValidacion(String(idEvento), opts.userId)
  }
  return http(`/api/eventos/${idEvento}/fila/validacion`, {
    method: 'POST',
    token: opts.token,
  })
}

/** POST /api/eventos/{id}/fila/habilitacion — override ADMIN. */
export async function habilitarFila(idEvento, hastaPosicion, opts = {}) {
  if (USE_MOCK) {
    await delay(250)
    mockEventoElegible(idEvento)
    return mockHabilitar(String(idEvento), hastaPosicion)
  }
  return http(`/api/eventos/${idEvento}/fila/habilitacion`, {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify({ hastaPosicion: Number(hastaPosicion) }),
  })
}

/** Mock: registra el turno usado al confirmar una compra con `tokenFila`. */
export function consumirTurnoFilaMock(idEvento, userId, tokenFila) {
  if (!USE_MOCK || !tokenFila) return false
  return mockConsumirTurno(String(idEvento), userId, tokenFila)
}
