/**
 * Datos de sitio (chrome). Escalable sin content.json de plantilla.
 * Logo/Footer leen brand + footer; el resto queda listo para SEO/contacto.
 */
const site = {
  brand: {
    name: 'TicketYa',
    slogan: 'Entradas sin filas',
    logo: '',
    logoAlt: 'TicketYa',
  },

  seo: {
    title: 'TicketYa',
    description: 'Plataforma de venta de entradas',
    keywords: 'entradas, eventos, ticketera',
    author: 'TUDAI 2026',
  },

  footer: {
    copyright: `© ${new Date().getFullYear()} TUDAI 2026`,
    madeWith: 'Arquitecturas Web — TUDAI',
  },
}

export default site
