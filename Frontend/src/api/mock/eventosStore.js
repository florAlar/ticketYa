/**
 * Persistencia mock de eventos de gestión (JSON en localStorage).
 * Simula lo que luego será ms-eventos (MySQL) vía gateway.
 */
import { MOCK_EVENTOS } from './eventos'
import { EVENTO_ESTADOS, toCatalogoDto } from '../../domain/evento'

const STORE_KEY = 'ticketya_mock_eventos_gestion'

function seedFromCatalog() {
  return MOCK_EVENTOS.map((e) => ({
    ...e,
    descripcion: e.descripcion || '',
    idOrganizador: e.idOrganizador || 'b0000000-0000-4000-8000-000000000002',
    participantes: e.participantes || [],
    fechaHora: e.fechaInicio,
    esAltaDemanda: Boolean(e.esAltaDemanda),
    lugar: {
      id: 'c0000000-0000-4000-8000-000000000001',
      nombre: e.lugar?.nombre || 'Estadio Más Monumental',
      ciudad: e.lugar?.ciudad || 'Buenos Aires',
      direccion: e.lugar?.direccion || 'Av. Figueroa Alcorta 7597',
      ubicacion: null,
      sectores: e.lugar?.sectores || [
        {
          id: 'c0000000-0000-4000-8000-000000000011',
          nombre: 'Campo',
          esNumerado: false,
          capacidadTotal: 60000,
          precio: e.precioDesde ?? 0,
        },
        {
          id: 'c0000000-0000-4000-8000-000000000012',
          nombre: 'Platea',
          esNumerado: true,
          // capacidad = filas × asientosPorFila (100 × 250)
          filas: 100,
          asientosPorFila: 250,
          capacidadTotal: 25000,
          precio: Math.round((e.precioDesde ?? 0) * 1.8),
        },
      ],
    },
    createdAt: e.createdAt || '2026-01-01T00:00:00.000Z',
    updatedAt: e.updatedAt || '2026-01-01T00:00:00.000Z',
  }))
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
    return Array.isArray(parsed) ? parsed : seedFromCatalog()
  } catch {
    return seedFromCatalog()
  }
}

/** Fuerza reseed del catálogo mock (3 eventos canónicos). */
export function resetGestionEventosSeed() {
  const seed = seedFromCatalog()
  writeGestionEventos(seed)
  return seed
}

export function listEventosCancelados() {
  return readGestionEventos().filter((e) => e.estado === EVENTO_ESTADOS.CANCELADO)
}

export function writeGestionEventos(list) {
  localStorage.setItem(STORE_KEY, JSON.stringify(list))
}

export function listCatalogoPublico() {
  return readGestionEventos()
    .filter((e) => {
      const est = String(e.estado || '').toUpperCase()
      // Alineado a BE default catálogo: PUBLICADO + AGOTADO
      return est === 'PUBLICADO' || est === 'AGOTADO'
    })
    .map(toCatalogoDto)
}

export function listGestionForUser(user, { isAdmin } = {}) {
  const all = readGestionEventos()
  if (isAdmin) return all
  return all.filter((e) => String(e.idOrganizador) === String(user?.id))
}

export function insertGestionEvento(evento) {
  const list = readGestionEventos()
  list.unshift(evento)
  writeGestionEventos(list)
  return evento
}

export function updateGestionEvento(id, patch) {
  const list = readGestionEventos()
  const i = list.findIndex((e) => String(e.id) === String(id))
  if (i < 0) return null
  list[i] = { ...list[i], ...patch, updatedAt: new Date().toISOString() }
  writeGestionEventos(list)
  return list[i]
}
