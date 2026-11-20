/**
 * Fila virtual de alta demanda (FIL-01…FIL-03).
 *
 * El turno, la tanda y el token de compra son efímeros: se guardan en Redis en el
 * backend y el token viaja al front solo para enviarlo como `tokenFila` en
 * `POST /reservas`. Acá viven las etiquetas de estado, los códigos de error y el
 * almacenamiento local del token de compra (sessionStorage, por pestaña).
 */

export const FILA_ESTADOS = {
  EN_ESPERA: 'EN_ESPERA',
  HABILITADO: 'HABILITADO',
  CON_TOKEN: 'CON_TOKEN',
  TURNO_USADO: 'TURNO_USADO',
}

export const FILA_ESTADO_LABELS = {
  [FILA_ESTADOS.EN_ESPERA]: 'En espera',
  [FILA_ESTADOS.HABILITADO]: 'Es tu turno',
  [FILA_ESTADOS.CON_TOKEN]: 'Turno validado',
  [FILA_ESTADOS.TURNO_USADO]: 'Turno usado',
}

export const FILA_ERROR_CODES = {
  EVENTO_NO_ELEGIBLE: 'FILA_EVENTO_NO_ELEGIBLE',
  NO_INGRESADO: 'FILA_NO_INGRESADO',
  FUERA_DE_TANDA: 'FILA_FUERA_DE_TANDA',
  TURNO_USADO: 'FILA_TURNO_USADO',
  TANDA_NO_REDUCIBLE: 'FILA_TANDA_NO_REDUCIBLE',
  TOKEN_REQUERIDO: 'FILA_TOKEN_REQUERIDO',
  TOKEN_INVALIDO: 'FILA_TOKEN_INVALIDO',
  NO_DISPONIBLE: 'FILA_NO_DISPONIBLE',
}

const TOKEN_STORE_KEY = 'ticketya_fila_tokens'
/** TTL de referencia si el backend no informa `expiraEn` (el BE usa 10 min). */
const TOKEN_TTL_FALLBACK_MS = 10 * 60 * 1000

function readTokenStore() {
  try {
    const raw = sessionStorage.getItem(TOKEN_STORE_KEY)
    const parsed = raw ? JSON.parse(raw) : null
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeTokenStore(store) {
  try {
    sessionStorage.setItem(TOKEN_STORE_KEY, JSON.stringify(store))
  } catch {
    /* sessionStorage bloqueado o lleno: el token sigue en memoria de la página */
  }
}

function expiraEnMs(entry) {
  if (!entry?.expiraEn) return null
  const ts = new Date(entry.expiraEn).getTime()
  return Number.isNaN(ts) ? null : ts
}

/** `true` si el token existe y todavía no venció. */
export function tokenFilaVigente(entry) {
  if (!entry?.token) return false
  const ts = expiraEnMs(entry)
  return ts != null && ts > Date.now()
}

/** Token vigente del evento, o `null`. Limpia el vencido de sessionStorage. */
export function getFilaToken(idEvento) {
  const key = String(idEvento || '')
  if (!key) return null
  const store = readTokenStore()
  const entry = store[key]
  if (!entry) return null
  if (!tokenFilaVigente(entry)) {
    delete store[key]
    writeTokenStore(store)
    return null
  }
  return entry
}

/** Guarda el token emitido por `POST /fila/validacion`. */
export function saveFilaToken(idEvento, payload) {
  const key = String(idEvento || '')
  if (!key || !payload?.token) return null
  const expiraEn =
    payload.expiraEn || new Date(Date.now() + TOKEN_TTL_FALLBACK_MS).toISOString()
  const entry = {
    token: payload.token,
    expiraEn,
    posicion: payload.posicion ?? null,
  }
  const store = readTokenStore()
  store[key] = entry
  writeTokenStore(store)
  return entry
}

/** Descarta el token de compra del evento (vencido, usado o consumido). */
export function clearFilaToken(idEvento) {
  const key = String(idEvento || '')
  if (!key) return
  const store = readTokenStore()
  if (!store[key]) return
  delete store[key]
  writeTokenStore(store)
}

/** Código de negocio del error (`FILA_*`), o `null`. */
export function codigoErrorFila(error) {
  return error?.code || error?.data?.code || null
}

/** Extras de `FILA_FUERA_DE_TANDA`: posicion, tanda y delante. */
export function extrasErrorFila(error) {
  const data = error?.data || {}
  const posicion = Number(data.posicion)
  const tanda = Number(data.posicionHabilitadaHasta ?? data.tanda)
  const delante = Number(data.delante)
  return {
    posicion: Number.isFinite(posicion) ? posicion : null,
    posicionHabilitadaHasta: Number.isFinite(tanda) ? tanda : null,
    delante: Number.isFinite(delante) ? delante : null,
  }
}

/** Error de fila que invalida el pase de compra guardado. */
export function esTokenDeFilaInvalido(error) {
  const code = codigoErrorFila(error)
  return (
    code === FILA_ERROR_CODES.TOKEN_REQUERIDO ||
    code === FILA_ERROR_CODES.TOKEN_INVALIDO
  )
}

/** Porcentaje 0..100 de la tanda respecto de la posición propia. */
export function progresoFila(posicion, posicionHabilitadaHasta) {
  const pos = Number(posicion)
  const tanda = Number(posicionHabilitadaHasta)
  if (!Number.isFinite(pos) || pos < 1) return 0
  if (!Number.isFinite(tanda) || tanda < 0) return 0
  return Math.min(100, Math.max(0, Math.round((tanda / pos) * 100)))
}
