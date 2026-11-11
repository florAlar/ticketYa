import { capacidadDeLayout, layoutValido, parseEntero } from './asiento'

/**
 * Reglas de los formularios de lugares y sectores (LUG-01/LUG-02). Reflejan las del backend:
 * nombre y ciudad obligatorios; latitud y longitud opcionales, juntas y dentro de rango; sector
 * numerado con layout filas × asientos por fila (capacidad derivada).
 */

export const TIPOS_SECTOR = ['GENERAL', 'CAMPO', 'PLATEA', 'PALCO', 'VIP']

export const FORM_LUGAR_VACIO = {
  nombre: '',
  ciudad: '',
  direccion: '',
  latitud: '',
  longitud: '',
}

export const FORM_SECTOR_VACIO = {
  nombre: '',
  tipo: 'GENERAL',
  esNumerado: false,
  capacidad: '',
  filas: '',
  asientosPorFila: '',
}

const texto = (v) => (v === null || v === undefined ? '' : String(v))

export function lugarToForm(lugar) {
  return {
    nombre: texto(lugar?.nombre),
    ciudad: texto(lugar?.ciudad),
    direccion: texto(lugar?.direccion),
    latitud: texto(lugar?.latitud),
    longitud: texto(lugar?.longitud),
  }
}

/** '' → null; otro valor → Number (NaN si es basura). */
function coordenada(valor) {
  const s = texto(valor).trim().replace(',', '.')
  return s === '' ? null : Number(s)
}

/** @returns {{ ok: boolean, error?: string }} */
export function validateLugarForm(form) {
  if (!texto(form?.nombre).trim()) return { ok: false, error: 'El nombre es obligatorio' }
  if (!texto(form?.ciudad).trim()) return { ok: false, error: 'La ciudad es obligatoria' }
  const lat = coordenada(form?.latitud)
  const lng = coordenada(form?.longitud)
  if (lat === null && lng === null) return { ok: true }
  if (lat === null || lng === null) {
    return { ok: false, error: 'Latitud y longitud van juntas: completá las dos o dejá las dos vacías' }
  }
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return { ok: false, error: 'Latitud y longitud deben ser números (ej. -34.5453)' }
  }
  if (lat < -90 || lat > 90) return { ok: false, error: 'La latitud debe estar entre -90 y 90' }
  if (lng < -180 || lng > 180) return { ok: false, error: 'La longitud debe estar entre -180 y 180' }
  return { ok: true }
}

/**
 * Pedido para POST/PUT /api/lugares. En edición, si el lugar tenía coordenadas y se vaciaron los dos
 * campos, se envía quitarCoordenadas para borrarlas (sin eso el backend conserva las existentes).
 */
export function lugarFormToPayload(form, { teniaCoordenadas = false, esEdicion = false } = {}) {
  const payload = {
    nombre: texto(form.nombre).trim(),
    ciudad: texto(form.ciudad).trim(),
    direccion: texto(form.direccion).trim(),
  }
  const lat = coordenada(form.latitud)
  const lng = coordenada(form.longitud)
  if (lat !== null && lng !== null) {
    payload.latitud = lat
    payload.longitud = lng
  } else if (esEdicion && teniaCoordenadas) {
    payload.quitarCoordenadas = true
  }
  return payload
}

export function sectorToForm(sector) {
  return {
    nombre: texto(sector?.nombre),
    tipo: texto(sector?.tipo) || 'GENERAL',
    esNumerado: Boolean(sector?.esNumerado),
    capacidad: sector?.esNumerado ? '' : texto(sector?.capacidad),
    filas: texto(sector?.filas),
    asientosPorFila: texto(sector?.asientosPorFila),
  }
}

export function validateSectorForm(form) {
  if (!texto(form?.nombre).trim()) return { ok: false, error: 'El nombre del sector es obligatorio' }
  if (form.esNumerado) {
    const filas = parseEntero(form.filas)
    const porFila = parseEntero(form.asientosPorFila)
    if (!layoutValido(filas, porFila)) {
      return { ok: false, error: 'Un sector numerado necesita filas (1 a 200) y asientos por fila (1 a 500), enteros' }
    }
    return { ok: true }
  }
  const cap = parseEntero(form.capacidad)
  if (!Number.isInteger(cap) || cap < 1) {
    return { ok: false, error: 'La capacidad debe ser un entero de 1 o más' }
  }
  return { ok: true }
}

/** Pedido para POST /lugares/{id}/sectores y PUT /sectores/{id}. */
export function sectorFormToPayload(form) {
  const numerado = Boolean(form.esNumerado)
  const base = {
    nombre: texto(form.nombre).trim(),
    tipo: texto(form.tipo).trim() || null,
    esNumerado: numerado,
  }
  if (numerado) {
    return {
      ...base,
      filas: parseEntero(form.filas),
      asientosPorFila: parseEntero(form.asientosPorFila),
    }
  }
  return { ...base, capacidad: parseEntero(form.capacidad) }
}

/** Texto corto de la capacidad de un sector (para listados). */
export function describirSector(sector) {
  if (sector?.esNumerado) {
    const cap = capacidadDeLayout(Number(sector.filas), Number(sector.asientosPorFila))
    return `numerado · ${sector.filas} filas × ${sector.asientosPorFila} asientos${cap != null ? ` = ${cap}` : ''}`
  }
  return `capacidad ${sector?.capacidad ?? 0}`
}
