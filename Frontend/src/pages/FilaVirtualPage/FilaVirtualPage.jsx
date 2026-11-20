import { useEffect, useRef } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { HiArrowRight, HiOutlineCalendar, HiOutlineLocationMarker } from 'react-icons/hi'
import useAuth from '../../hooks/useAuth'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import ProgressRing from '../../components/ui/ProgressRing'
import Countdown from '../../components/ui/Countdown'
import InfoTip from '../../components/ui/InfoTip'
import { formatEntero, formatFecha } from '../../lib/format'
import {
  FILA_ESTADOS,
  progresoFila,
  tokenFilaVigente,
} from '../../domain/filaVirtual'
import useFilaVirtual from './useFilaVirtual'
import './FilaVirtualPage.css'

const PASOS = ['Entrar', 'Esperar', 'Comprar']

/**
 * Pantalla de fila virtual (alta demanda): una sala de espera con la foto del show de fondo.
 * Un solo protagonista por estado (entrar, esperar, es tu turno, turno usado).
 *
 * Cliente: estado público, turno propio con polling y validación del turno.
 * Admin: además puede fijar la tanda (override `POST .../fila/habilitacion`).
 */
const FilaVirtualPage = () => {
  const { eventoId } = useParams()
  const [params] = useSearchParams()
  const sectorParam = params.get('sector') || ''
  const { token, user } = useAuth()

  const {
    evento,
    estado,
    posicion,
    status,
    loadError,
    error,
    aviso,
    pollWarning,
    busy,
    paseFila,
    hastaPosicion,
    setHastaPosicion,
    elegible,
    puedeUsarFila,
    esAdmin,
    puedeEntrar,
    entrar,
    validar,
    habilitar,
    reload,
  } = useFilaVirtual({ eventoId, token, user, sectorParam })

  const estadoTurno = posicion?.estado || null
  const esMiTurno =
    estadoTurno === FILA_ESTADOS.HABILITADO ||
    estadoTurno === FILA_ESTADOS.CON_TOKEN
  const turnoUsado = estadoTurno === FILA_ESTADOS.TURNO_USADO
  const progreso = posicion
    ? progresoFila(posicion.posicion, posicion.posicionHabilitadaHasta)
    : 0
  const pasoActual = !posicion ? 0 : turnoUsado ? 3 : esMiTurno ? 2 : 1
  const pasePuedeUsarse = Boolean(paseFila && tokenFilaVigente(paseFila))

  useEffect(() => {
    if (aviso) toast.success(aviso)
  }, [aviso])

  const eraMiTurno = useRef(false)
  useEffect(() => {
    if (esMiTurno && !eraMiTurno.current) {
      toast.success('¡Es tu turno! Validalo para comprar.')
    }
    eraMiTurno.current = esMiTurno
  }, [esMiTurno])

  const fondo = evento?.imagenUrl
    ? { '--fila-bg': `url("${evento.imagenUrl}")` }
    : undefined
  const fase = !posicion
    ? 'entrar'
    : turnoUsado
      ? 'usado'
      : esMiTurno
        ? 'turno'
        : 'espera'

  return (
    <section className={`fila-page fila-page--${fase}`} style={fondo}>
      <div className="fila-page__scene" aria-hidden />

      <div className="fila-page__wrap">
        <p className="fila-page__back">
          <Link to={`/eventos/${eventoId}`}>← Volver al evento</Link>
        </p>

        {status === 'loading' && <LoadingState label="Cargando la fila virtual…" />}
        {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

        {status === 'success' && evento && (
          <>
            <header className="fila-page__head">
              <p className="fila-page__eyebrow">
                Fila virtual{' '}
                <InfoTip label="¿Qué es la fila virtual?">
                  Para que nadie se quede afuera por una compra simultánea, los
                  clientes entran en una cola y compran por tandas, en orden de
                  llegada.
                </InfoTip>
              </p>
              <h1>{evento.nombre}</h1>
              <p className="fila-page__meta">
                <span>
                  <HiOutlineCalendar aria-hidden /> {formatFecha(evento.fechaInicio, { dateStyle: 'long' })}
                </span>
                {evento.lugar?.nombre && (
                  <span>
                    <HiOutlineLocationMarker aria-hidden /> {evento.lugar.nombre}
                  </span>
                )}
              </p>
            </header>

            {error && <p className="fila-page__error">{error}</p>}
            {pollWarning && (
              <p className="fila-page__warning" role="status">
                {pollWarning}
              </p>
            )}

            <div className="fila-stage">
              {!elegible && (
                <div className="fila-msg">
                  <h2>Fila virtual no disponible</h2>
                  <p>
                    La fila solo se abre para eventos publicados con alta demanda.
                    Si hay cupo, podés comprar desde el detalle del evento.
                  </p>
                  <Link className="btn btn-secondary" to={`/eventos/${eventoId}`}>
                    Volver al evento
                  </Link>
                </div>
              )}

              {elegible && estado?.abierta === false && (
                <div className="fila-msg">
                  <h2>La fila está cerrada</h2>
                  <p>La venta con fila virtual no está abierta ahora. Probá de nuevo más tarde.</p>
                </div>
              )}

              {elegible && !puedeUsarFila && (
                <div className="fila-msg">
                  <h2>Tu rol no participa de la fila</h2>
                  <p>Solo las cuentas de cliente (y administradores) pueden entrar a la fila de compra.</p>
                </div>
              )}

              {/* 1 · Entrar */}
              {elegible && puedeEntrar && puedeUsarFila && (
                <div className="fila-msg fila-msg--join">
                  <h2>Entrá a la fila</h2>
                  <p>
                    Te damos un turno por orden de llegada. Cuando la tanda llegue
                    a tu posición, vas a poder comprar.
                  </p>
                  <button
                    type="button"
                    className="btn btn-primary btn-lg"
                    onClick={entrar}
                    disabled={busy}
                  >
                    {busy ? 'Ingresando…' : 'Entrar a la fila'} <HiArrowRight aria-hidden />
                  </button>
                </div>
              )}

              {/* 2 · Esperar */}
              {posicion && fase === 'espera' && (
                <div className="fila-wait">
                  <ProgressRing value={progreso} size={220} stroke={14} label="Avance hacia tu turno">
                    <span className="fila-wait__label">Tu posición</span>
                    <span className="fila-wait__pos">#{formatEntero(posicion.posicion)}</span>
                  </ProgressRing>
                  <p className="fila-wait__lead">
                    {posicion.delante > 0
                      ? `Hay ${formatEntero(posicion.delante)} ${
                          posicion.delante === 1 ? 'persona' : 'personas'
                        } antes que vos`
                      : 'Ya casi es tu turno'}
                  </p>
                  <p className="fila-wait__hint">
                    No cierres esta pantalla: la posición se actualiza sola.
                  </p>
                </div>
              )}

              {/* 3 · Es tu turno */}
              {posicion && fase === 'turno' && (
                <div className="fila-turno">
                  <p className="fila-turno__kicker">¡Llegó tu turno!</p>
                  <h2 className="fila-turno__title">Ya podés comprar</h2>
                  {pasePuedeUsarse && (
                    <Countdown
                      expiraEn={paseFila.expiraEn}
                      label="Tu pase vence en"
                      avisoToast="Te quedan 2 minutos para completar la compra."
                    />
                  )}
                  <button
                    type="button"
                    className="btn btn-primary btn-lg"
                    onClick={validar}
                    disabled={busy}
                  >
                    {busy ? 'Validando…' : 'Comprar ahora'} <HiArrowRight aria-hidden />
                  </button>
                  <p className="fila-turno__hint">
                    Tenés 10 minutos para completar la compra
                    {pasePuedeUsarse
                      ? ` (hasta las ${formatFecha(paseFila.expiraEn, { timeStyle: 'short' })})`
                      : ''}
                    .
                  </p>
                </div>
              )}

              {/* 4 · Turno usado */}
              {posicion && fase === 'usado' && (
                <div className="fila-msg">
                  <h2>Ya usaste tu turno</h2>
                  <p>
                    Si la compra no se completó, revisá{' '}
                    <Link to="/mis-entradas">Mis entradas</Link> o volvé al detalle
                    del evento.
                  </p>
                  <Link className="btn btn-secondary" to={`/eventos/${eventoId}`}>
                    Volver al evento
                  </Link>
                </div>
              )}
            </div>

            {elegible && puedeUsarFila && (
              <ol className="fila-dots" aria-label="Pasos">
                {PASOS.map((p, i) => (
                  <li
                    key={p}
                    className={
                      i < pasoActual ? 'is-done' : i === pasoActual ? 'is-current' : ''
                    }
                  >
                    <span className="fila-dots__dot" aria-hidden />
                    {p}
                  </li>
                ))}
              </ol>
            )}

            {elegible && (
              <p className="fila-facts">
                <span>
                  <strong>{formatEntero(estado?.totalEnFila ?? '—')}</strong> en la fila
                </span>
                <span>
                  Habilitados hasta el <strong>#{formatEntero(estado?.posicionHabilitadaHasta ?? '—')}</strong>
                </span>
              </p>
            )}

            {esAdmin && elegible && (
              <details className="fila-admin">
                <summary>Override de tanda (ADMIN)</summary>
                <p className="fila-page__muted">
                  Fija hasta qué posición se habilita la compra. La tanda nunca
                  puede retroceder; el job automático sigue avanzando por cupo.
                </p>
                <div className="fila-admin__row">
                  <label htmlFor="hastaPosicion">Habilitar hasta</label>
                  <input
                    id="hastaPosicion"
                    className="form-input fila-admin__input"
                    type="number"
                    min="1"
                    step="1"
                    value={hastaPosicion}
                    onChange={(e) => setHastaPosicion(e.target.value)}
                    placeholder={String(
                      Number(estado?.posicionHabilitadaHasta || 0) + 25
                    )}
                    disabled={busy}
                  />
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={habilitar}
                    disabled={busy}
                  >
                    {busy ? 'Aplicando…' : 'Habilitar tanda'}
                  </button>
                </div>
              </details>
            )}
          </>
        )}
      </div>
    </section>
  )
}

export default FilaVirtualPage
