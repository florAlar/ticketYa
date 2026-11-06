/**
 * Imágenes locales fake (Vite resuelve URL al build).
 * src/assets/img/*
 */
import heroLolla from '../../assets/img/0b6268f7-a629-4bcb-8ddd-22e517278186-banners-lineuplolla27-dfaaheadliners-1920x720-3-2.jpg'
import heroRiver from '../../assets/img/0e6ab744-d73c-49a0-8426-f3b913fc2dae-abgriver1920x720quentro-1.jpg'
import heroFoo from '../../assets/img/9cb0ea32-0e49-40ad-b09a-0962eae3a21d-foofighters-bannersaa-1920x720.jpg'
import imgParty from '../../assets/img/banner_superior-back-in-time-party-en-c-art-media-547329.jpg'
import imgFotojet from '../../assets/img/fotojet-2023-12-15t153717-682-1702676311666-354214877.jpg'
import imgQatar from '../../assets/img/qatar-flyer-futbol-mundial-2022-design-template-6208ad784a83f014766781e6e56fd33c_screen-3776715169.jpg'

/** Carrusel Hero (banners anchos + extras) */
export const HERO_SLIDES = [
  { id: 'h1', imageUrl: heroLolla, alt: 'Lollapalooza lineup' },
  { id: 'h2', imageUrl: heroRiver, alt: 'River — evento en estadio' },
  { id: 'h3', imageUrl: heroFoo, alt: 'Foo Fighters' },
  { id: 'h4', imageUrl: imgParty, alt: 'Back in Time Party' },
  { id: 'h5', imageUrl: imgFotojet, alt: 'Evento destacado' },
  { id: 'h6', imageUrl: imgQatar, alt: 'Fútbol / deporte' },
]

/** Pool reutilizable para cards de eventos mock */
export const EVENT_IMAGES = [
  heroLolla,
  heroRiver,
  heroFoo,
  imgParty,
  imgFotojet,
  imgQatar,
]
