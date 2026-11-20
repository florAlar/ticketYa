import { useCallback, useMemo } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { getEvento, getDisponibilidadEvento } from '../../api/eventos'
import { getEstadoFila } from '../../api/filaVirtual'
import { isAbortError } from '../../api/http'
import { getFilaToken } from '../../domain/filaVirtual'
import { formatFecha } from '../../lib/format'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { formatPrecioDesde } from '../../components/home/events/eventFormat'
import EventCover from '../../components/ui/EventCover'
import InfoTip from '../../components/ui/InfoTip'
import Badge from '../../components/ui/Badge'
import { HiOutlineBan, HiOutlineFire, HiOutlineLightningBolt } from 'react-icons/hi'
import EventoCupoBadge from '../../components/home/events/EventoCupoBadge'
import EventosSimilares from '../../components/preferencias/EventosSimilares'
import { sectorTieneLayout } from '../../domain/asiento'
import {
  EVENTO_ESTADOS,
  EVENTO_TIPO_LABELS,
  NIVEL_CUPO_LABELS,
  formatParticipantesLine,
  isEventoAgotado,
  normalizeDisponibilidadDto,
  withCupoResumen,
} from '../../domain/evento'
import EventoCuandoDonde from './EventoCuandoDonde'
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

  const load = useCallback(
    async (signal) => {
      const data = await getEvento(eventoId, { signal })
      let disp
      try {
        disp = normalizeDisponibilidadDto(
          await getDisponibilidadEvento(eventoId, { signal })
        )
      } catch (err) {
        if (isAbortError(err)) throw err
        disp = normalizeDisponibilidadDto({
          idEvento: eventoId,
          disponibleTotal: 0,
          sectores: [],
        })
      }
      let filaEstado = null
      const esAltaDemanda = Boolean(data?.esAltaDemanda)
      if (esAltaDemanda) {
        try {
          filaEstado = await getEstadoFila(eventoId, { signal })
        } catch (err) {
          if (isAbortError(err)) throw err
          filaEstado = null
        }
      }
      return {
        evento: withCupoResumen(data, disp),
        disponibilidad: disp,
        filaEstado,
      }
    },
    [eventoId]
  )

  const { data, status, error, reload } = useAsync(load, [eventoId])
  const evento = data?.evento || null
  const disponibilidad = data?.disponibilidad || null
  const filaEstado = data?.filaEstado || null
  const paseFila = useMemo(() => getFilaToken(eventoId), [eventoId])

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
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    if (evento?.esAltaDemanda && !getFilaToken(eventoId)) {
      navigate(
        `/eventos/${eventoId}/fila?sector=${encodeURIComponent(sectorId)}`
      )
      return
    }
    navigate(
      `/eventos/${eventoId}/comprar?sector=${encodeURIComponent(sectorId)}`
    )
  }

  return (
    <section className="evento-detalle-page container section-v-padding">
      <p className="evento-detalle-page__back">
        <Link to="/eventos">← Volver al listado</Link>
      </p>

      {status === 'loading' && <LoadingState label="Cargando evento…" />}
      {status === 'error' && <ErrorState error={error} onRetry={reload} />}

      {status === 'success' && evento && (
        <>
          <div className="evento-detalle-page__media">
            <EventCover evento={evento} alt={evento.nombre} position="center 30%" />
            <EventoCupoBadge evento={evento} className="evento-detalle-page__cupo" />
            {evento.esAltaDemanda && (
              <Badge
                hot
                icon={HiOutlineLightningBolt}
                className="evento-detalle-page__hot"
              >
                Alta demanda
              </Badge>
            )}
            <header className="evento-detalle-page__head">
              <p className="evento-detalle-page__meta">
                {EVENTO_TIPO_LABELS[evento.tipo] || evento.tipo}
                {evento.estado ? ` · ${evento.estado}` : ''}
              </p>
              <h1>{evento.nombre}</h1>
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
              {formatParticipantesLine(evento.participantes) && (
                <p className="evento-detalle-page__artistas">
                  {formatParticipantesLine(evento.participantes)}
                </p>
              )}
              {evento.descripcion && (
                <p className="evento-detalle-page__desc">{evento.descripcion}</p>
              )}
            </header>
          </div>

          <EventoCuandoDonde evento={evento} />

          <section className="evento-entradas" id="entradas">
            <header className="evento-entradas__head">
              <h2>Elegí tu entrada</h2>
              {aLaVenta && evento.esAltaDemanda && (
                <p className="evento-entradas__fila">
                  <HiOutlineLightningBolt aria-hidden /> Se compra con fila virtual
                  <InfoTip label="¿Qué es la fila virtual?">
                    Este evento tiene mucha demanda: entrás en una cola, te
                    asignamos un turno por orden de llegada y comprás cuando te
                    toque, por tandas.
                  </InfoTip>
                  <span>
                    {paseFila
                      ? `Tenés un pase vigente hasta las ${formatFecha(paseFila.expiraEn, { timeStyle: 'short' })}`
                      : filaEstado?.abierta
                        ? `${filaEstado.totalEnFila ?? 0} en la fila ahora`
                        : 'La fila no está abierta ahora'}
                  </span>
                  {paseFila && (
                    <Link to={`/eventos/${eventoId}/fila`}>Ver mi turno</Link>
                  )}
                </p>
              )}
            </header>

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
                  const disp = cupo ? Number(cupo.cantidadDisponible) : null
                  const sectorAgotado =
                    cupo?.agotado === true ||
                    (disp != null && Number.isFinite(disp) && disp < 1) ||
                    (disponibilidad && !cupo && eventoAgotado)
                  const nivelLabel =
                    cupo?.nivelCupoLabel ||
                    (sectorAgotado ? NIVEL_CUPO_LABELS.AGOTADO : null)
                  const canBuy = aLaVenta && Number.isFinite(disp) && disp >= 1

                  return (
                    <li
                      key={s.id}
                      className={`evento-ticket-card${
                        sectorAgotado ? ' evento-ticket-card--agotado' : ''
                      }`}
                    >
                      <div className="evento-ticket-card__top">
                        <h3>{s.nombre}</h3>
                        <Badge
                          tone={sectorAgotado ? 'danger' : nivelLabel ? 'warning' : 'success'}
                          size="sm"
                          icon={sectorAgotado ? HiOutlineBan : nivelLabel ? HiOutlineFire : undefined}
                        >
                          {nivelLabel || 'Hay entradas'}
                        </Badge>
                      </div>
                      <p className="evento-ticket-card__price">
                        {formatPrecioDesde(s.precio)}
                        {s.esNumerado && <small> por asiento</small>}
                      </p>
                      <p className="evento-detalle-page__muted">
                        {s.esNumerado
                          ? sectorTieneLayout(s)
                            ? `Numerado · ${s.filas} filas × ${s.asientosPorFila}`
                            : 'Numerado'
                          : 'Entrada general'}
                      </p>
                      <button
                        type="button"
                        className="btn btn-primary btn-block"
                        disabled={!canBuy}
                        title={
                          canBuy
                            ? evento.esAltaDemanda && !paseFila
                              ? 'Primero vas a entrar a la fila virtual'
                              : 'Comprar'
                            : sectorAgotado
                              ? 'Sector agotado'
                              : 'Sin cupo disponible'
                        }
                        onClick={() => canBuy && irComprar(s.id)}
                      >
                        {sectorAgotado
                          ? 'Agotado'
                          : s.esNumerado
                            ? 'Elegir asientos'
                            : 'Comprar'}
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>

          <EventosSimilares key={eventoId} eventoId={eventoId} />
        </>
      )}
    </section>
  )
}

export default EventoDetallePage
