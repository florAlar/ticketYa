import { EVENT_IMAGES, HERO_SLIDES } from './images'

export { HERO_SLIDES }

const img = (i) => EVENT_IMAGES[i % EVENT_IMAGES.length]

/** Catálogo mock alineado al seed BE (3 eventos, estados distintos). */
export const MOCK_EVENTOS = [
  {
    id: 'd0000000-0000-4000-8000-000000000001',
    nombre: 'Metallica — M72 World Tour',
    tipo: 'RECITAL',
    fechaInicio: '2027-03-15T21:00:00',
    lugar: { nombre: 'Estadio Más Monumental', ciudad: 'Buenos Aires' },
    imagenUrl: img(2),
    precioDesde: 95000,
    destacado: true,
    estado: 'PUBLICADO',
    esAltaDemanda: true,
    participantes: [
      { id: 'p0000000-0000-4000-8000-000000000001', nombre: 'Metallica', rol: 'PRINCIPAL' },
      { id: 'p0000000-0000-4000-8000-000000000002', nombre: 'Gojira', rol: 'TELONERO' },
    ],
  },
  {
    id: 'd0000000-0000-4000-8000-000000000002',
    nombre: 'UEFA Champions League Final 2027',
    tipo: 'DEPORTE',
    fechaInicio: '2027-05-30T21:00:00',
    lugar: { nombre: 'Estadio Más Monumental', ciudad: 'Buenos Aires' },
    imagenUrl: img(5),
    precioDesde: 150000,
    destacado: true,
    estado: 'SUSPENDIDO',
    esAltaDemanda: true,
    participantes: [
      { id: 'p0000000-0000-4000-8000-000000000003', nombre: 'Real Madrid CF', rol: 'PRINCIPAL' },
      { id: 'p0000000-0000-4000-8000-000000000004', nombre: 'Manchester City FC', rol: 'PRINCIPAL' },
    ],
  },
  {
    id: 'd0000000-0000-4000-8000-000000000003',
    nombre: 'Rock in Rio — día metal (cancelado)',
    tipo: 'FESTIVAL',
    fechaInicio: '2027-09-18T16:00:00',
    lugar: { nombre: 'Estadio Más Monumental', ciudad: 'Buenos Aires' },
    imagenUrl: img(0),
    precioDesde: 75000,
    destacado: false,
    estado: 'CANCELADO',
    esAltaDemanda: false,
    participantes: [
      { id: 'p0000000-0000-4000-8000-000000000005', nombre: 'Iron Maiden', rol: 'PRINCIPAL' },
      { id: 'p0000000-0000-4000-8000-000000000006', nombre: 'Ghost', rol: 'TELONERO' },
    ],
  },
]
