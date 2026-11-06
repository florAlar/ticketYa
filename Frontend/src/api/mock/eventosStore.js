/**
 * Persistencia mock de eventos de gestión (JSON en localStorage).
 * Simula lo que luego será ms-eventos (MySQL) vía gateway.
 */
import { MOCK_EVENTOS } from './eventos'
import { EVENTO_ESTADOS, toCatalogoDto } from '../../domain/evento'

const STORE_KEY = 'ticketya_mock_eventos_gestion'

function seedFromCatalog() {
  // Catálogo seed como eventos “de sistema” (sin org dueño → solo admin los ve todos)
  const base = MOCK_EVENTOS.map((e) => ({
    ...e,
    descripcion: e.descripcion || '',
    idOrganizador: e.idOrganizador || 'u_system',
    participantes: e.participantes || [],
    fechaHora: e.fechaInicio,
    esAltaDemanda: Boolean(e.esAltaDemanda),
    lugar: {
      nombre: e.lugar?.nombre || '',
      ciudad: e.lugar?.ciudad || '',
      direccion: e.lugar?.direccion || '',
      ubicacion: null,
      sectores: e.lugar?.sectores || [
        {
          id: 'sec_gen',
          nombre: 'General',
          esNumerado: false,
          capacidadTotal: 200,
          precio: e.precioDesde ?? 0,
        },
      ],
    },
    createdAt: e.createdAt || '2026-01-01T00:00:00.000Z',
    updatedAt: e.updatedAt || '2026-01-01T00:00:00.000Z',
  }))

  // Evento cancelado de demo: el panel ventas lista sus entradas (no todo el catálogo de compras)
  base.push({
    id: 'e_cancel_demo',
    nombre: 'Festival cancelado (demo admin)',
    descripcion: 'Evento de prueba en estado CANCELADO para cola de reintegros.',
    tipo: 'FESTIVAL',
    idOrganizador: 'u_system',
    participantes: [],
    fechaInicio: '2026-12-01T20:00:00',
    fechaHora: '2026-12-01T20:00:00',
    estado: EVENTO_ESTADOS.CANCELADO,
    esAltaDemanda: false,
    lugar: {
      nombre: 'Predio Municipal',
      ciudad: 'Tandil',
      direccion: '',
      ubicacion: null,
      sectores: [
        {
          id: 'sec_campo',
          nombre: 'Campo',
          esNumerado: false,
          capacidadTotal: 500,
          precio: 15000,
        },
      ],
    },
    imagenUrl: undefined,
    precioDesde: 15000,
    destacado: false,
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-10-01T00:00:00.000Z',
  })

  return base
}

/** Garantiza el evento cancelado demo aunque el LS ya existía. */
function ensureCancelDemo(list) {
  if (list.some((e) => e.id === 'e_cancel_demo')) return list
  const next = [...list, ...seedFromCatalog().filter((e) => e.id === 'e_cancel_demo')]
  writeGestionEventos(next)
  return next
}

export function readGestionEventos() {
  try {
    const raw = localStorage.getItem(STORE_KEY)
    if (!raw) {
      const seed = seedFromCatalog()
      writeGestionEventos(seed)
      return seed
    }
    const parsed = JSON.parse(raw)
    const list = Array.isArray(parsed) ? parsed : seedFromCatalog()
    return ensureCancelDemo(list)
  } catch {
    return seedFromCatalog()
  }
}

export function listEventosCancelados() {
  return readGestionEventos().filter((e) => e.estado === EVENTO_ESTADOS.CANCELADO)
}

export function writeGestionEventos(list) {
  localStorage.setItem(STORE_KEY, JSON.stringify(list))
}

export function insertGestionEvento(evento) {
  const list = readGestionEventos()
  list.unshift(evento)
  writeGestionEventos(list)
  return evento
}

export function updateGestionEvento(id, patch) {
  const list = readGestionEventos()
  const idx = list.findIndex((e) => e.id === String(id))
  if (idx === -1) return null
  const next = {
    ...list[idx],
    ...patch,
    id: list[idx].id,
    updatedAt: new Date().toISOString(),
  }
  list[idx] = next
  writeGestionEventos(list)
  return next
}

/** Catálogo público: solo PUBLICADO (+ AGOTADO visible). */
export function listCatalogoPublico() {
  return readGestionEventos()
    .filter((e) =>
      [EVENTO_ESTADOS.PUBLICADO, EVENTO_ESTADOS.AGOTADO].includes(e.estado)
    )
    .map(toCatalogoDto)
}

/** Listado gestión filtrado por rol. */
export function listGestionForUser(user, { isAdmin } = {}) {
  const all = readGestionEventos()
  if (isAdmin) return all
  const oid = user?.id
  return all.filter((e) => e.idOrganizador === oid)
}
