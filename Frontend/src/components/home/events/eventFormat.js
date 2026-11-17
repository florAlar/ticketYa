import { BANNER_IMAGES } from '../../../assets/img/banners'

function pad(n) {
  return String(n).padStart(2, '0')
}

function hashEvento(value) {
  let hash = 0
  const text = String(value || '')
  for (let i = 0; i < text.length; i++) {
    hash = (hash * 31 + text.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

/**
 * Imagen de la card/detalle. El backend no expone imagenUrl (null): se usa un
 * banner estático del pool según el tipo, elegido de forma determinística por
 * evento para que las cards no queden todas iguales.
 */
export function imagenEvento(evento) {
  if (evento?.imagenUrl) return evento.imagenUrl
  const tipo = String(evento?.tipo || '').toUpperCase()
  const pools = {
    RECITAL: [BANNER_IMAGES[0], BANNER_IMAGES[2], BANNER_IMAGES[3], BANNER_IMAGES[4]],
    FESTIVAL: [BANNER_IMAGES[0], BANNER_IMAGES[1], BANNER_IMAGES[3]],
    DEPORTE: [BANNER_IMAGES[5], BANNER_IMAGES[1]],
    TEATRO: [BANNER_IMAGES[4], BANNER_IMAGES[3]],
    CONFERENCIA: [BANNER_IMAGES[4], BANNER_IMAGES[3]],
  }
  const pool = pools[tipo] || BANNER_IMAGES
  const key = evento?.id || evento?.nombre || tipo
  return pool[hashEvento(key) % pool.length]
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
  if (n == null || n === '') return ''
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 0,
  }).format(n)
}
