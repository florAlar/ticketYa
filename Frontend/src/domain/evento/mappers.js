import { EVENTO_ESTADOS, EVENTO_TIPOS } from './constants'
import { minPrecioSectores, normalizeSectores } from './sectores'

/** Fecha de inicio estrictamente posterior a ahora. Acepta datetime-local o ISO. */
export function isFechaInicioFutura(value) {
  if (value == null || String(value).trim() === '') return false
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return false
  return d.getTime() > Date.now()
}

function numeroOUndefined(valor) {
  if (valor === null || valor === undefined || valor === '') return undefined
  const n = Number(valor)
  return Number.isFinite(n) ? n : undefined
}

/** Enlace a OpenStreetMap con la ubicación del lugar (LUG-02). */
export function urlMapaLugar(lugar) {
  const lat = numeroOUndefined(lugar?.latitud)
  const lng = numeroOUndefined(lugar?.longitud)
  if (lat === undefined || lng === undefined) return null
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null
  return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=16/${lat}/${lng}`
}

function splitList(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  return String(value || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

/** Principal + teloneros → lista con rol para mock/DTO. */
export function normalizeParticipantesForm(form) {
  if (Array.isArray(form?.participantes) && form.participantes.length) {
    const first = form.participantes[0]
    if (first && typeof first === 'object' && (first.nombre || first.id)) {
      return form.participantes
        .map((p, i) => ({
          id: p.id || null,
          nombre: String(p.nombre || '').trim(),
          rol: String(p.rol || (i === 0 ? 'PRINCIPAL' : 'TELONERO')).toUpperCase(),
          orden: p.orden != null ? p.orden : i,
        }))
        .filter((p) => p.id || p.nombre)
    }
    return splitList(form.participantes).map((nombre, i) => ({
      nombre,
      rol: i === 0 ? 'PRINCIPAL' : 'TELONERO',
      orden: i,
    }))
  }
  const principalId = form?.principalId || form?.principal?.id || null
  const principalNombre = String(
    form?.principalNombre || form?.principal?.nombre || ''
  ).trim()
  const teloneros = Array.isArray(form?.teloneros) ? form.teloneros : []
  const out = []
  if (principalId || principalNombre) {
    out.push({
      id: principalId,
      nombre: principalNombre,
      rol: 'PRINCIPAL',
      orden: 0,
    })
  }
  let i = 1
  for (const t of teloneros) {
    const id = t?.id || null
    const nombre = String(t?.nombre || t || '').trim()
    if (!id && !nombre) continue
    out.push({
      id,
      nombre,
      rol: 'TELONERO',
      orden: i++,
    })
  }
  return out
}

/** Labels legibles para detalle. */
export function formatParticipantesLine(list) {
  const arr = Array.isArray(list) ? list : []
  if (!arr.length) return ''
  const principal = arr.find(
    (p) => String(p?.rol || '').toUpperCase() === 'PRINCIPAL'
  )
  const head =
    principal?.nombre || (typeof arr[0] === 'string' ? arr[0] : arr[0]?.nombre)
  const telos = arr
    .filter((p) => p !== principal)
    .map((p) => (typeof p === 'string' ? p : p?.nombre))
    .filter(Boolean)
  if (!head) return telos.join(', ')
  if (!telos.length) return head
  return `${head} · teloneros: ${telos.join(', ')}`
}

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
    participantes: normalizeParticipantesForm(form),
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
      latitud: numeroOUndefined(evento.lugar?.latitud),
      longitud: numeroOUndefined(evento.lugar?.longitud),
      sectores,
    },
    sectores,
  }
}
