import { formatMoney } from '../../../lib/format'

function pad(n) {
  return String(n).padStart(2, '0')
}

/** Fecha corta es-AR para cards */
export function formatEventoFecha(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const dias = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']
  return `${dias[d.getDay()]} ${pad(d.getDate())}/${pad(d.getMonth() + 1)} · ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatPrecioDesde(n) {
  return formatMoney(n, { fallback: '' })
}

const MESES = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC']

/** Día y mes abreviado para el chip de fecha de las cards. */
export function partesFecha(iso) {
  if (!iso) return null
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return null
  return { dia: pad(d.getDate()), mes: MESES[d.getMonth()] }
}

/**
 * Parte el nombre del evento en banda/equipo y nombre del show: "Metallica — M72 World Tour" →
 * { artista: 'Metallica', titulo: 'M72 World Tour' }. Sin separador, todo es el título.
 */
export function partesNombre(nombre) {
  const [artista, ...resto] = String(nombre || '').split(' — ')
  return resto.length
    ? { artista: artista.trim(), titulo: resto.join(' — ').trim() }
    : { artista: '', titulo: String(nombre || '').trim() }
}
