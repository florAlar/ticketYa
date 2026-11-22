import boca from '../../../assets/img/hero/boca.webp'
import dakar from '../../../assets/img/hero/dakar.webp'
import formula1 from '../../../assets/img/hero/formula1.webp'
import ghost from '../../../assets/img/hero/ghost.webp'
import ironMaiden from '../../../assets/img/hero/iron-maiden.webp'
import metallica from '../../../assets/img/hero/metallica.webp'
import motogp from '../../../assets/img/hero/motogp.webp'
import rammstein from '../../../assets/img/hero/rammstein.webp'
import rhcp from '../../../assets/img/hero/red-hot-chili-peppers.webp'
import ufc from '../../../assets/img/hero/ufc.webp'

/**
 * Logos oficiales: `assets/img/hero/logos/<id>.svg|png|webp`, donde `<id>` es el de la lista de abajo.
 * Si falta el archivo se muestra el nombre en letras (`BandLogo`) hasta que se suba el logo.
 */
const LOGOS = import.meta.glob('../../../assets/img/hero/logos/*.{svg,png,webp}', {
  eager: true,
  query: '?url',
  import: 'default',
})

function logoDe(id) {
  const hit = Object.entries(LOGOS).find(
    ([ruta]) => /\/([^/]+)\.[a-z]+$/.exec(ruta)?.[1] === id
  )
  return hit ? hit[1] : null
}

/**
 * Carteles del hero (bandas y deportes). `match` busca el evento en el catálogo por nombre;
 * `position` es el object-position de la foto; `escala` (opcional, 1 por defecto) achica el logo; `acento` es el color de la foto con el que se pinta el botón de compra; `lineas` parte el nombre en el logotipo de respaldo.
 */
const BANDAS = [
  { id: 'metallica', nombre: 'Metallica', match: /metallica/i, imageUrl: metallica, position: 'center 40%', acento: '#e6d200' },
  { id: 'iron-maiden', nombre: 'Iron Maiden', match: /iron maiden/i, imageUrl: ironMaiden, position: 'center 40%', lineas: ['Iron', 'Maiden'], acento: '#2a7bff' },
  { id: 'boca', nombre: 'Boca Juniors', match: /boca/i, imageUrl: boca, position: 'center 55%', lineas: ['Boca', 'Juniors'], escala: 0.8, acento: '#f2a900' },
  { id: 'rammstein', nombre: 'Rammstein', match: /rammstein/i, imageUrl: rammstein, position: 'center 45%', acento: '#ff7a33' },
  { id: 'motogp', nombre: 'MotoGP', match: /motogp/i, imageUrl: motogp, position: 'center 60%', escala: 0.8, acento: '#e8333d' },
  { id: 'dakar', nombre: 'Dakar Rally', match: /dakar/i, imageUrl: dakar, position: 'center 35%', lineas: ['Dakar', 'Rally'], acento: '#ff6a13' },
  { id: 'rhcp', nombre: 'Red Hot Chili Peppers', match: /red hot chili/i, imageUrl: rhcp, position: 'center 35%', lineas: ['Red Hot', 'Chili Peppers'], acento: '#ff2a82' },
  { id: 'ufc', nombre: 'UFC', match: /ufc/i, imageUrl: ufc, position: 'center 45%', escala: 0.78, acento: '#4a5bff' },
  { id: 'ghost', nombre: 'Ghost', match: /ghost/i, imageUrl: ghost, position: 'center 35%', acento: '#3d8bff' },
  { id: 'formula1', nombre: 'Fórmula 1', match: /formula 1/i, imageUrl: formula1, position: 'center 50%', escala: 0.8, acento: '#2f5bff' },
]

export const HERO_BANDAS = BANDAS.map((b) => ({ ...b, logoUrl: logoDe(b.id) }))
