/**
 * Store mock de la fila virtual (solo `VITE_USE_MOCK=true`).
 *
 * Simula el estado efímero de Redis: turno por cliente, tanda acumulativa,
 * token temporal y consumo del turno al comprar. La tanda avanza sola cada
 * pocos segundos para que la demo se pueda recorrer sin backend.
 */
import { FILA_ERROR_CODES } from '../../domain/filaVirtual.js'

const STORE_KEY = 'ticketya_mock_fila'
const TOKEN_TTL_MS = 10 * 60 * 1000
const AUTO_ADVANCE_MS = 7000
const AUTO_ADVANCE_STEP = 3

function newToken() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `fila-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function hashEvento(idEvento) {
  return [...String(idEvento)].reduce(
    (acc, ch) => (acc * 31 + ch.charCodeAt(0)) % 997,
    0
  )
}

function initialState(idEvento) {
  // Cola ficticia inicial: el primer cliente real entra detrás de 8–15 turnos.
  const seq = 8 + (hashEvento(idEvento) % 8)
  return {
    seq,
    tanda: Math.max(0, seq - 5),
    usados: [],
    turnos: {},
    tokens: {},
    lastAdvance: Date.now(),
  }
}

function readStore() {
  try {
    const raw = localStorage.getItem(STORE_KEY)
    const parsed = raw ? JSON.parse(raw) : null
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeStore(store) {
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify(store))
  } catch {
    /* localStorage bloqueado: la demo sigue en memoria de esta operación */
  }
}

function avanzarTanda(state) {
  const now = Date.now()
  const elapsed = now - Number(state.lastAdvance || 0)
  if (elapsed < AUTO_ADVANCE_MS || state.tanda >= state.seq) return
  const pasos = Math.floor(elapsed / AUTO_ADVANCE_MS)
  state.tanda = Math.min(
    state.seq,
    state.tanda + pasos * AUTO_ADVANCE_STEP
  )
  state.lastAdvance = now
}

function getState(idEvento) {
  const store = readStore()
  const key = String(idEvento)
  if (!store[key]) store[key] = initialState(key)
  avanzarTanda(store[key])
  writeStore(store)
  return store[key]
}

function saveState(idEvento, state) {
  const store = readStore()
  store[String(idEvento)] = state
  writeStore(store)
  return state
}

function tokenVigente(entry) {
  if (!entry?.token || !entry?.expiraEn) return false
  return new Date(entry.expiraEn).getTime() > Date.now()
}

function estadoTurno(state, userId) {
  const posicion = Number(state.turnos[userId])
  if (!Number.isFinite(posicion)) return null
  if (state.usados.includes(userId)) return 'TURNO_USADO'
  if (tokenVigente(state.tokens[userId])) return 'CON_TOKEN'
  return posicion <= state.tanda ? 'HABILITADO' : 'EN_ESPERA'
}

function posicionDto(idEvento, state, userId) {
  const posicion = Number(state.turnos[userId])
  return {
    idEvento,
    posicion,
    posicionHabilitadaHasta: state.tanda,
    delante: Math.max(0, posicion - state.tanda),
    estado: estadoTurno(state, userId),
  }
}

export function filaError(status, code, message, extras = {}) {
  const err = new Error(message)
  err.status = status
  err.code = code
  err.data = { status, code, message, ...extras }
  return err
}

export function mockEstadoPublico(idEvento, abierta) {
  if (!abierta) {
    return {
      idEvento,
      totalEnFila: 0,
      posicionHabilitadaHasta: 0,
      abierta: false,
    }
  }
  const state = getState(idEvento)
  return {
    idEvento,
    totalEnFila: Math.max(0, state.seq - state.usados.length),
    posicionHabilitadaHasta: state.tanda,
    abierta: true,
  }
}

export function mockIngreso(idEvento, userId) {
  if (!userId) {
    throw filaError(401, null, 'Falta identidad del cliente')
  }
  const state = getState(idEvento)
  const key = String(userId)

  if (state.usados.includes(key)) {
    throw filaError(
      409,
      FILA_ERROR_CODES.TURNO_USADO,
      'El cliente ya consumió su turno en este evento'
    )
  }

  const reingreso = Number.isFinite(Number(state.turnos[key]))
  if (!reingreso) {
    state.seq += 1
    state.turnos[key] = state.seq
    saveState(idEvento, state)
  }

  return { ...posicionDto(idEvento, state, key), reingreso }
}

export function mockPosicion(idEvento, userId) {
  const state = getState(idEvento)
  const key = String(userId)
  if (!Number.isFinite(Number(state.turnos[key]))) {
    throw filaError(
      404,
      FILA_ERROR_CODES.NO_INGRESADO,
      'El cliente no ingresó a la fila de este evento'
    )
  }
  return posicionDto(idEvento, state, key)
}

export function mockValidacion(idEvento, userId) {
  const state = getState(idEvento)
  const key = String(userId)
  const posicion = Number(state.turnos[key])
  if (!Number.isFinite(posicion)) {
    throw filaError(
      404,
      FILA_ERROR_CODES.NO_INGRESADO,
      'El cliente no ingresó a la fila de este evento'
    )
  }
  if (state.usados.includes(key)) {
    throw filaError(
      409,
      FILA_ERROR_CODES.TURNO_USADO,
      'El cliente ya consumió su turno en este evento'
    )
  }
  if (posicion > state.tanda) {
    throw filaError(
      409,
      FILA_ERROR_CODES.FUERA_DE_TANDA,
      'Aún no es tu turno',
      {
        posicion,
        posicionHabilitadaHasta: state.tanda,
        delante: Math.max(0, posicion - state.tanda),
      }
    )
  }

  let entry = state.tokens[key]
  if (!tokenVigente(entry)) {
    entry = {
      token: newToken(),
      expiraEn: new Date(Date.now() + TOKEN_TTL_MS).toISOString(),
      posicion,
    }
    state.tokens[key] = entry
    saveState(idEvento, state)
  }

  return {
    idEvento,
    token: entry.token,
    expiraEn: entry.expiraEn,
    posicion,
    delante: 0,
  }
}

export function mockHabilitar(idEvento, hastaPosicion) {
  const hasta = Number(hastaPosicion)
  if (!Number.isInteger(hasta) || hasta < 1) {
    throw filaError(400, null, 'hastaPosicion: debe ser mayor o igual a 1')
  }
  const state = getState(idEvento)
  if (hasta < state.tanda) {
    throw filaError(
      409,
      FILA_ERROR_CODES.TANDA_NO_REDUCIBLE,
      'La tanda no puede retroceder'
    )
  }
  state.tanda = hasta
  state.lastAdvance = Date.now()
  saveState(idEvento, state)
  return {
    idEvento,
    posicionHabilitadaHasta: hasta,
    modo: 'MANUAL',
  }
}

/**
 * Consume el token al confirmar la reserva (mock). Devuelve `true` si el turno
 * queda usado.
 */
export function mockConsumirTurno(idEvento, userId, tokenFila) {
  const state = getState(idEvento)
  const key = String(userId)
  const entry = state.tokens[key]
  if (!tokenFila || !entry || entry.token !== tokenFila) return false
  if (!tokenVigente(entry)) return false
  delete state.tokens[key]
  if (!state.usados.includes(key)) state.usados.push(key)
  saveState(idEvento, state)
  return true
}
