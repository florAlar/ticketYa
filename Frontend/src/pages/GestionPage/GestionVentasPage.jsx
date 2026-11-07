import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
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
import '../GestionPage/GestionPage.css'

function formatFecha(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

function formatMoney(n) {
  try {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(n ?? 0)
  } catch {
    return `$${n ?? 0}`
  }
}

/**
 * Admin ventas: solo cola operativa
 * - devoluciones / reclamos pendientes
 * - entradas de eventos cancelados
 * (no el listado completo de compras)
 */
const GestionVentasPage = () => {
  const { user, token } = useAuth()
  const [cola, setCola] = useState({
    devoluciones: [],
    eventosCancelados: [],
    entradasEventosCancelados: [],
  })
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await listColaAdminVentas(user, { token })
      setCola({
        devoluciones: data?.devoluciones || [],
        eventosCancelados: data?.eventosCancelados || [],
        entradasEventosCancelados: data?.entradasEventosCancelados || [],
      })
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar')
      setStatus('error')
    }
  }, [user, token])

  useEffect(() => {
    load()
  }, [load])

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
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo resolver')
    } finally {
      setBusyId(null)
    }
  }

  const onReintegro = async (entradaId) => {
    if (!FEATURE.adminColaVentas) return
    setBusyId(entradaId)
    setError('')
    try {
      await marcarReintegroEntradaCancelada(entradaId, user, { token })
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo marcar reintegro')
    } finally {
      setBusyId(null)
    }
  }

  const empty =
    status === 'ok' &&
    cola.devoluciones.length === 0 &&
    cola.entradasEventosCancelados.length === 0

  return (
    <section className="gestion-page container section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / ventas
        </p>
        <h1 className="gestion-page__title">Cola de ventas</h1>
        <p className="gestion-page__sub">
          Solo <strong>reclamos / devoluciones pendientes</strong> y{' '}
          <strong>entradas de eventos cancelados</strong>. No se listan todas
          las compras.
          {!FEATURE.adminColaVentas && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <div className="gestion-card__actions" style={{ marginBottom: '1rem' }}>
        <button type="button" className="btn" onClick={load}>
          Actualizar
        </button>
      </div>

      {error && <p className="gestion-error">{error}</p>}
      {status === 'loading' && <p className="gestion-card__muted">Cargando…</p>}

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
        <section style={{ marginBottom: '2rem' }}>
          <h2 className="gestion-page__title" style={{ fontSize: '1.15rem' }}>
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
          <h2 className="gestion-page__title" style={{ fontSize: '1.15rem' }}>
            Eventos cancelados — entradas a reintegrar
          </h2>
          {entradasPorEvento.map(({ evento, entradas }) => (
            <div key={evento.id} style={{ marginBottom: '1.25rem' }}>
              <div className="gestion-card" style={{ marginBottom: '0.65rem' }}>
                <h2 style={{ margin: 0, fontSize: '1.05rem' }}>{evento.nombre}</h2>
                <p className="gestion-card__muted">
                  {evento.id} · {formatFecha(evento.fechaInicio)} · CANCELADO ·{' '}
                  {entradas.length} entrada(s)
                </p>
              </div>
              {entradas.length === 0 ? (
                <p className="gestion-card__muted">Sin entradas vendidas pendientes.</p>
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
                          {' · '}
                          {e.codigoQR}
                        </p>
                      </div>
                      <div className="gestion-event-row__actions">
                        <GateButton
                          enabled={FEATURE.adminColaVentas}
                          type="button"
                          className="btn btn-primary"
                          disabled={busyId === e.id}
                          onClick={() => onReintegro(e.id)}
                        >
                          Marcar reintegro
                        </GateButton>
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
