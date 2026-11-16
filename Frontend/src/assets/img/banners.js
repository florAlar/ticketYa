/** Banners estáticos de la home (decorativos, no dependen de mock ni de backend). */
import heroLolla from './0b6268f7-a629-4bcb-8ddd-22e517278186-banners-lineuplolla27-dfaaheadliners-1920x720-3-2.jpg'
import heroRiver from './0e6ab744-d73c-49a0-8426-f3b913fc2dae-abgriver1920x720quentro-1.jpg'
import heroFoo from './9cb0ea32-0e49-40ad-b09a-0962eae3a21d-foofighters-bannersaa-1920x720.jpg'
import imgParty from './banner_superior-back-in-time-party-en-c-art-media-547329.jpg'
import imgFotojet from './fotojet-2023-12-15t153717-682-1702676311666-354214877.jpg'
import imgQatar from './qatar-flyer-futbol-mundial-2022-design-template-6208ad784a83f014766781e6e56fd33c_screen-3776715169.jpg'
import heroMetallica from './1747868040_image002-401716555.jpg'
import heroRammstein from './rammstein-foro-sol-cronica-concierto-gira-2022-mexico-2-4194887842.jpg'

/**
 * Carrusel Hero. El recorte real es muy apaisado (~3.4:1: 52vh de alto a todo el ancho), por eso
 * solo van imágenes horizontales y de buena resolución. `position` = object-position para elegir
 * qué franja se ve (ej. 'center top' deja el logo y recorta el resto).
 */
export const HERO_SLIDES = [
  { id: 'h1', imageUrl: heroMetallica, alt: 'Metallica — M72 World Tour', position: 'center top' },
  { id: 'h2', imageUrl: heroRammstein, alt: 'Rammstein en concierto', position: 'center 55%' },
  { id: 'h3', imageUrl: heroLolla, alt: 'Lollapalooza lineup' },
  { id: 'h4', imageUrl: heroRiver, alt: 'River — evento en estadio' },
  { id: 'h5', imageUrl: heroFoo, alt: 'Foo Fighters' },
]

/** Pool de imágenes del mock de eventos (se mantiene el original; no es el carrusel). */
export const BANNER_IMAGES = [heroLolla, heroRiver, heroFoo, imgParty, imgFotojet, imgQatar]
