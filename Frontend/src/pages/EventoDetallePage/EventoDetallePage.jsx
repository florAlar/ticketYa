import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento, getDisponibilidadEvento } from '../../api/eventos'
import {
  formatEventoFecha,
  formatPrecioDesde,
} from '../../components/home/events/eventFormat'
import EventoCupoBadge from '../../components/home/events/EventoCupoBadge'
import {
  EVENTO_ESTADOS,
  EVENTO_TIPO_LABELS,
  NIVEL_CUPO_LABELS,
  formatParticipantesLine,
  isEventoAgotado,
  normalizeDisponibilidadDto,
  withCupoResumen,
} from '../../domain/evento'
import '../../components/home/events/events.css'
import './EventoDetallePage.css'

/**
 * Detalle + tipos de entrada.
 * Cupo comercial: GET disponibilidad (mismo que checkout); badge + Comprar por sector.
 */
const EventoDetallePage = () => {
  const { eventoId } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const [evento, setEvento] = useState(null)
  const [disponibilidad, setDisponibilidad] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await getEvento(eventoId)
      let disp = null
      try {
        disp = normalizeDisponibilidadDto(
          await getDisponibilidadEvento(eventoId)
        )
      } catch {
        disp = normalizeDisponibilidadDto({
          idEvento: eventoId,
          disponibleTotal: 0,
          sectores: [],
        })
      }
      setDisponibilidad(disp)
      setEvento(withCupoResumen(data, disp))
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar el evento')
      setStatus('error')
    }
  }, [eventoId])

  useEffect(() => {
    load()
  }, [load])

  const sectores = useMemo(
    () => evento?.sectores || evento?.lugar?.sectores || [],
    [evento]
  )

  const cupoBySector = useMemo(() => {
    const map = new Map()
    for (const s of disponibilidad?.sectores || []) {
      if (s.idSector) map.set(String(s.idSector), s)
    }
    return map
  }, [disponibilidad])

  const aLaVenta =
    evento &&
    (evento.estado === EVENTO_ESTADOS.PUBLICADO || evento.estado === 'PUBLICADO')

  const suspendido =
    evento &&
    (evento.estado === EVENTO_ESTADOS.SUSPENDIDO ||
      evento.estado === 'SUSPENDIDO')

  const eventoAgotado = isEventoAgotado(evento)

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
              <EventoCupoBadge evento={evento} className="evento-detalle-page__cupo" />
            </div>
          )}

          <header className="evento-detalle-page__head">
            <p className="evento-detalle-page__meta">
              {EVENTO_TIPO_LABELS[evento.tipo] || evento.tipo}
              {evento.estado ? ` · ${evento.estado}` : ''}
            </p>
            <h1>
              {evento.nombre}{' '}
              {!evento.imagenUrl && <EventoCupoBadge evento={evento} />}
            </h1>
            {evento.cupo && !evento.cupo.agotado && evento.cupo.nivelCupoLabel && (
              <p className="evento-detalle-page__cupo-line">
                {evento.cupo.nivelCupoLabel}
                {evento.cupo.disponibleTotal != null
                  ? ` · ${evento.cupo.disponibleTotal} libres`
                  : ''}
              </p>
            )}
            {eventoAgotado && (
              <p className="evento-detalle-page__cupo-line evento-detalle-page__cupo-line--agotado">
                Agotado — no hay cupo comercial disponible.
              </p>
            )}
            {suspendido && (
              <p className="evento-detalle-page__cupo-line evento-detalle-page__cupo-line--suspend">
                Evento suspendido — la venta de entradas nuevas está pausada.
                Quienes ya compraron conservan su entrada.
              </p>
            )}
            <p>
              {formatEventoFecha(evento.fechaInicio)}
              {evento.lugar?.nombre ? ` · ${evento.lugar.nombre}` : ''}
              {evento.lugar?.ciudad ? ` · ${evento.lugar.ciudad}` : ''}
            </p>
            {formatParticipantesLine(evento.participantes) && (
              <p className="evento-detalle-page__artistas">
                {formatParticipantesLine(evento.participantes)}
              </p>
            )}
            {evento.descripcion && (
              <p className="evento-detalle-page__desc">{evento.descripcion}</p>
            )}
          </header>

          <section className="evento-detalle-page__tickets">
            <h2>Tipos de entrada</h2>
            {!aLaVenta && (
              <p className="evento-detalle-page__muted">
                {suspendido
                  ? 'No se pueden comprar entradas mientras el evento esté suspendido.'
                  : `Este evento no está a la venta (${evento.estado}).`}
              </p>
            )}
            {aLaVenta && sectores.length === 0 && (
              <p className="evento-detalle-page__muted">
                No hay sectores cargados para este evento.
              </p>
            )}
            {aLaVenta && sectores.length > 0 && (
              <ul className="evento-ticket-list">
                {sectores.map((s) => {
                  const cupo =
                    cupoBySector.get(String(s.id)) ||
                    cupoBySector.get(String(s.nombre))
                  const disp = cupo
                    ? Number(cupo.cantidadDisponible)
                    : null
                  const sectorAgotado =
                    cupo?.agotado === true ||
                    (disp != null && Number.isFinite(disp) && disp < 1) ||
                    (disponibilidad && !cupo && eventoAgotado)
                  const nivelLabel =
                    cupo?.nivelCupoLabel ||
                    (sectorAgotado ? NIVEL_CUPO_LABELS.AGOTADO : null)
                  const canBuy =
                    aLaVenta && Number.isFinite(disp) && disp >= 1

                  return (
                    <li
                      key={s.id}
                      className={`evento-ticket-card${
                        sectorAgotado ? ' evento-ticket-card--agotado' : ''
                      }`}
                    >
                      <div>
                        <h3>
                          {s.nombre}
                          {nivelLabel && (
                            <span
                              className={`evento-cupo-badge ${
                                sectorAgotado
                                  ? 'evento-cupo-badge--agotado'
                                  : 'evento-cupo-badge--aviso'
                              }`}
                              style={{ marginLeft: '0.5rem', verticalAlign: 'middle' }}
                            >
                              {nivelLabel}
                            </span>
                          )}
                        </h3>
                        <p className="evento-detalle-page__muted">
                          {s.esNumerado ? 'Numerado' : 'No numerado'}
                          {Number.isFinite(disp)
                            ? ` · ${disp} disponibles`
                            : cupo
                              ? ''
                              : ' · sin cupo comercial'}
                        </p>
                        <p className="evento-ticket-card__price">
                          {formatPrecioDesde(s.precio)}
                        </p>
                      </div>
                      <button
                        type="button"
                        className="btn btn-primary"
                        disabled={!canBuy}
                        title={
                          canBuy
                            ? 'Comprar'
                            : sectorAgotado
                              ? 'Sector agotado'
                              : 'Sin cupo disponible'
                        }
                        onClick={() => canBuy && irComprar(s.id)}
                      >
                        {sectorAgotado ? 'Agotado' : 'Comprar'}
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>
        </>
      )}
    </section>
  )
}

export default EventoDetallePage
