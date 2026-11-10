/**
 * Asientos virtuales de sectores numerados (espejo de la convención del backend).
 *
 * El sector guarda `filas` x `asientosPorFila`; no hay asientos persistidos. Un asiento se
 * identifica por (fila, número): fila A..Z, AA..AZ, BA... y número 1..asientosPorFila.
 * La capacidad de un sector numerado SIEMPRE se deriva: filas * asientosPorFila.
 */

export const MAX_FILAS = 200
export const MAX_ASIENTOS_POR_FILA = 500
/** Tope por compra en sectores numerados (igual al límite por cliente/evento del BE). */
export const MAX_ASIENTOS_POR_COMPRA = 6

/** Rótulo de fila base 1: 1→A, 26→Z, 27→AA, 28→AB, ... */
export function rotuloFila(indice) {
  let n = Number(indice)
  if (!Number.isInteger(n) || n < 1) return ''
  let out = ''
  while (n > 0) {
    n -= 1
    out = String.fromCharCode(65 + (n % 26)) + out
    n = Math.floor(n / 26)
  }
  return out
}

/** Índice base 1 del rótulo (A=1, AA=27); 0 si no es un rótulo válido. */
export function indiceFila(rotulo) {
  const r = String(rotulo || '').trim().toUpperCase()
  if (!r || r.length > 3 || !/^[A-Z]+$/.test(r)) return 0
  let idx = 0
  for (const c of r) {
    idx = idx * 26 + (c.charCodeAt(0) - 64)
  }
  return idx
}

/** '' / null → null; otro valor → Number (puede ser NaN si es basura). */
export function parseEntero(value) {
  if (value === '' || value == null) return null
  return Number(value)
}

/** true si filas y asientosPorFila son enteros dentro de rango. */
export function layoutValido(filas, asientosPorFila) {
  return (
    Number.isInteger(filas) &&
    Number.isInteger(asientosPorFila) &&
    filas >= 1 &&
    filas <= MAX_FILAS &&
    asientosPorFila >= 1 &&
    asientosPorFila <= MAX_ASIENTOS_POR_FILA
  )
}

/** filas * asientosPorFila, o null si el layout no es válido. */
export function capacidadDeLayout(filas, asientosPorFila) {
  return layoutValido(filas, asientosPorFila) ? filas * asientosPorFila : null
}

/** El sector numerado tiene layout utilizable para elegir asientos. */
export function sectorTieneLayout(sector) {
  return Boolean(
    sector?.esNumerado &&
      layoutValido(Number(sector.filas), Number(sector.asientosPorFila))
  )
}

export function codigoAsiento(fila, numero) {
  return `${fila}-${numero}`
}

/** Ordena por fila (A, B, ... AA) y número. */
export function compararAsientos(a, b) {
  return indiceFila(a.fila) - indiceFila(b.fila) || a.numero - b.numero
}

/**
 * Asientos que un mensaje de conflicto del BE dice no disponibles.
 * Ej.: "Asiento(s) no disponible(s) en sector <id>: A-2, A-3" → [{fila:'A',numero:2}, ...]
 * Solo mira lo que sigue al último ': ' (el id del sector contiene guiones).
 */
export function asientosDeMensajeConflicto(mensaje) {
  const texto = String(mensaje || '')
  if (!/asiento/i.test(texto)) return []
  const cola = texto.split(': ').pop()
  const out = []
  const re = /\b([A-Z]{1,3})-(\d{1,3})\b/g
  let m
  while ((m = re.exec(cola)) !== null) {
    out.push({ fila: m[1], numero: Number(m[2]) })
  }
  return out
}
