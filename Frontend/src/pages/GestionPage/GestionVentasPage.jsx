import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import {
  listColaAdminVentas,
  marcarReintegroEntradaCancelada,
  resolverDevolucion,
} from '../../api/ventas'
import {
  DEVOLUCION_ESTADOS,
  DEVOLUCION_TIPO_LABELS,
} from '../../domain/devolucion'
import { ENTRADA_ESTADO_LABELS } from '../../domain/entrada'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { formatFecha, formatMoney } from '../../lib/format'
import '../GestionPage/GestionPage.css'

const COLA_VACIA = {
  devoluciones: [],
  eventosCancelados: [],
  entradasEventosCancelados: [],
}

/**
 * Admin ventas: solo cola operativa
 * - devoluciones / reclamos pendientes
 * - entradas de eventos cancelados
 * (no el listado completo de compras)
 */
const GestionVentasPage = () => {
  const { user, token } = useAuth()
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(
    async (signal) => {
      const data = await listColaAdminVentas(user, { token, signal })
      return {
        devoluciones: data?.devoluciones || [],
        eventosCancelados: data?.eventosCancelados || [],
        entradasEventosCancelados: data?.entradasEventosCancelados || [],
      }
    },
    [user, token]
  )

  const {
    data: cola = COLA_VACIA,
    status,
    error: loadError,
    reload,
  } = useAsync(load, [user?.id, token], {
    enabled: Boolean(user?.id && token),
  })

  const entradasPorEvento = useMemo(() => {
    const map = new Map()
    for (const ev of cola.eventosCancelados) {
      map.set(ev.id, {
        evento: ev,
        entradas: [],
      })
    }
    for (const ent of cola.entradasEventosCancelados) {
      const bucket = map.get(ent.idEvento) || {
        evento: {
          id: ent.idEvento,
          nombre: ent.eventoNombre,
          fechaInicio: ent.eventoFecha,
          estado: 'CANCELADO',
        },
        entradas: [],
      }
      bucket.entradas.push(ent)
      map.set(ent.idEvento, bucket)
    }
    return [...map.values()]
  }, [cola])

  const onResolver = async (id, estado) => {
    if (!FEATURE.adminColaVentas) return
    setBusyId(id)
    setError('')
    try {
      await resolverDevolucion(id, estado, user, { token })
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo resolver')
    } finally {
      setBusyId(null)
    }
  }

  const onReintegroEvento = async (idEvento) => {
    if (!FEATURE.adminReintegroEvento) return
    setBusyId(idEvento)
    setError('')
    try {
      await marcarReintegroEntradaCancelada(idEvento, user, {
        token,
        idEvento,
      })
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo marcar reintegro')
    } finally {
      setBusyId(null)
    }
  }

  const empty =
    status === 'success' &&
    cola.devoluciones.length === 0 &&
    cola.entradasEventosCancelados.length === 0 &&
    cola.eventosCancelados.length === 0

  return (
    <section className="gestion-page container section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / ventas
        </p>
        <h1 className="gestion-page__title">Cola de ventas</h1>
        <p className="gestion-page__sub">
          Solo <strong>reclamos / devoluciones pendientes</strong> y{' '}
          <strong>reintegro por evento cancelado</strong> (API masiva). No se
          listan todas las compras.
          {!FEATURE.adminColaVentas && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <div className="gestion-card__actions gestion-card__actions--spaced">
        <GateButton
          enabled={FEATURE.adminColaVentas}
          type="button"
          className="btn"
          onClick={reload}
        >
          Actualizar
        </GateButton>
      </div>

      {error && <p className="gestion-error">{error}</p>}
      {status === 'loading' && <LoadingState label="Cargando cola…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

      {empty && (
        <div className="gestion-card">
          <p>No hay ítems en la cola.</p>
          <p className="gestion-card__muted">
            Cuando haya devoluciones pendientes o se cancele un evento con
            entradas vendidas, aparecen acá.
          </p>
        </div>
      )}

      {cola.devoluciones.length > 0 && (
        <section className="gestion-section--spaced">
          <h2 className="gestion-page__title gestion-section__title">
            Devoluciones y reclamos
          </h2>
          <ul className="gestion-event-list">
            {cola.devoluciones.map((d) => (
              <li key={d.id} className="gestion-event-row">
                <div className="gestion-event-row__main">
                  <h2>
                    {DEVOLUCION_TIPO_LABELS[d.tipo] || d.tipo}
                    {' · '}
                    {d.eventoNombre || d.idEvento}
                  </h2>
                  <p>
                    {d.clienteEmail || d.idCliente}
                    {' · '}
                    {formatFecha(d.fechaSolicitud)}
                    {d.monto != null ? ` · ${formatMoney(d.monto)}` : ''}
                  </p>
                  <p className="gestion-card__muted">
                    entrada {d.idEntrada}
                    {d.idCompra ? ` · compra ${d.idCompra}` : ''}
                  </p>
                  {d.motivo && <p>{d.motivo}</p>}
                </div>
                <div className="gestion-event-row__actions">
                  {d.idEntrada && (
                    <Link
                      className="btn"
                      to={`/gestion/entradas/${d.idEntrada}`}
                    >
                      Ver entrada
                    </Link>
                  )}
                  <GateButton
                    enabled={FEATURE.adminColaVentas}
                    type="button"
                    className="btn btn-primary"
                    disabled={busyId === d.id}
                    onClick={() => onResolver(d.id, DEVOLUCION_ESTADOS.APROBADA)}
                  >
                    Aprobar
                  </GateButton>
                  <GateButton
                    enabled={FEATURE.adminColaVentas}
                    type="button"
                    className="btn"
                    disabled={busyId === d.id}
                    onClick={() => onResolver(d.id, DEVOLUCION_ESTADOS.RECHAZADA)}
                  >
                    Rechazar
                  </GateButton>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {entradasPorEvento.length > 0 && (
        <section>
          <h2 className="gestion-page__title gestion-section__title">
            Eventos cancelados — reintegro
          </h2>
          {entradasPorEvento.map(({ evento, entradas }) => (
            <div key={evento.id} className="gestion-event-group">
              <div className="gestion-card gestion-card--spaced">
                <h2 className="gestion-section__title">{evento.nombre}</h2>
                <p className="gestion-card__muted">
                  {evento.id} · {formatFecha(evento.fechaInicio)} · CANCELADO ·{' '}
                  {entradas.length} entrada(s)
                </p>
                <div className="gestion-card__actions gestion-card__actions--top">
                  <GateButton
                    enabled={FEATURE.adminReintegroEvento}
                    type="button"
                    className="btn btn-primary"
                    disabled={busyId === evento.id}
                    title={
                      FEATURE.adminReintegroEvento
                        ? 'POST /api/eventos/{id}/reintegro-cancelacion'
                        : GATE_HINT
                    }
                    onClick={() => onReintegroEvento(evento.id)}
                  >
                    Reintegrar evento
                  </GateButton>
                </div>
              </div>
              {entradas.length === 0 ? (
                <p className="gestion-card__muted">Sin entradas en cola local.</p>
              ) : (
                <ul className="gestion-event-list">
                  {entradas.map((e) => (
                    <li key={e.id} className="gestion-event-row">
                      <div className="gestion-event-row__main">
                        <h2>{e.id}</h2>
                        <p>
                          cliente {e.idCliente}
                          {' · '}
                          {e.sectorNombre}
                          {' · '}
                          {formatMoney(e.precio)}
                        </p>
                        <p className="gestion-card__muted">
                          {ENTRADA_ESTADO_LABELS[e.estado] || e.estado}
                        </p>
                      </div>
                      <div className="gestion-event-row__actions">
                        <GateButton
                          enabled={FEATURE.adminReintegroEntrada}
                          type="button"
                          className="btn"
                          disabled
                          title="No hay endpoint BE por entrada suelta — usá reintegro del evento"
                        >
                          Reintegro unitario
                        </GateButton>
                        <Link className="btn" to={`/gestion/entradas/${e.id}`}>
                          Ver entrada
                        </Link>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </section>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion">← Panel</Link>
      </p>
    </section>
  )
}

export default GestionVentasPage
