import ImageCarousel from './ImageCarousel'
import { HERO_SLIDES } from '../../../api/mock/eventos'
import './Hero.css'

/** Hero Home: carrusel de imágenes (~30–40vh). */
const Hero = ({ slides = HERO_SLIDES }) => {
  return (
    <section id="inicio" className="home-hero" aria-label="Destacados visuales">
      <ImageCarousel slides={slides} className="image-carousel--hero" />
    </section>
  )
}

export default Hero
