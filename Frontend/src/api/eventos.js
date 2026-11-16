import { http } from './http'
import { USE_MOCK } from './config'
import {
  insertGestionEvento,
  listCatalogoPublico,
  listGestionForUser,
  readGestionEventos,
  updateGestionEvento,
} from './mock/eventosStore'
import { buildEventoFromForm, EVENTO_ESTADOS, toEventoDetalleDto } from '../domain/evento'

const delay = (ms = 280) => new Promise((r) => setTimeout(r, ms))

function applyFilter(items, filter = {}) {
  let list = [...items]

  if (filter.destacados === true || filter.destacado === true) {
    list = list.filter((e) => e.destacado)
  }
  if (filter.tipo) {
    list = list.filter(
      (e) => String(e.tipo).toUpperCase() === String(filter.tipo).toUpperCase()
    )
  }
  if (filter.ciudad) {
    const c = String(filter.ciudad).toLowerCase()
    list = list.filter((e) => e.lugar?.ciudad?.toLowerCase().includes(c))
  }
  if (filter.organizador) {
    const o = String(filter.organizador).toLowerCase()
    list = list.filter(
      (e) =>
        String(e.idOrganizador || '')
          .toLowerCase()
          .includes(o) ||
        String(e.organizadorNombre || '')
          .toLowerCase()
          .includes(o)
    )
  }
  if (filter.q) {
    const q = String(filter.q).toLowerCase()
    list = list.filter(
      (e) =>
        e.nombre?.toLowerCase().includes(q) ||
        e.descripcion?.toLowerCase().includes(q) ||
        e.lugar?.nombre?.toLowerCase().includes(q) ||
        e.lugar?.ciudad?.toLowerCase().includes(q)
    )
  }
  if (filter.fechaDesde) {
    const from = new Date(filter.fechaDesde)
    if (!Number.isNaN(from.getTime())) {
      list = list.filter((e) => new Date(e.fechaInicio) >= from)
    }
  }
  if (filter.fechaHasta) {
    const to = new Date(filter.fechaHasta)
    if (!Number.isNaN(to.getTime())) {
      to.setHours(23, 59, 59, 999)
      list = list.filter((e) => new Date(e.fechaInicio) <= to)
    }
  }
  if (filter.esAltaDemanda === true || filter.esAltaDemanda === 'true') {
    list = list.filter((e) => e.esAltaDemanda)
  }

  const orden = filter.orden || filter.sort
  if (orden === 'precio') {
    list.sort((a, b) => (a.precioDesde ?? 0) - (b.precioDesde ?? 0))
  } else {
    list.sort((a, b) => new Date(a.fechaInicio) - new Date(b.fechaInicio))
  }

  const limit = Number(filter.limit || filter.size)
  if (limit > 0) list = list.slice(0, limit)

  return list
}

function toQuery(filter = {}) {
  const params = new URLSearchParams()
  const map = { ...filter }
  if (map.sort && !map.orden) {
    map.orden = map.sort === 'precio' ? 'precio' : 'fecha'
    delete map.sort
  }
  if (map.limit && !map.size) {
    map.size = map.limit
    delete map.limit
  }
  Object.entries(map).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    if (typeof v === 'boolean') {
      params.set(k, v ? 'true' : 'false')
      return
    }
    params.set(k, String(v))
  })
  const s = params.toString()
  return s ? `?${s}` : ''
}

/** Normaliza página BE `{ content }` o array mock. */
function unwrapEventosList(data) {
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.content)) return data.content
  return []
}

/**
 * Catálogo público.
 * GET /api/eventos?tipo&ciudad&organizador&estado&fechaDesde&fechaHasta&orden&page&size
 * Default BE: PUBLICADO+AGOTADO. `incluirSuspendidos` → estado=TODOS + FE deja PUBLICADO|SUSPENDIDO|AGOTADO.
 * `q` y `esAltaDemanda` se aplican en cliente (BE listar aún no los expone).
 */
export async function listEventos(filter = {}) {
  if (USE_MOCK) {
    await delay()
    let list = applyFilter(listCatalogoPublico(), filter)
    if (filter.incluirSuspendidos === true || filter.incluirSuspendidos === 'true') {
      list = list.filter((e) => {
        const est = String(e.estado || '').toUpperCase()
        return est === 'PUBLICADO' || est === 'SUSPENDIDO' || est === 'AGOTADO'
      })
    }
    return list
  }
  const serverFilter = { ...filter }
  const q = serverFilter.q
  const alta = serverFilter.esAltaDemanda
  const incluirSuspendidos =
    serverFilter.incluirSuspendidos === true ||
    serverFilter.incluirSuspendidos === 'true'
  delete serverFilter.q
  delete serverFilter.esAltaDemanda
  delete serverFilter.incluirSuspendidos
  delete serverFilter.destacados
  delete serverFilter.destacado
  if (incluirSuspendidos && !serverFilter.estado) {
    serverFilter.estado = 'TODOS'
  }
  const data = await http(`/api/eventos${toQuery(serverFilter)}`)
  let list = unwrapEventosList(data)
  // post-filtro FE para campos no cableados en listar BE
  if (q || alta === true || alta === 'true') {
    list = applyFilter(list, { q, esAltaDemanda: alta })
  }
  // Catálogo comprable ampliado: no filtrar borradores/cancelados del TODOS admin
  if (incluirSuspendidos) {
    list = list.filter((e) => {
      const est = String(e.estado || '').toUpperCase()
      return est === 'PUBLICADO' || est === 'SUSPENDIDO' || est === 'AGOTADO'
    })
  }
  return list
}

/** GET /api/eventos/{id} */
export async function getEvento(id, opts = {}) {
  if (USE_MOCK) {
    await delay()
    const found = readGestionEventos().find((e) => e.id === String(id))
    if (!found) {
      const err = new Error('Evento no encontrado')
      err.status = 404
      throw err
    }
    return toEventoDetalleDto(found)
  }
  return http(`/api/eventos/${id}`, { token: opts.token })
}

/**
 * Listado panel gestión.
 * ORGANIZADOR: GET /api/organizadores/me/eventos
 * ADMIN: GET /api/eventos
 */
export async function listEventosGestion(user, { isAdmin, token } = {}) {
  if (USE_MOCK) {
    await delay()
    if (!user) {
      const err = new Error('No autenticado')
      err.status = 401
      throw err
    }
    return listGestionForUser(user, { isAdmin })
  }
  if (isAdmin) {
    const data = await http(`/api/eventos${toQuery({ size: 200, estado: 'TODOS' })}`, { token })
    return unwrapEventosList(data)
  }
  return http('/api/organizadores/me/eventos', { token })
}

/** POST /api/eventos — crear borrador (ORGANIZADOR) */
export async function createEvento(form, user, opts = {}) {
  if (USE_MOCK) {
    await delay(400)
    if (!user?.id) {
      const err = new Error('No autenticado')
      err.status = 401
      throw err
    }
    const evento = buildEventoFromForm(form, { idOrganizador: user.id })
    if (!evento.nombre) {
      const err = new Error('El nombre es obligatorio')
      err.status = 400
      throw err
    }
    if (!evento.fechaInicio) {
      const err = new Error('La fecha es obligatoria')
      err.status = 400
      throw err
    }
    return insertGestionEvento(evento)
  }
  return http('/api/eventos', {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify(form),
  })
}

/**
 * Cambiar estado / publicar.
 * PUBLICADO → POST /api/eventos/{id}/publicacion
 * resto → PATCH /api/eventos/{id}/estado
 */
export async function patchEventoEstado(id, estado, user, { isAdmin, token } = {}) {
  const nextEstado = String(estado || '').toUpperCase()

  if (USE_MOCK) {
    await delay()
    const list = readGestionEventos()
    const cur = list.find((e) => e.id === String(id))
    if (!cur) {
      const err = new Error('Evento no encontrado')
      err.status = 404
      throw err
    }
    if (!isAdmin && cur.idOrganizador !== user?.id) {
      const err = new Error('Sin permiso sobre este evento')
      err.status = 403
      throw err
    }
    if (!Object.values(EVENTO_ESTADOS).includes(nextEstado)) {
      const err = new Error('Estado inválido')
      err.status = 400
      throw err
    }
    return updateGestionEvento(id, { estado: nextEstado })
  }

  if (nextEstado === EVENTO_ESTADOS.PUBLICADO || nextEstado === 'PUBLICADO') {
    return http(`/api/eventos/${id}/publicacion`, {
      method: 'POST',
      token,
    })
  }

  return http(`/api/eventos/${id}/estado`, {
    method: 'PATCH',
    token,
    body: JSON.stringify({ estado: nextEstado }),
  })
}

/** PUT /api/eventos/{id} */
export async function updateEvento(id, body, opts = {}) {
  if (USE_MOCK) {
    await delay()
    const built = buildEventoFromForm(body, {
      idOrganizador: opts.userId || body.idOrganizador,
      id: String(id),
    })
    const updated = updateGestionEvento(id, built)
    if (!updated) {
      const err = new Error('Evento no encontrado')
      err.status = 404
      throw err
    }
    return updated
  }
  return http(`/api/eventos/${id}`, {
    method: 'PUT',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** DELETE /api/eventos/{id} */
export async function deleteEvento(id, opts = {}) {
  if (USE_MOCK) {
    const err = new Error('deleteEvento mock no implementado')
    err.status = 501
    throw err
  }
  return http(`/api/eventos/${id}`, {
    method: 'DELETE',
    token: opts.token,
  })
}

/** GET /api/participantes?q&tipo */
export async function listParticipantes(filter = {}, opts = {}) {
  if (USE_MOCK) {
    await delay(80)
    return readMockParticipantes(filter)
  }
  const data = await http(`/api/participantes${toQuery(filter)}`, {
    token: opts.token,
  })
  return Array.isArray(data) ? data : []
}

/** POST /api/participantes */
export async function createParticipante(body, opts = {}) {
  if (USE_MOCK) {
    await delay(80)
    return insertMockParticipante(body)
  }
  return http('/api/participantes', {
    method: 'POST',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** PUT /api/eventos/{id}/participantes — reemplaza set */
export async function setEventoParticipantes(eventoId, body, opts = {}) {
  if (USE_MOCK) {
    await delay(80)
    const items = body?.participantes || (body?.ids || []).map((id) => ({ id }))
    const catalog = readMockParticipantes({})
    const byId = new Map(catalog.map((p) => [String(p.id), p]))
    const updated = updateGestionEvento(eventoId, {
      participantes: items.map((it, i) => {
        const cat = it.id ? byId.get(String(it.id)) : null
        return {
          id: it.id,
          nombre: it.nombre || cat?.nombre || it.id,
          rol: it.rol || (i === 0 ? 'PRINCIPAL' : 'TELONERO'),
          orden: it.orden != null ? it.orden : i,
        }
      }),
    })
    return updated?.participantes || items
  }
  return http(`/api/eventos/${eventoId}/participantes`, {
    method: 'PUT',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/**
 * Asocia artistas ya elegidos (por id) al evento.
 * @param {{ principalId: string, principalNombre?: string, teloneros?: {id:string,nombre?:string}[] }} formPart
 */
export async function syncEventoParticipantes(eventoId, formPart, opts = {}) {
  const principalId = String(formPart?.principalId || '').trim()
  if (!principalId) {
    const err = new Error(
      'Elegí un artista principal del catálogo (o crealo desde el buscador)'
    )
    err.status = 400
    throw err
  }

  const items = [
    {
      id: principalId,
      nombre: formPart?.principalNombre || undefined,
      rol: 'PRINCIPAL',
      orden: 0,
    },
  ]
  let orden = 1
  const seen = new Set([principalId])
  for (const t of formPart?.teloneros || []) {
    const id = String(t?.id || '').trim()
    if (!id || seen.has(id)) continue
    seen.add(id)
    items.push({
      id,
      nombre: t?.nombre || undefined,
      rol: 'TELONERO',
      orden: orden++,
    })
  }

  return setEventoParticipantes(eventoId, { participantes: items }, opts)
}

const MOCK_PART_KEY = 'ticketya_mock_participantes'

function readMockParticipantes(filter = {}) {
  let list = []
  try {
    const raw = localStorage.getItem(MOCK_PART_KEY)
    list = raw ? JSON.parse(raw) : []
    if (!Array.isArray(list)) list = []
  } catch {
    list = []
  }
  if (!list.length) {
    list = [
      { id: 'p0000000-0000-4000-8000-000000000001', nombre: 'Metallica', tipo: 'BANDA', descripcion: 'Thrash metal — M72' },
      { id: 'p0000000-0000-4000-8000-000000000002', nombre: 'Gojira', tipo: 'BANDA', descripcion: 'Soporte M72' },
      { id: 'p0000000-0000-4000-8000-000000000003', nombre: 'Real Madrid CF', tipo: 'EQUIPO', descripcion: 'Finalista Champions' },
      { id: 'p0000000-0000-4000-8000-000000000004', nombre: 'Manchester City FC', tipo: 'EQUIPO', descripcion: 'Finalista Champions' },
      { id: 'p0000000-0000-4000-8000-000000000005', nombre: 'Iron Maiden', tipo: 'BANDA', descripcion: 'The Future Past / festivals' },
      { id: 'p0000000-0000-4000-8000-000000000006', nombre: 'Ghost', tipo: 'BANDA', descripcion: 'Soporte / co-bill' },
      { id: 'p0000000-0000-4000-8000-000000000007', nombre: 'Rammstein', tipo: 'BANDA', descripcion: 'Industrial metal stadium' },
      { id: 'p0000000-0000-4000-8000-000000000008', nombre: 'Foo Fighters', tipo: 'BANDA', descripcion: 'Alternative rock' },
      { id: 'p0000000-0000-4000-8000-000000000009', nombre: 'Coldplay', tipo: 'BANDA', descripcion: 'Pop rock stadium' },
      { id: 'p0000000-0000-4000-8000-00000000000a', nombre: 'Los Angeles Lakers', tipo: 'EQUIPO', descripcion: 'NBA' },
      { id: 'p0000000-0000-4000-8000-00000000000b', nombre: 'Boston Celtics', tipo: 'EQUIPO', descripcion: 'NBA' },
      { id: 'p0000000-0000-4000-8000-00000000000c', nombre: 'Slipknot', tipo: 'BANDA', descripcion: 'Nu metal' },
      { id: 'p0000000-0000-4000-8000-00000000000d', nombre: 'Bring Me The Horizon', tipo: 'BANDA', descripcion: 'Download co-headliner' },
      { id: 'p0000000-0000-4000-8000-00000000000e', nombre: 'Oasis', tipo: 'BANDA', descripcion: 'Britpop reunion' },
      { id: 'p0000000-0000-4000-8000-00000000000f', nombre: 'Tool', tipo: 'BANDA', descripcion: 'Progressive metal' },
      { id: 'p0000000-0000-4000-8000-000000000010', nombre: 'Selección Brasil', tipo: 'EQUIPO', descripcion: 'FIFA' },
      { id: 'p0000000-0000-4000-8000-000000000011', nombre: 'Selección Argentina', tipo: 'EQUIPO', descripcion: 'FIFA' },
      { id: 'p0000000-0000-4000-8000-000000000012', nombre: 'Max Verstappen', tipo: 'PILOTO', descripcion: 'F1 Red Bull' },
    ]
    try {
      localStorage.setItem(MOCK_PART_KEY, JSON.stringify(list))
    } catch {
      /* ignore */
    }
  }
  const q = String(filter.q || '').trim().toLowerCase()
  const tipo = String(filter.tipo || '').trim().toLowerCase()
  return list
    .filter((p) => {
      if (q && !String(p.nombre || '').toLowerCase().includes(q)) return false
      if (tipo && String(p.tipo || '').toLowerCase() !== tipo) return false
      return true
    })
    .sort((a, b) => String(a.nombre).localeCompare(String(b.nombre), 'es'))
}

function insertMockParticipante(body) {
  const nombre = String(body?.nombre || '').trim()
  if (!nombre) {
    const err = new Error('nombre es obligatorio')
    err.status = 400
    throw err
  }
  const list = readMockParticipantes({})
  const exact = list.find(
    (p) => String(p.nombre || '').toLowerCase() === nombre.toLowerCase()
  )
  if (exact) return exact
  const created = {
    id: `p_mock_${Date.now()}`,
    nombre,
    tipo: body?.tipo || 'BANDA',
    descripcion: body?.descripcion || null,
  }
  const next = [...list, created]
  try {
    localStorage.setItem(MOCK_PART_KEY, JSON.stringify(next))
  } catch {
    /* ignore */
  }
  return created
}

/** GET /api/eventos/{id}/disponibilidad — msvc-ventas */
export async function getDisponibilidadEvento(id, opts = {}) {
  if (USE_MOCK) {
    await delay(120)
    return mockDisponibilidadItem(id)
  }
  return http(`/api/eventos/${id}/disponibilidad`, { token: opts.token })
}

/**
 * GET /api/eventos/disponibilidad?ids=a&ids=b — batch catálogo (1 request).
 * @returns {{ items: object[] }}
 */
export async function getDisponibilidadBatch(ids, opts = {}) {
  const unique = [
    ...new Set(
      (Array.isArray(ids) ? ids : [])
        .map((id) => String(id || '').trim())
        .filter(Boolean)
    ),
  ].slice(0, 50)

  if (!unique.length) {
    return { items: [] }
  }

  if (USE_MOCK) {
    await delay(150)
    return { items: unique.map((id) => mockDisponibilidadItem(id)) }
  }

  const qs = unique
    .map((id) => `ids=${encodeURIComponent(id)}`)
    .join('&')
  return http(`/api/eventos/disponibilidad?${qs}`, {
    method: 'GET',
    token: opts.token,
  })
}

/** Mock: número real + clasificación (misma forma que BE). */
function mockDisponibilidadItem(idEvento) {
  const seed = [...String(idEvento)].reduce(
    (acc, ch) => acc + ch.charCodeAt(0),
    0
  )
  // 0..149 → cubre agotado / umbrales / ok
  const disponibleTotal = seed % 150
  const sectores = [
    {
      idSector: 'mock_sec',
      capacidadTotal: Math.max(disponibleTotal, 1),
      cantidadDisponible: disponibleTotal,
      cantidadReservada: 0,
      cantidadVendida: 0,
    },
  ]
  return {
    idEvento,
    disponibleTotal,
    sectores,
    mock: true,
  }
}
