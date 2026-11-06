/**
 * Secciones de Home: filtro API + layout + tipo de card.
 * Preferencias futuras = más entradas acá (o desde backend).
 */
export const homeSections = [
  {
    id: 'destacados',
    title: 'Destacados',
    filter: { destacados: true, limit: 8 },
    layout: 'grid',
    card: 'featured',
    linkToAll: '/eventos?destacados=1',
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
