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
 * Lugar reservado del backend para los borradores que todavía no tienen lugar elegido.
 * No se muestra ni se elige (el backend lo oculta del listado de lugares).
 */
export const LUGAR_POR_DEFINIR_ID = 'c0000000-0000-4000-8000-0000000000ff'

/**
 * Filas de la tabla de sectores de un evento (LUG-04). Los sectores los define el lugar: el
 * organizador solo elige cuáles se venden en el evento y les fija precio y cupo.
 * @param {{ sectores?: object[] }} lugar lugar con sus sectores
 * @param {object[]} [delEvento] sectores ya elegidos en el evento (detalle) para precargar al editar
 */
export function rowsDeLugar(lugar, delEvento = []) {
  const sectores = Array.isArray(lugar?.sectores) ? lugar.sectores : []
  return sectores.map((s) => {
    const previo = (Array.isArray(delEvento) ? delEvento : []).find((x) => x.id === s.id)
    const capacidad = Number(s.capacidad) || 0
    const cupoPrevio = previo ? Number(previo.capacidad ?? previo.capacidadTotal) : 0
    return {
      id: s.id,
      nombre: s.nombre || '',
      tipo: s.tipo || '',
      capacidad,
      esNumerado: Boolean(s.esNumerado),
      filas: s.filas,
      asientosPorFila: s.asientosPorFila,
      incluido: Boolean(previo),
      precio: previo && previo.precio != null ? String(previo.precio) : '',
      // Un cupo menor a la capacidad es propio del evento; igual (o vacío) = se vende todo el sector.
      cupo:
        previo && !s.esNumerado && cupoPrevio > 0 && cupoPrevio < capacidad
          ? String(cupoPrevio)
          : '',
    }
  })
}

/** '' / null → null; otro valor → Number (NaN si es basura). Acepta coma decimal. */
function numeroOVacio(valor) {
  const s = String(valor ?? '').trim().replace(',', '.')
  return s === '' ? null : Number(s)
}

/**
 * Sectores elegidos del form, listos para el pedido: { id, precio, capacidadTotal? }.
 * `capacidadTotal` es el cupo del evento y solo va si se cargó (vacío = todo el sector; en sectores
 * numerados nunca va: se venden completos).
 */
export function cleanSectoresFromForm(rows) {
  if (!Array.isArray(rows)) return []
  return rows
    .filter((r) => r?.incluido)
    .map((r) => {
      const precio = numeroOVacio(r.precio)
      const cupo = r.esNumerado ? null : numeroOVacio(r.cupo)
      const out = { id: r.id, precio: Number.isFinite(precio) ? precio : null }
      if (cupo !== null) out.capacidadTotal = cupo
      return out
    })
}

/** Mensaje de error de los sectores elegidos ('' si están bien). `estricto` = al publicar. */
function errorEnSectoresElegidos(rows, estricto) {
  for (const r of (Array.isArray(rows) ? rows : []).filter((x) => x?.incluido)) {
    const precio = numeroOVacio(r.precio)
    if (precio === null) {
      if (estricto) return `Para publicar, «${r.nombre}» necesita precio ≥ 0`
    } else if (!Number.isFinite(precio) || precio < 0) {
      return `Precio inválido en «${r.nombre}»`
    }
    if (estricto && !(r.capacidad >= 1)) {
      return `El sector «${r.nombre}» no tiene capacidad: el administrador tiene que definirla en el lugar`
    }
    if (!r.esNumerado) {
      const cupo = numeroOVacio(r.cupo)
      if (cupo !== null && (!Number.isInteger(cupo) || cupo < 1 || cupo > r.capacidad)) {
        return `El cupo de «${r.nombre}» debe ser un entero entre 1 y ${r.capacidad} (capacidad del sector)`
      }
    }
  }
  return ''
}

/**
 * Validación form gestión (LUG-04: el evento elige un lugar y sus sectores).
 * mode 'borrador': solo nombre y artista principal; lugar y sectores opcionales.
 * mode 'publicacion': completo (fecha, lugar, ≥1 sector elegido con precio) + artista principal.
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
  const idLugar = String(form?.idLugar || '').trim()
  const sectores = cleanSectoresFromForm(sectoresRows)

  if (sectores.length && !idLugar) {
    return { ok: false, error: 'Elegí un lugar antes de seleccionar sectores', sectores }
  }
  const errorSectores = errorEnSectoresElegidos(sectoresRows, isPub)
  if (errorSectores) {
    return { ok: false, error: errorSectores, sectores }
  }

  if (!isPub) {
    // Borrador: sin límite de fecha; lugar y sectores pueden quedar a medias
    return { ok: true, error: '', sectores }
  }

  // Publicación: completo (sin chequeo de “futuro”)
  if (!fecha) {
    return { ok: false, error: 'Para publicar necesitás fecha y hora de inicio', sectores }
  }
  if (!idLugar) {
    return { ok: false, error: 'Para publicar elegí un lugar', sectores }
  }
  if (!sectores.length) {
    return {
      ok: false,
      error: 'Para publicar elegí al menos un sector del lugar con su precio',
      sectores,
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

