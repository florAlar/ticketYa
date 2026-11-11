/**
 * Formas de dominio (referencia a entidades BE msvc-eventos MySQL).
 * El mock JSON del front sigue esta forma; se reacomoda cuando el BE exponga DTOs.
 *
 * msvc-eventos = MySQL (confirmado). Notificaciones = msvc propio futuro (no mockeamos aún).
 */

import {
  MAX_ASIENTOS_POR_FILA,
  MAX_FILAS,
  capacidadDeLayout,
  layoutValido,
  parseEntero,
} from './asiento'

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

/**
 * Fecha de inicio estrictamente posterior a ahora (cliente).
 * Acepta datetime-local o ISO.
 */
export function isFechaInicioFutura(value) {
  if (value == null || String(value).trim() === '') return false
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return false
  return d.getTime() > Date.now()
}

/**
 * Limpia filas de tipos de entrada del form.
 * Nunca cupo ilimitado: sinCupo se ignora; capacidad es número o null si vacío.
 */
export function cleanSectoresFromForm(sectores) {
  if (!Array.isArray(sectores)) return []
  return sectores
    .map((s) => {
      const nombre = String(s?.nombre || '').trim()
      const precioRaw = s?.precio
      const precio =
        precioRaw === '' || precioRaw == null ? null : Number(precioRaw)
      const esNumerado = Boolean(s?.esNumerado)
      // Sector numerado: la capacidad SIEMPRE se deriva de filas * asientosPorFila (se ignora el cupo suelto).
      const filas = esNumerado ? parseEntero(s?.filas) : undefined
      const asientosPorFila = esNumerado ? parseEntero(s?.asientosPorFila) : undefined
      let capacidadTotal
      if (esNumerado) {
        capacidadTotal = capacidadDeLayout(filas, asientosPorFila)
      } else {
        const capRaw = s?.capacidad ?? s?.capacidadTotal
        const cap = capRaw === '' || capRaw == null ? null : Number(capRaw)
        capacidadTotal = Number.isFinite(cap) ? cap : null
      }
      return {
        id: s?.id || undefined,
        nombre,
        precio: Number.isFinite(precio) ? precio : null,
        capacidadTotal,
        esNumerado,
        filas,
        asientosPorFila,
        sinCupo: false,
      }
    })
    .filter((s) => s.nombre)
}

/**
 * Validación form gestión.
 * mode 'borrador': solo nombre; sin límite de fecha; sectores opcionales.
 * mode 'publicacion': completo (fecha, lugar, ≥1 sector con cupo≥1) + artista principal.
 * No se valida “fecha futura” acá (fuera de alcance de evaluación).
 * @returns {{ ok: boolean, error: string, sectores: object[] }}
 */
export function validateEventoGestionForm(form, sectoresRows, mode = 'borrador') {
  const isPub =
    String(mode).toUpperCase() === 'PUBLICACION' ||
    String(mode).toUpperCase() === EVENTO_ESTADOS.PUBLICADO

  const nombre = String(form?.nombre || '').trim()
  if (!nombre) {
    return { ok: false, error: 'El nombre del evento es obligatorio', sectores: [] }
  }

  const principal = String(
    form?.principalId ?? form?.principal?.id ?? ''
  ).trim()
  if (!principal) {
    return {
      ok: false,
      error:
        'Elegí un artista / banda principal del catálogo (o crealo desde el buscador)',
      sectores: [],
    }
  }

  // teloneros: si hay fila, debe tener id
  const telos = Array.isArray(form?.teloneros) ? form.teloneros : []
  for (let i = 0; i < telos.length; i++) {
    const t = telos[i]
    const hasText = String(t?.nombre || '').trim()
    const hasId = String(t?.id || '').trim()
    if (hasText && !hasId) {
      return {
        ok: false,
        error: `Telonero ${i + 1}: elegí del catálogo o crealo (hace falta el id)`,
        sectores: [],
      }
    }
  }

  const fecha = form?.fechaInicio || form?.fechaHora || ''
  const sectores = cleanSectoresFromForm(sectoresRows)

  if (!isPub) {
    // Borrador: sin límite de fecha; sectores a medias OK
    for (const s of sectores) {
      if (s.precio != null && (s.precio < 0 || !Number.isFinite(s.precio))) {
        return {
          ok: false,
          error: `Precio inválido en «${s.nombre}»`,
          sectores,
        }
      }
      // Numerado en borrador: puede quedar sin layout, pero si se cargó algo debe ser válido.
      if (
        s.esNumerado &&
        (s.filas != null || s.asientosPorFila != null) &&
        !layoutValido(s.filas, s.asientosPorFila)
      ) {
        return {
          ok: false,
          error: `Layout inválido en «${s.nombre}»: filas (1 a ${MAX_FILAS}) y asientos por fila (1 a ${MAX_ASIENTOS_POR_FILA}), números enteros`,
          sectores,
        }
      }
      if (
        s.capacidadTotal != null &&
        (!Number.isFinite(s.capacidadTotal) || s.capacidadTotal < 0)
      ) {
        return {
          ok: false,
          error: `Cupo inválido en «${s.nombre}»`,
          sectores,
        }
      }
    }
    return { ok: true, error: '', sectores }
  }

  // Publicación: completo (sin chequeo de “futuro”)
  if (!fecha) {
    return {
      ok: false,
      error: 'Para publicar necesitás fecha y hora de inicio',
      sectores,
    }
  }
  if (!String(form?.lugarNombre || '').trim()) {
    return {
      ok: false,
      error: 'Para publicar necesitás el nombre del lugar',
      sectores,
    }
  }
  if (!String(form?.lugarCiudad || '').trim()) {
    return {
      ok: false,
      error: 'Para publicar necesitás la ciudad del lugar',
      sectores,
    }
  }
  if (!sectores.length) {
    return {
      ok: false,
      error: 'Para publicar agregá al menos un tipo de entrada con nombre',
      sectores,
    }
  }
  for (const s of sectores) {
    if (s.precio == null || !Number.isFinite(s.precio) || s.precio < 0) {
      return {
        ok: false,
        error: `Para publicar, «${s.nombre}» necesita precio ≥ 0`,
        sectores,
      }
    }
    if (s.esNumerado && !layoutValido(s.filas, s.asientosPorFila)) {
      return {
        ok: false,
        error: `Para publicar, el sector numerado «${s.nombre}» necesita filas (1 a ${MAX_FILAS}) y asientos por fila (1 a ${MAX_ASIENTOS_POR_FILA})`,
        sectores,
      }
    }
    if (
      s.capacidadTotal == null ||
      !Number.isFinite(s.capacidadTotal) ||
      s.capacidadTotal < 1
    ) {
      return {
        ok: false,
        error: `Para publicar, «${s.nombre}» necesita cupo ≥ 1 (no hay cupo ilimitado)`,
        sectores,
      }
    }
  }
  return { ok: true, error: '', sectores }
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

function numeroOUndefined(valor) {
  if (valor === null || valor === undefined || valor === '') return undefined
  const n = Number(valor)
  return Number.isFinite(n) ? n : undefined
}

/**
 * Enlace a OpenStreetMap con la ubicación del lugar (LUG-02). Solo si tiene latitud y longitud
 * válidas; si no, null (el lugar puede existir solo con la dirección).
 */
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
  const head = principal?.nombre || (typeof arr[0] === 'string' ? arr[0] : arr[0]?.nombre)
  const telos = arr
    .filter((p) => p !== principal)
    .map((p) => (typeof p === 'string' ? p : p?.nombre))
    .filter(Boolean)
  if (!head) return telos.join(', ')
  if (!telos.length) return head
  return `${head} · teloneros: ${telos.join(', ')}`
}

function normalizeSectores(raw) {
  if (!Array.isArray(raw) || raw.length === 0) {
    return []
  }
  return raw.map((s, i) => {
    // BE expone `capacidad`; form/mock usan capacidadTotal. Nunca cupo ilimitado.
    const capRaw = s.capacidadTotal != null ? s.capacidadTotal : s.capacidad
    const cap = Number(capRaw)
    return {
      id: s.id || `sec_${i + 1}`,
      nombre: String(s.nombre || `Sector ${i + 1}`).trim(),
      esNumerado: Boolean(s.esNumerado),
      sinCupo: false,
      capacidadTotal: Number.isFinite(cap) ? cap : 0,
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

/** Niveles de cupo libre (msvc-ventas NivelCupo). */
export const NIVEL_CUPO = {
  AGOTADO: 'AGOTADO',
  ULTIMOS_LUGARES: 'ULTIMOS_LUGARES',
  MENOS_DE_50: 'MENOS_DE_50',
  MENOS_DE_100: 'MENOS_DE_100',
  OK: 'OK',
}

export const NIVEL_CUPO_LABELS = {
  [NIVEL_CUPO.AGOTADO]: 'Agotado',
  [NIVEL_CUPO.ULTIMOS_LUGARES]: 'Últimos lugares',
  [NIVEL_CUPO.MENOS_DE_50]: 'Menos de 50',
  [NIVEL_CUPO.MENOS_DE_100]: 'Menos de 100',
  [NIVEL_CUPO.OK]: null,
}

/** Umbrales alineados al BE (NivelCupo.java). */
export const CUPO_UMBRAL_ULTIMOS = 20
export const CUPO_UMBRAL_50 = 50
export const CUPO_UMBRAL_100 = 100

/** Clasifica número real de cupos libres → nivel + label. */
export function clasificarNivelCupo(disponibleTotal) {
  const n = Number(disponibleTotal)
  const total = Number.isFinite(n) ? Math.max(0, Math.floor(n)) : 0
  let nivel = NIVEL_CUPO.OK
  if (total < 1) nivel = NIVEL_CUPO.AGOTADO
  else if (total < CUPO_UMBRAL_ULTIMOS) nivel = NIVEL_CUPO.ULTIMOS_LUGARES
  else if (total < CUPO_UMBRAL_50) nivel = NIVEL_CUPO.MENOS_DE_50
  else if (total < CUPO_UMBRAL_100) nivel = NIVEL_CUPO.MENOS_DE_100
  return {
    disponibleTotal: total,
    agotado: nivel === NIVEL_CUPO.AGOTADO,
    nivelCupo: nivel,
    nivelCupoLabel: NIVEL_CUPO_LABELS[nivel],
  }
}

/**
 * Normaliza DTO disponibilidad (1 evento o ítem batch).
 * Respeta números del BE; completa clasificación si faltara.
 */
export function normalizeDisponibilidadDto(raw) {
  if (!raw || typeof raw !== 'object') return null
  const sectores = Array.isArray(raw.sectores)
    ? raw.sectores.map((s) => {
        const disp = Number(s.cantidadDisponible)
        const d = Number.isFinite(disp) ? disp : 0
        const cls = clasificarNivelCupo(d)
        return {
          idSector: s.idSector,
          capacidadTotal: s.capacidadTotal,
          cantidadDisponible: d,
          cantidadReservada: s.cantidadReservada,
          cantidadVendida: s.cantidadVendida,
          // asientos tomados (RESERVADO|VENDIDO) de sectores numerados; el resto del layout está libre
          asientosOcupados: Array.isArray(s.asientosOcupados)
            ? s.asientosOcupados.map((a) => ({
                fila: String(a.fila),
                numero: Number(a.numero),
                estado: a.estado,
              }))
            : [],
          agotado: s.agotado != null ? Boolean(s.agotado) : cls.agotado,
          nivelCupo: s.nivelCupo || cls.nivelCupo,
          nivelCupoLabel:
            s.nivelCupoLabel != null
              ? s.nivelCupoLabel
              : cls.nivelCupoLabel,
        }
      })
    : []
  let total = Number(raw.disponibleTotal)
  if (!Number.isFinite(total)) {
    total = sectores.reduce((acc, s) => acc + (s.cantidadDisponible || 0), 0)
  }
  const cls = clasificarNivelCupo(total)
  return {
    idEvento: raw.idEvento,
    disponibleTotal: cls.disponibleTotal,
    agotado: raw.agotado != null ? Boolean(raw.agotado) : cls.agotado,
    nivelCupo: raw.nivelCupo || cls.nivelCupo,
    nivelCupoLabel:
      raw.nivelCupoLabel != null ? raw.nivelCupoLabel : cls.nivelCupoLabel,
    sectores,
  }
}

/** Adjunta resumen de cupo al evento de catálogo/detalle. */
export function withCupoResumen(evento, disponibilidad) {
  if (!evento) return evento
  const cupo = normalizeDisponibilidadDto(disponibilidad)
  return cupo ? { ...evento, cupo } : { ...evento, cupo: null }
}

export function cupoLabelDeEvento(evento) {
  return evento?.cupo?.nivelCupoLabel || null
}

export function isEventoAgotado(evento) {
  if (evento?.estado === EVENTO_ESTADOS.AGOTADO) return true
  if (evento?.cupo) return Boolean(evento.cupo.agotado)
  return false
}

