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
import { PageHeader, Pill, RowMenu, Toolbar, VacioTabla } from '../../components/gestion/gestionUi'
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
    <>
      <PageHeader
        title="Cola de ventas"
        subtitle="Devoluciones y reclamos pendientes, y reintegros de eventos cancelados."
      >
        <GateButton enabled={FEATURE.adminColaVentas} type="button" className="btn btn-secondary" onClick={reload}>
          Actualizar
        </GateButton>
      </PageHeader>

      {!FEATURE.adminColaVentas && <VacioTabla>La cola de ventas no está habilitada en esta build. {GATE_HINT}</VacioTabla>}
      {error && <p className="g-error">{error}</p>}
      {status === 'loading' && <LoadingState label="Cargando cola…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

      {empty && <VacioTabla>No hay nada pendiente. Cuando haya devoluciones o se cancele un evento con entradas vendidas, aparecen acá.</VacioTabla>}

      {cola.devoluciones.length > 0 && (
        <section style={{ marginBottom: '1.75rem' }}>
          <h2 className="g-card__title">Devoluciones y reclamos ({cola.devoluciones.length})</h2>
          <div className="g-table-wrap">
            <table className="g-table">
              <thead>
                <tr>
                  <th>Solicitud</th>
                  <th>Cliente</th>
                  <th>Fecha</th>
                  <th className="is-num">Monto</th>
                  <th className="is-actions">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {cola.devoluciones.map((d) => (
                  <tr key={d.id}>
                    <td>
                      <div className="g-cell-main">
                        <strong>
                          {DEVOLUCION_TIPO_LABELS[d.tipo] || d.tipo}{d.eventoNombre ? ` · ${d.eventoNombre}` : ""}
                        </strong>
                        {d.motivo && <span>{d.motivo}</span>}
                      </div>
                    </td>
                    <td>{d.clienteEmail || String(d.idCliente || "—").slice(0, 8)}</td>
                    <td>{formatFecha(d.fechaSolicitud, { dateStyle: 'medium' })}</td>
                    <td className="is-num">{d.monto != null ? formatMoney(d.monto) : '—'}</td>
                    <td className="is-actions">
                      <div className="g-row-actions">
                        <GateButton
                          enabled={FEATURE.adminColaVentas}
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={busyId === d.id}
                          onClick={() => onResolver(d.id, DEVOLUCION_ESTADOS.APROBADA)}
                        >
                          Aprobar
                        </GateButton>
                        <GateButton
                          enabled={FEATURE.adminColaVentas}
                          type="button"
                          className="btn btn-secondary btn-sm"
                          disabled={busyId === d.id}
                          onClick={() => onResolver(d.id, DEVOLUCION_ESTADOS.RECHAZADA)}
                        >
                          Rechazar
                        </GateButton>
                        {d.idEntrada && <RowMenu items={[{ label: 'Ver entrada', to: '/gestion/entradas/' + d.idEntrada }]} />}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {entradasPorEvento.length > 0 && (
        <section>
          <h2 className="g-card__title">Eventos cancelados: reintegro</h2>
          {entradasPorEvento.map(({ evento, entradas }) => (
            <div key={evento.id} style={{ marginBottom: '1.5rem' }}>
              <div className="g-toolbar" style={{ marginBottom: '0.6rem' }}>
                <div className="g-cell-main">
                  <strong>{evento.nombre}</strong>
                  <span>
                    {formatFecha(evento.fechaInicio, { dateStyle: 'medium' })} · {entradas.length} entrada(s)
                  </span>
                </div>
                <div className="g-toolbar__right">
                  <GateButton
                    enabled={FEATURE.adminReintegroEvento}
                    type="button"
                    className="btn btn-primary btn-sm"
                    disabled={busyId === evento.id}
                    title={FEATURE.adminReintegroEvento ? undefined : GATE_HINT}
                    onClick={() => onReintegroEvento(evento.id)}
                  >
                    Reintegrar evento
                  </GateButton>
                </div>
              </div>
              {entradas.length === 0 ? (
                <p className="g-muted">Sin entradas en cola local.</p>
              ) : (
                <div className="g-table-wrap">
                  <table className="g-table">
                    <thead>
                      <tr>
                        <th>Entrada</th>
                        <th>Cliente</th>
                        <th>Sector</th>
                        <th className="is-num">Precio</th>
                        <th>Estado</th>
                        <th className="is-actions">Acciones</th>
                      </tr>
                    </thead>
                    <tbody>
                      {entradas.map((e) => (
                        <tr key={e.id}>
                          <td className="g-muted">{String(e.id).slice(0, 8)}</td>
                          <td>{String(e.idCliente || "—").slice(0, 8)}</td>
                          <td>{e.sectorNombre}</td>
                          <td className="is-num">{formatMoney(e.precio)}</td>
                          <td>
                            <Pill tone="neutral">{ENTRADA_ESTADO_LABELS[e.estado] || e.estado}</Pill>
                          </td>
                          <td className="is-actions">
                            <RowMenu items={[{ label: 'Ver entrada', to: '/gestion/entradas/' + e.id }]} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          ))}
        </section>
      )}
    </>
  )
}

export default GestionVentasPage
