/**
 * Formas de dominio (referencia a entidades BE msvc-eventos MySQL).
 * El mock JSON del front sigue esta forma; se reacomoda cuando el BE exponga DTOs.
 *
 * msvc-eventos = MySQL (confirmado). Notificaciones = msvc propio futuro (no mockeamos aún).
 */

export const EVENTO_TIPOS = {
  RECITAL: 'RECITAL',
  TEATRO: 'TEATRO',
  DEPORTE: 'DEPORTE',
  CONFERENCIA: 'CONFERENCIA',
  FESTIVAL: 'FESTIVAL',
}

export const EVENTO_TIPO_LABELS = {
  [EVENTO_TIPOS.RECITAL]: 'Recital',
  [EVENTO_TIPOS.TEATRO]: 'Teatro',
  [EVENTO_TIPOS.DEPORTE]: 'Deporte',
  [EVENTO_TIPOS.CONFERENCIA]: 'Conferencia',
  [EVENTO_TIPOS.FESTIVAL]: 'Festival',
}

/** Estados de ciclo de vida del evento (ms-eventos). */
export const EVENTO_ESTADOS = {
  BORRADOR: 'BORRADOR',
  PUBLICADO: 'PUBLICADO',
  AGOTADO: 'AGOTADO',
  SUSPENDIDO: 'SUSPENDIDO',
  CANCELADO: 'CANCELADO',
  FINALIZADO: 'FINALIZADO',
}

export const EVENTO_ESTADO_LABELS = {
  [EVENTO_ESTADOS.BORRADOR]: 'Borrador',
  [EVENTO_ESTADOS.PUBLICADO]: 'Publicado',
  [EVENTO_ESTADOS.AGOTADO]: 'Agotado',
  [EVENTO_ESTADOS.SUSPENDIDO]: 'Suspendido',
  [EVENTO_ESTADOS.CANCELADO]: 'Cancelado',
  [EVENTO_ESTADOS.FINALIZADO]: 'Finalizado',
}

/**
 * Sector embebido / tabla Sector (referencia).
 * @typedef {object} Sector
 * @property {string} id
 * @property {string} nombre
 * @property {boolean} esNumerado
 * @property {number} capacidadTotal
 * @property {number} [filas]
 * @property {number} [asientosPorFila]
 * @property {number} [precio]  // PrecioSector simplificado en mock
 */

/**
 * Lugar (referencia entidad/tabla Lugar).
 * @typedef {object} Lugar
 * @property {string} [id]
 * @property {string} nombre
 * @property {string} [direccion]
 * @property {string} ciudad
 * @property {{ lat: number, lng: number } | null} [ubicacion]
 * @property {Sector[]} [sectores]
 */

/**
 * Evento (referencia entidad Evento + joins livianos para UI).
 * @typedef {object} Evento
 * @property {string} id
 * @property {string} nombre
 * @property {string} [descripcion]
 * @property {string} tipo
 * @property {string} idOrganizador
 * @property {string[]} [participantes]
 * @property {string} fechaInicio  ISO
 * @property {string} [fechaHora]  alias BE
 * @property {string} estado
 * @property {boolean} [esAltaDemanda]
 * @property {string} [idLugar]
 * @property {Lugar} lugar
 * @property {string} [imagenUrl]
 * @property {number} [precioDesde]
 * @property {boolean} [destacado]
 * @property {string} [createdAt]
 * @property {string} [updatedAt]
 */

/** Factory: alta mock desde form de gestión. */
export function buildEventoFromForm(form, { idOrganizador, id }) {
  const sectores = normalizeSectores(form.sectores)
  const precioDesde = minPrecioSectores(sectores, form.precioDesde)

  return {
    id: id || `e_${Date.now()}`,
    nombre: String(form.nombre || '').trim(),
    descripcion: String(form.descripcion || '').trim(),
    tipo: String(form.tipo || EVENTO_TIPOS.RECITAL).toUpperCase(),
    idOrganizador: String(idOrganizador || ''),
    participantes: splitList(form.participantes),
    fechaInicio: form.fechaInicio || form.fechaHora || '',
    fechaHora: form.fechaInicio || form.fechaHora || '',
    estado: String(form.estado || EVENTO_ESTADOS.BORRADOR).toUpperCase(),
    esAltaDemanda: Boolean(form.esAltaDemanda),
    idLugar: form.idLugar || undefined,
    lugar: {
      id: form.idLugar || undefined,
      nombre: String(form.lugarNombre || '').trim(),
      direccion: String(form.lugarDireccion || '').trim(),
      ciudad: String(form.lugarCiudad || '').trim(),
      ubicacion: null,
      sectores,
    },
    imagenUrl: String(form.imagenUrl || '').trim() || undefined,
    precioDesde,
    destacado: Boolean(form.destacado),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
}

/** Vista catálogo público (campos livianos que ya usa EventList). */
export function toCatalogoDto(evento) {
  return {
    id: evento.id,
    nombre: evento.nombre,
    tipo: evento.tipo,
    fechaInicio: evento.fechaInicio || evento.fechaHora,
    lugar: {
      nombre: evento.lugar?.nombre,
      ciudad: evento.lugar?.ciudad,
    },
    imagenUrl: evento.imagenUrl,
    precioDesde: evento.precioDesde ?? 0,
    destacado: Boolean(evento.destacado),
    estado: evento.estado,
    idOrganizador: evento.idOrganizador,
    esAltaDemanda: Boolean(evento.esAltaDemanda),
  }
}

/** Detalle para compra: incluye sectores/tipos de entrada del alta. */
export function toEventoDetalleDto(evento) {
  const sectores = normalizeSectores(evento.lugar?.sectores)
  return {
    ...toCatalogoDto(evento),
    descripcion: evento.descripcion || '',
    participantes: evento.participantes || [],
    lugar: {
      id: evento.lugar?.id,
      nombre: evento.lugar?.nombre || '',
      direccion: evento.lugar?.direccion || '',
      ciudad: evento.lugar?.ciudad || '',
      sectores,
    },
    sectores,
  }
}

function splitList(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  return String(value || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

function normalizeSectores(raw) {
  if (!Array.isArray(raw) || raw.length === 0) {
    return [
      {
        id: 'sec_general',
        nombre: 'General',
        esNumerado: false,
        sinCupo: false,
        capacidadTotal: 100,
        precio: 0,
      },
    ]
  }
  return raw.map((s, i) => {
    const sinCupo = Boolean(s.sinCupo || s.capacidadIlimitada)
    const cap = sinCupo ? null : Number(s.capacidadTotal)
    return {
      id: s.id || `sec_${i + 1}`,
      nombre: String(s.nombre || `Sector ${i + 1}`).trim(),
      esNumerado: Boolean(s.esNumerado),
      sinCupo,
      capacidadTotal: sinCupo ? null : Number.isFinite(cap) ? cap : 0,
      filas: s.filas != null ? Number(s.filas) : undefined,
      asientosPorFila:
        s.asientosPorFila != null ? Number(s.asientosPorFila) : undefined,
      precio: Number(s.precio) || 0,
    }
  })
}

function minPrecioSectores(sectores, fallback) {
  const prices = (sectores || [])
    .map((s) => Number(s.precio))
    .filter((n) => !Number.isNaN(n) && n >= 0)
  if (prices.length) return Math.min(...prices)
  const f = Number(fallback)
  return Number.isFinite(f) ? f : 0
}
