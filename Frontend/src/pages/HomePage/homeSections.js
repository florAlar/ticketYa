/**
 * Secciones de Home: solo catálogo comprable (PUBLICADO).
 * Suspendidos no van en vitrina de inicio.
 */
export const homeSections = [
  {
    id: 'destacados',
    title: 'Destacados',
    filter: { destacados: true, limit: 8 },
    layout: 'grid',
    card: 'featured',
    linkToAll: '/eventos',
  },
  {
    id: 'recitales',
    title: 'Recitales',
    filter: { tipo: 'RECITAL', limit: 8 },
    layout: 'row',
    card: 'standard',
    linkToAll: '/eventos?tipo=RECITAL',
  },
  {
    id: 'teatro',
    title: 'Teatro',
    filter: { tipo: 'TEATRO', limit: 8 },
    layout: 'row',
    card: 'standard',
    linkToAll: '/eventos?tipo=TEATRO',
  },
]
