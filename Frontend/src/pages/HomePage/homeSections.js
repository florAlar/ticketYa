/**
 * Secciones de Home: solo catálogo comprable (PUBLICADO).
 * Suspendidos no van en vitrina de inicio. Ningún carrusel muestra eventos que ya pasaron (soloFuturos).
 */
export const homeSections = [
  {
    id: 'destacados',
    title: 'Destacados',
    // Todos los destacados que todavía no pasaron (200 = tope de página del backend)
    filter: { destacados: true, soloFuturos: true, limit: 200 },
    layout: 'grid-featured',
    card: 'featured',
    linkToAll: '/eventos',
  },
  {
    id: 'recitales',
    title: 'Recitales',
    filter: { tipo: 'RECITAL', soloFuturos: true, limit: 8 },
    layout: 'row',
    card: 'standard',
    linkToAll: '/eventos?tipo=RECITAL',
  },
  {
    id: 'deportes',
    title: 'Deportes',
    filter: { tipo: 'DEPORTE', soloFuturos: true, limit: 8 },
    layout: 'row',
    card: 'standard',
    linkToAll: '/eventos?tipo=DEPORTE',
  },
]
