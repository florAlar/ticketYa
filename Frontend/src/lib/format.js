/**
 * Formateadores compartidos (es-AR).
 * Centraliza fecha/hora y moneda para que las pantallas no dupliquen
 * instancias de Intl ni criterios de fallback.
 */

const FALLBACK = '—'

const currencyFormatter = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
})

/**
 * Fecha + hora legible. Devuelve `fallback` si el valor no es una fecha válida.
 * @param {string|number|Date|null|undefined} iso
 * @param {Intl.DateTimeFormatOptions} [options]
 * @param {string} [fallback]
 */
export function formatFecha(
  iso,
  options = { dateStyle: 'medium', timeStyle: 'short' },
  fallback = FALLBACK
) {
  if (iso == null || iso === '') return fallback
  const date = iso instanceof Date ? iso : new Date(iso)
  if (Number.isNaN(date.getTime())) return fallback
  try {
    return date.toLocaleString('es-AR', options)
  } catch {
    return fallback
  }
}

/** Solo fecha (sin hora). */
export function formatFechaCorta(iso, fallback = FALLBACK) {
  return formatFecha(iso, { dateStyle: 'medium' }, fallback)
}

/**
 * Moneda ARS sin decimales.
 * @param {number|string|null|undefined} value
 * @param {{ fallback?: string }} [options]
 */
export function formatMoney(value, { fallback = FALLBACK } = {}) {
  if (value == null || value === '') return fallback
  const number = Number(value)
  if (!Number.isFinite(number)) return fallback
  try {
    return currencyFormatter.format(number)
  } catch {
    return fallback
  }
}

/** Porcentaje con hasta 2 decimales. */
export function formatPct(value, fallback = FALLBACK) {
  if (value == null || value === '') return fallback
  const number = Number(value)
  if (!Number.isFinite(number)) return fallback
  return `${number.toLocaleString('es-AR', { maximumFractionDigits: 2 })} %`
}

/** Entero con separador de miles. */
export function formatEntero(value, fallback = FALLBACK) {
  if (value == null || value === '') return fallback
  const number = Number(value)
  if (!Number.isFinite(number)) return fallback
  return number.toLocaleString('es-AR')
}
