import {
  HiOutlineExternalLink,
  HiOutlineLocationMarker,
} from 'react-icons/hi'
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

  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')

  return (
    <section
      className={`evento-donde${fechaOk && (lugar.nombre || conMapa) ? '' : ' evento-donde--solo'}`}
      aria-label="Cuándo y dónde"
    >
      {fechaOk && (
        <article className="evento-cal">
          <header className="evento-cal__mes">
            {MESES[d.getMonth()]} {d.getFullYear()}
          </header>
          <div className="evento-cal__cuerpo">
            <span className="evento-cal__num">{d.getDate()}</span>
            <span className="evento-cal__dia">{DIAS[d.getDay()]}</span>
            <span className="evento-cal__hora">
              {hh}:{mm} <small>hs</small>
            </span>
          </div>
        </article>
      )}

      {(lugar.nombre || conMapa) && (
        <article className="evento-lugar">
          {conMapa && (
            <iframe
              className="evento-lugar__mapa"
              title={`Mapa: ${lugar.nombre}`}
              src={srcMapa(lat, lng)}
              loading="lazy"
              referrerPolicy="no-referrer"
            />
          )}
          <div className={`evento-lugar__info${conMapa ? ' evento-lugar__info--sobre' : ''}`}>
            <div>
              <h3 className="evento-lugar__nombre">
                <HiOutlineLocationMarker aria-hidden /> {lugar.nombre}
              </h3>
              {direccion && <p className="evento-lugar__dir">{direccion}</p>}
            </div>
            {enlaceMapa && (
              <a
                className="evento-lugar__ir"
                href={enlaceMapa}
                target="_blank"
                rel="noopener noreferrer"
              >
                Cómo llegar <HiOutlineExternalLink aria-hidden />
              </a>
            )}
          </div>
        </article>
      )}
    </section>
  )
}

export default EventoCuandoDonde
