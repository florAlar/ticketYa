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
  if (filter.q) {
    const q = String(filter.q).toLowerCase()
    list = list.filter(
      (e) =>
        e.nombre.toLowerCase().includes(q) ||
        e.lugar?.nombre?.toLowerCase().includes(q) ||
        e.lugar?.ciudad?.toLowerCase().includes(q)
    )
  }

  // back: orden=fecha|precio|relevancia — mock usa sort/orden
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
  // alias front → query back
  if (map.sort && !map.orden) {
    map.orden = map.sort === 'precio' ? 'precio' : 'fecha'
    delete map.sort
  }
  if (map.limit && !map.size) {
    map.size = map.limit
    delete map.limit
  }
  if (map.destacados !== undefined && map.destacado === undefined) {
    // back no documenta destacados; se envía igual si el gateway lo acepta
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

/**
 * Catálogo público.
 * GET /api/eventos?tipo&ciudad&fechaDesde&fechaHasta&orden&page&size&...
 */
export async function listEventos(filter = {}) {
  if (USE_MOCK) {
    await delay()
    return applyFilter(listCatalogoPublico(), filter)
  }
  return http(`/api/eventos${toQuery(filter)}`)
}

/** GET /api/eventos/{id} */
export async function getEvento(id) {
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
  return http(`/api/eventos/${id}`)
}

/**
 * Listado panel gestión.
 * ORGANIZADOR: GET /api/organizadores/me/eventos
 * ADMIN: GET /api/eventos (catálogo amplio; alcance admin lo define el BE)
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
    return http(`/api/eventos${toQuery({ size: 200 })}`, { token })
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
 * SUSPENDIDO|CANCELADO|… → PATCH /api/eventos/{id}/estado
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

/** PUT /api/eventos/{id} — actualizar config (ORGANIZADOR) */
export async function updateEvento(id, body, opts = {}) {
  if (USE_MOCK) {
    await delay()
    return updateGestionEvento(id, body)
  }
  return http(`/api/eventos/${id}`, {
    method: 'PUT',
    token: opts.token,
    body: JSON.stringify(body),
  })
}

/** DELETE /api/eventos/{id} — eliminar borrador */
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

/** GET /api/eventos/{id}/disponibilidad — msvc-ventas (gateway) */
export async function getDisponibilidadEvento(id, opts = {}) {
  if (USE_MOCK) {
    await delay(120)
    return { idEvento: id, mock: true, mensaje: 'Disponibilidad mock local' }
  }
  return http(`/api/eventos/${id}/disponibilidad`, { token: opts.token })
}
