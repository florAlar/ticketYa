import { useEffect, useState } from 'react'
import './ImageCarousel.css'

/**
 * Carrusel de imágenes (Hero u otros banners).
 * slides: [{ id, imageUrl, alt? }]
 */
const ImageCarousel = ({
  slides = [],
  intervalMs = 5000,
  className = '',
  heightClass = '',
}) => {
  const [index, setIndex] = useState(0)
  const total = slides.length

  useEffect(() => {
    if (total <= 1) return undefined
    const id = setInterval(() => {
      setIndex((i) => (i + 1) % total)
    }, intervalMs)
    return () => clearInterval(id)
  }, [total, intervalMs])

  if (!total) {
    return (
      <div className={`image-carousel image-carousel--empty ${className} ${heightClass}`} />
    )
  }

  const current = slides[index]

  return (
    <div className={`image-carousel ${className} ${heightClass}`}>
      <div className="image-carousel__frame">
        <img
          key={current.id || current.imageUrl}
          src={current.imageUrl}
          alt={current.alt || ''}
          className="image-carousel__img"
        />
      </div>

      {total > 1 && (
        <div className="image-carousel__dots" role="tablist" aria-label="Slides">
          {slides.map((s, i) => (
            <button
              key={s.id || i}
              type="button"
              role="tab"
              aria-selected={i === index}
              className={`image-carousel__dot ${i === index ? 'is-active' : ''}`}
              onClick={() => setIndex(i)}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export default ImageCarousel
