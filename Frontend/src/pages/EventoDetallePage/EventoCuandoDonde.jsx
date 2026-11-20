import {
  HiOutlineCalendar,
  HiOutlineExternalLink,
  HiOutlineLocationMarker,
} from 'react-icons/hi'
import { descargarEventoIcs } from '../../lib/ics'
import { urlMapaLugar } from '../../domain/evento'

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado']
const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
]

function srcMapa(lat, lng) {
  const dx = 0.008
  const dy = 0.0045
  const bbox = [lng - dx, lat - dy, lng + dx, lat + dy].join('%2C')
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat}%2C${lng}`
}

/**
 * "Cuándo y dónde": calendario con el día en grande + lugar con mapa y botones de acción.
 * Va debajo de la portada, antes de elegir entradas.
 */
const EventoCuandoDonde = ({ evento }) => {
  const d = new Date(evento.fechaInicio)
  const fechaOk = !Number.isNaN(d.getTime())
  const lugar = evento.lugar || {}
  const lat = Number(lugar.latitud)
  const lng = Number(lugar.longitud)
  const conMapa = Number.isFinite(lat) && Number.isFinite(lng)
  const enlaceMapa = urlMapaLugar(lugar)
  const direccion = [lugar.direccion, lugar.ciudad].filter(Boolean).join(', ')

  return (
    <section className="evento-donde" aria-label="Cuándo y dónde">
      {fechaOk && (
        <article className="evento-cuando">
          <div className="evento-cuando__cal" aria-hidden>
            <span className="evento-cuando__mes">{MESES[d.getMonth()]}</span>
            <strong>{d.getDate()}</strong>
            <span className="evento-cuando__anio">{d.getFullYear()}</span>
          </div>
          <div className="evento-cuando__info">
            <p className="evento-donde__label">Cuándo</p>
            <p className="evento-cuando__dia">
              {DIAS[d.getDay()]} {d.getDate()} de {MESES[d.getMonth()]}
            </p>
            <p className="evento-cuando__hora">
              {String(d.getHours()).padStart(2, '0')}:{String(d.getMinutes()).padStart(2, '0')} h
            </p>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => descargarEventoIcs(evento)}
            >
              <HiOutlineCalendar aria-hidden /> Agregar al calendario
            </button>
          </div>
        </article>
      )}

      {(lugar.nombre || conMapa) && (
        <article className="evento-lugar">
          <div className="evento-lugar__info">
            <p className="evento-donde__label">Dónde</p>
            <p className="evento-lugar__nombre">
              <HiOutlineLocationMarker aria-hidden /> {lugar.nombre}
            </p>
            {direccion && <p className="evento-lugar__dir">{direccion}</p>}
            {enlaceMapa && (
              <a
                className="btn btn-secondary btn-sm"
                href={enlaceMapa}
                target="_blank"
                rel="noopener noreferrer"
              >
                Cómo llegar <HiOutlineExternalLink aria-hidden />
              </a>
            )}
          </div>
          {conMapa && (
            <iframe
              className="evento-lugar__mapa"
              title={`Mapa: ${lugar.nombre}`}
              src={srcMapa(lat, lng)}
              loading="lazy"
              referrerPolicy="no-referrer"
            />
          )}
        </article>
      )}
    </section>
  )
}

export default EventoCuandoDonde
