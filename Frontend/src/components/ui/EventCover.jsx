import { useState } from 'react'
import {
  MdFestival,
  MdMic,
  MdMusicNote,
  MdSportsSoccer,
  MdTheaterComedy,
} from 'react-icons/md'

const TIPO_ICON = {
  RECITAL: MdMusicNote,
  FESTIVAL: MdFestival,
  DEPORTE: MdSportsSoccer,
  TEATRO: MdTheaterComedy,
  CONFERENCIA: MdMic,
}

function iniciales(nombre) {
  const palabras = String(nombre || '')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .split(/\s+/)
    .filter(Boolean)
  if (!palabras.length) return '♪'
  return palabras
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('')
}

/**
 * Portada del evento. Si hay `imagenUrl` y carga, la muestra; si no hay o falla,
 * dibuja una portada generada (degradé de marca según el tipo + ícono + iniciales),
 * para que ningún evento muestre una foto que no le corresponde.
 */
const EventCover = ({ evento, className = '', position, alt = '' }) => {
  const url = evento?.imagenUrl || ''
  const [failed, setFailed] = useState('')
  const tipo = String(evento?.tipo || '').toUpperCase()
  const Icon = TIPO_ICON[tipo] || MdMusicNote

  if (url && failed !== url) {
    return (
      <img
        className={`event-cover event-cover--img ${className}`.trim()}
        src={url}
        alt={alt}
        loading="lazy"
        style={position ? { objectPosition: position } : undefined}
        onError={() => setFailed(url)}
      />
    )
  }

  return (
    <div
      className={`event-cover event-cover--gen event-cover--${tipo.toLowerCase() || 'default'} ${className}`.trim()}
      role="img"
      aria-label={alt || evento?.nombre || 'Portada'}
    >
      <span className="event-cover__initials" aria-hidden>
        {iniciales(evento?.nombre)}
      </span>
      <Icon className="event-cover__icon" aria-hidden />
    </div>
  )
}

export default EventCover
