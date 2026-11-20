import { Link, useParams, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { formatEntero, formatFecha } from '../../lib/format'
import {
  FILA_ESTADOS,
  FILA_ESTADO_LABELS,
  progresoFila,
  tokenFilaVigente,
} from '../../domain/filaVirtual'
import useFilaVirtual from './useFilaVirtual'
import './FilaVirtualPage.css'

/**
 * Pantalla de fila virtual (alta demanda).
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

  return (
    <section className="fila-page container-slim section-v-padding">
      <p className="fila-page__back">
        <Link to={`/eventos/${eventoId}`}>← Volver al evento</Link>
      </p>

      {status === 'loading' && <LoadingState label="Cargando la fila virtual…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

      {status === 'success' && evento && (
        <>
          <header className="fila-page__head">
            <p className="fila-page__eyebrow">Alta demanda · Fila virtual</p>
            <h1>{evento.nombre}</h1>
            <p className="fila-page__meta">
              {formatFecha(evento.fechaInicio)}
              {evento.lugar?.nombre ? ` · ${evento.lugar.nombre}` : ''}
              {sectorParam ? ' · volvés al sector que elegiste' : ''}
            </p>
          </header>

          {!elegible && (
            <div className="fila-card fila-card--muted">
              <h2>Fila virtual no disponible</h2>
              <p>
                La fila solo se abre para eventos <strong>PUBLICADOS</strong> con
                alta demanda. Podés comprar desde el detalle del evento si hay
                cupo disponible.
              </p>
              <Link className="btn" to={`/eventos/${eventoId}`}>
                Volver al evento
              </Link>
            </div>
          )}

          {elegible && estado?.abierta === false && (
            <div className="fila-card fila-card--muted">
              <h2>La fila está cerrada</h2>
              <p>
                La venta con fila virtual no está abierta en este momento. Probá
                de nuevo más tarde.
              </p>
            </div>
          )}

          {elegible && (
            <div className="fila-stats">
              <div className="fila-stat">
                <span className="fila-stat__label">En la fila</span>
                <strong>{formatEntero(estado?.totalEnFila ?? '—')}</strong>
                <span className="fila-stat__hint">personas sin turno usado</span>
              </div>
              <div className="fila-stat">
                <span className="fila-stat__label">Habilitados hasta</span>
                <strong>
                  {formatEntero(estado?.posicionHabilitadaHasta ?? '—')}
                </strong>
                <span className="fila-stat__hint">posición de la tanda</span>
              </div>
            </div>
          )}

          {error && <p className="fila-page__error">{error}</p>}
          {aviso && <p className="fila-page__aviso">{aviso}</p>}
          {pollWarning && (
            <p className="fila-page__warning" role="status">
              {pollWarning}
            </p>
          )}

          {elegible && puedeEntrar && puedeUsarFila && (
            <div className="fila-card fila-card--join">
              <h2>Entrá a la fila</h2>
              <p>
                Te asignamos un turno por orden de llegada. Vas a poder comprar
                cuando la tanda habilitada llegue a tu posición. La pantalla se
                actualiza sola cada pocos segundos.
              </p>
              <button
                type="button"
                className="btn btn-primary"
                onClick={entrar}
                disabled={busy}
              >
                {busy ? 'Ingresando…' : 'Entrar a la fila virtual'}
              </button>
            </div>
          )}

          {elegible && !puedeUsarFila && (
            <div className="fila-card fila-card--muted">
              <h2>Tu rol no participa de la fila</h2>
              <p>
                Solo las cuentas de cliente (y administradores) pueden ingresar a
                la fila virtual de compra.
              </p>
            </div>
          )}

          {posicion && (
            <div
              className={`fila-card fila-card--turno fila-card--${String(
                estadoTurno || ''
              ).toLowerCase()}`}
            >
              <div className="fila-turno__head">
                <div>
                  <p className="fila-page__eyebrow">Tu turno</p>
                  <p className="fila-turno__posicion">
                    #{formatEntero(posicion.posicion)}
                  </p>
                </div>
                <span
                  className={`fila-badge fila-badge--${String(
                    estadoTurno || ''
                  ).toLowerCase()}`}
                >
                  {FILA_ESTADO_LABELS[estadoTurno] || estadoTurno}
                </span>
              </div>

              {!turnoUsado && (
                <>
                  <div
                    className="fila-progress"
                    role="progressbar"
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={progreso}
                    aria-label="Avance de la tanda"
                  >
                    <span
                      className="fila-progress__bar"
                      style={{ width: `${progreso}%` }}
                    />
                  </div>
                  <p className="fila-page__muted">
                    Tanda hasta la posición{' '}
                    {formatEntero(posicion.posicionHabilitadaHasta)}
                    {' · '}
                    {posicion.delante > 0
                      ? `faltan ${formatEntero(posicion.delante)} turnos`
                      : 'ya podés comprar'}
                  </p>
                </>
              )}

              {esMiTurno && (
                <div className="fila-turno__cta">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={validar}
                    disabled={busy}
                  >
                    {busy ? 'Validando…' : 'Validar turno y comprar'}
                  </button>
                  {paseFila && tokenFilaVigente(paseFila) && (
                    <p className="fila-page__muted">
                      Pase de compra vigente hasta{' '}
                      {formatFecha(paseFila.expiraEn, {
                        timeStyle: 'short',
                      })}
                      . Tenés 10 minutos para completar la compra.
                    </p>
                  )}
                </div>
              )}

              {estadoTurno === FILA_ESTADOS.EN_ESPERA && (
                <p className="fila-page__muted">
                  No cierres esta pantalla: el turno se mantiene y la posición se
                  actualiza automáticamente.
                </p>
              )}

              {turnoUsado && (
                <div className="fila-turno__cta">
                  <p>
                    Ya usaste el turno de este evento. Si la compra no se
                    completó, revisá{' '}
                    <Link to="/mis-entradas">Mis entradas</Link> o volvé al{' '}
                    <Link to={`/eventos/${eventoId}`}>detalle del evento</Link>.
                  </p>
                </div>
              )}
            </div>
          )}

          {esAdmin && elegible && (
            <div className="fila-card fila-admin">
              <h2>Override de tanda (ADMIN)</h2>
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
            </div>
          )}

          <p className="fila-page__foot">
            Estado público de la fila: se actualiza cada 5 segundos.
            {elegible && puedeUsarFila && !posicion
              ? ' Ingresá para obtener tu turno.'
              : ''}
          </p>
        </>
      )}
    </section>
  )
}

export default FilaVirtualPage
