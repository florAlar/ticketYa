import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento } from '../../api/eventos'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'
import { EVENTO_ESTADOS, EVENTO_TIPO_LABELS } from '../../domain/evento'
import './EventoDetallePage.css'

/**
 * Detalle + tipos de entrada (sectores del alta org/admin).
 * Comprar → checkout con sector (sin carrito).
 */
const EventoDetallePage = () => {
  const { eventoId } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const [evento, setEvento] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await getEvento(eventoId)
      setEvento(data)
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar el evento')
      setStatus('error')
    }
  }, [eventoId])

  useEffect(() => {
    load()
  }, [load])

  const sectores = evento?.sectores || evento?.lugar?.sectores || []
  const aLaVenta =
    evento &&
    (evento.estado === EVENTO_ESTADOS.PUBLICADO || evento.estado === 'PUBLICADO')

  const irComprar = (sectorId) => {
    const path = `/eventos/${eventoId}/comprar?sector=${encodeURIComponent(sectorId)}`
    if (!isAuthenticated) {
      navigate(`/login?next=${encodeURIComponent(path)}`)
      return
    }
    navigate(path)
  }

  return (
    <section className="evento-detalle-page container-slim section-v-padding">
      <p className="evento-detalle-page__back">
        <Link to="/eventos">← Volver al listado</Link>
      </p>

      {status === 'loading' && <p>Cargando evento…</p>}
      {error && <p className="evento-detalle-page__error">{error}</p>}

      {status === 'ok' && evento && (
        <>
          {evento.imagenUrl && (
            <div className="evento-detalle-page__media">
              <img src={evento.imagenUrl} alt="" />
            </div>
          )}

          <header className="evento-detalle-page__head">
            <p className="evento-detalle-page__meta">
              {EVENTO_TIPO_LABELS[evento.tipo] || evento.tipo}
              {evento.estado ? ` · ${evento.estado}` : ''}
            </p>
            <h1>{evento.nombre}</h1>
            <p>
              {formatEventoFecha(evento.fechaInicio)}
              {evento.lugar?.nombre
                ? ` · ${evento.lugar.nombre}`
                : ''}
              {evento.lugar?.ciudad ? ` · ${evento.lugar.ciudad}` : ''}
            </p>
            {evento.descripcion && (
              <p className="evento-detalle-page__desc">{evento.descripcion}</p>
            )}
          </header>

          <section className="evento-detalle-page__tickets">
            <h2>Tipos de entrada</h2>
            {!aLaVenta && (
              <p className="evento-detalle-page__muted">
                Este evento no está a la venta ({evento.estado}).
              </p>
            )}
            {aLaVenta && sectores.length === 0 && (
              <p className="evento-detalle-page__muted">
                No hay sectores cargados para este evento.
              </p>
            )}
            {aLaVenta && sectores.length > 0 && (
              <ul className="evento-ticket-list">
                {sectores.map((s) => (
                  <li key={s.id} className="evento-ticket-card">
                    <div>
                      <h3>{s.nombre}</h3>
                      <p className="evento-detalle-page__muted">
                        {s.esNumerado ? 'Numerado' : 'No numerado'}
                        {s.sinCupo
                          ? ' · sin cupo'
                          : s.capacidadTotal != null
                            ? ` · cupo ${s.capacidadTotal}`
                            : ''}
                      </p>
                      <p className="evento-ticket-card__price">
                        {formatPrecioDesde(s.precio)}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="btn btn-primary"
                      onClick={() => irComprar(s.id)}
                    >
                      Comprar
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </section>
  )
}

export default EventoDetallePage
