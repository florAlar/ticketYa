import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  HiArrowRight,
  HiOutlineChevronDown,
} from 'react-icons/hi'
import useAsync from '../../../hooks/useAsync'
import { listEventos } from '../../../api/eventos'
import BandLogo from './BandLogo'
import { HERO_BANDAS } from './heroBands'
import './Hero.css'

const SLIDE_MS = 6500

/** Fecha corta es-AR: "15 de marzo"; con año solo si no es el año en curso ("15 de marzo de 2027"). */
function fechaCorta(iso) {
  const d = new Date(iso)
  if (!iso || Number.isNaN(d.getTime())) return ''
  const base = d.toLocaleDateString('es-AR', { day: 'numeric', month: 'long' })
  return d.getFullYear() === new Date().getFullYear() ? base : `${base} de ${d.getFullYear()}`
}

/** Texto oscuro o blanco según el brillo del color de fondo del botón. */
function tintaSobre(hex) {
  const n = parseInt(String(hex).slice(1), 16)
  const y = (((n >> 16) & 255) * 299 + ((n >> 8) & 255) * 587 + (n & 255) * 114) / 1000
  return y > 150 ? '#150d26' : '#ffffff'
}

/**
 * Portada de la home: escenario a pantalla completa que rota entre las bandas. Cada cartel lleva
 * el logo de la banda y, debajo, los datos de su evento (fecha, lugar y compra).
 * Los eventos vienen del catálogo (requiere sesión); sin sesión se muestra solo el logo.
 */
const Hero = ({ slides = HERO_BANDAS }) => {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (slides.length <= 1) return undefined
    const id = setInterval(() => setIndex((i) => (i + 1) % slides.length), SLIDE_MS)
    return () => clearInterval(id)
  }, [slides.length])

  const { data: eventos } = useAsync(
    async (signal) => {
      const data = await listEventos({ limit: 100 }, { signal })
      return Array.isArray(data) ? data : data?.content || data?.items || []
    },
    []
  )

  const lista = Array.isArray(eventos) ? eventos : []
  const actual = slides[index]
  const evento = lista.find((e) => actual.match.test(e.nombre))
  const gira = evento ? evento.nombre.split(' — ')[1] || evento.nombre : ''

  return (
    <>
      <section id="inicio" className="hero-t" aria-label="Portada">
        <div className="hero-t__stage" aria-hidden>
          {slides.map((s, i) => (
            <div
              key={s.id}
              className={`hero-t__slide${i === index ? ' is-active' : ''}`}
            >
              <img
                src={s.imageUrl}
                alt=""
                style={s.position ? { objectPosition: s.position } : undefined}
              />
            </div>
          ))}
          <div className="hero-t__wash" />
          <div className="hero-t__vignette" />
        </div>

        <div className="hero-t__content">
          <div className="hero-t__copy" key={actual.id}>
            <div className="hero-t__brand">
              <h1 className="hero-t__title">
                <BandLogo banda={actual} />
              </h1>
            </div>

            <div
              className="hero-t__info"
              style={{ '--cta': actual.acento, '--cta-ink': tintaSobre(actual.acento) }}
            >
              {evento && (
                <>
                  <strong className="hero-t__tour">{gira}</strong>
                  <span className="hero-t__star" aria-hidden>★</span>
                  <span className="hero-t__dato">{fechaCorta(evento.fechaInicio)}</span>
                  {evento.lugar?.nombre && (
                    <>
                      <span className="hero-t__star" aria-hidden>★</span>
                      <span className="hero-t__dato">
                        {evento.lugar.nombre}
                        {evento.lugar.ciudad ? `, ${evento.lugar.ciudad}` : ''}
                      </span>
                    </>
                  )}
                </>
              )}
              <Link
                className="btn btn-primary hero-t__buy"
                to={evento ? `/eventos/${evento.id}` : '/eventos'}
              >
                {evento ? 'Comprar entradas' : 'Explorar eventos'} <HiArrowRight aria-hidden />
              </Link>
            </div>
          </div>
        </div>

        <div className="hero-t__stars" role="tablist" aria-label="Bandas">
          {slides.map((s, i) => (
            <button
              key={s.id}
              type="button"
              role="tab"
              aria-selected={i === index}
              aria-label={s.nombre}
              className={`hero-t__starbtn${i === index ? ' is-active' : ''}`}
              onClick={() => setIndex(i)}
            >
              ★
            </button>
          ))}
        </div>

        <div className="hero-t__foot">
          <a className="hero-t__scroll" href="#explorar" aria-label="Bajar a los eventos">
            <HiOutlineChevronDown aria-hidden />
          </a>
        </div>

      </section>

      {lista.length > 0 && (
        <div className="hero-marquee" aria-hidden>
          <div className="hero-marquee__track">
            {[0, 1].map((n) => (
              <ul key={n}>
                {lista.map((e) => (
                  <li key={`${n}-${e.id}`}>
                    <span className="hero-marquee__star">★</span> {e.nombre}
                  </li>
                ))}
              </ul>
            ))}
          </div>
        </div>
      )}
    </>
  )
}

export default Hero
