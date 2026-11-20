import { useCallback, useEffect, useRef, useState } from 'react'
import { HiOutlineChevronRight } from 'react-icons/hi'

const AUTO_MS = 4200

/**
 * Fila horizontal de cards sin barra de scroll: avanza sola cada pocos segundos
 * (se pausa con el mouse o el foco encima, y no se mueve con "reducir movimiento")
 * y tiene una flecha discreta a la derecha para avanzar a mano. Al llegar al final vuelve al inicio.
 */
const EventRow = ({ className, children }) => {
  const ref = useRef(null)
  const [pausado, setPausado] = useState(false)
  const [desborda, setDesborda] = useState(false)

  const medir = useCallback(() => {
    const el = ref.current
    if (el) setDesborda(el.scrollWidth > el.clientWidth + 4)
  }, [])

  const avanzar = useCallback(() => {
    const el = ref.current
    if (!el) return
    const item = el.querySelector('.event-list__item')
    const paso = item ? item.getBoundingClientRect().width + 14 : el.clientWidth * 0.8
    if (el.scrollLeft + el.clientWidth >= el.scrollWidth - 4) {
      el.scrollTo({ left: 0, behavior: 'smooth' })
    } else {
      el.scrollBy({ left: paso, behavior: 'smooth' })
    }
  }, [])

  useEffect(() => {
    medir()
    window.addEventListener('resize', medir)
    return () => window.removeEventListener('resize', medir)
  }, [medir, children])

  useEffect(() => {
    if (pausado || !desborda) return undefined
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return undefined
    const id = setInterval(avanzar, AUTO_MS)
    return () => clearInterval(id)
  }, [pausado, desborda, avanzar])

  return (
    <div
      className="event-row"
      onMouseEnter={() => setPausado(true)}
      onMouseLeave={() => setPausado(false)}
      onFocus={() => setPausado(true)}
      onBlur={() => setPausado(false)}
    >
      <ul className={className} ref={ref}>
        {children}
      </ul>
      {desborda && (
        <button
          type="button"
          className="event-row__next"
          onClick={avanzar}
          aria-label="Ver más eventos"
        >
          <HiOutlineChevronRight aria-hidden />
        </button>
      )}
    </div>
  )
}

export default EventRow
