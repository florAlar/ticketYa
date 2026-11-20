import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { listEventosGestion, getDisponibilidadBatch } from '../../api/eventos'
import { listUsuarios } from '../../api/usuarios'
import { listColaAdminVentas } from '../../api/ventas'
import { FEATURE } from '../../config/featureGates'
import { EVENTO_ESTADOS, EVENTO_ESTADO_LABELS, normalizeDisponibilidadDto } from '../../domain/evento'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { PageHeader, Pill } from '../../components/gestion/gestionUi'
import { formatEntero, formatFecha } from '../../lib/format'
import './GestionPage.css'

const TONO_ESTADO = {
  [EVENTO_ESTADOS.PUBLICADO]: 'success',
  [EVENTO_ESTADOS.BORRADOR]: 'neutral',
  [EVENTO_ESTADOS.SUSPENDIDO]: 'warning',
  [EVENTO_ESTADOS.CANCELADO]: 'danger',
  [EVENTO_ESTADOS.AGOTADO]: 'danger',
}

const num = (v) => (Number.isFinite(Number(v)) ? Number(v) : 0)

/** Resumen de gestión: indicadores, ocupación, lo que requiere atención y próximos eventos. */
const GestionPage = () => {
  const { user, token, isAdmin, canManageAllEvents, canManageStaff, canManageSalesOps } = useAuth()

  const load = useCallback(
    async (signal) => {
      const raw = await listEventosGestion(user, { isAdmin: canManageAllEvents, token, signal })
      const eventos = Array.isArray(raw) ? raw : raw?.content || raw?.items || []

      // Disponibilidad de los publicados (entradas vendidas / capacidad)
      const publicados = eventos.filter((e) => e.estado === EVENTO_ESTADOS.PUBLICADO)
      const cupos = new Map()
      try {
        const ids = publicados.map((e) => e.id).filter(Boolean)
        if (ids.length) {
          const batch = await getDisponibilidadBatch(ids, { signal })
          const items = Array.isArray(batch?.items) ? batch.items : Array.isArray(batch) ? batch : []
          for (const it of items) {
            const d = normalizeDisponibilidadDto(it)
            if (d?.idEvento) cupos.set(String(d.idEvento), d)
          }
        }
      } catch (err) {
        if (err?.name === 'AbortError') throw err
      }

      let usuarios = null
      if (canManageStaff && FEATURE.adminUsuariosListado) {
        try {
          const u = await listUsuarios(user, { token, signal })
          usuarios = Array.isArray(u) ? u : u?.content || []
        } catch (err) {
          if (err?.name === 'AbortError') throw err
        }
      }
      let cola = null
      if (canManageSalesOps && FEATURE.adminColaVentas) {
        try {
          cola = await listColaAdminVentas(user, { token, signal })
        } catch (err) {
          if (err?.name === 'AbortError') throw err
        }
      }
      return { eventos, cupos, usuarios, cola }
    },
    [user, token, canManageAllEvents, canManageStaff, canManageSalesOps]
  )

  const { data, status, error, reload } = useAsync(load, [user?.id, token], {
    enabled: Boolean(user?.id && token && FEATURE.eventosGestion),
  })

  const eventos = data?.eventos || []
  const cupos = data?.cupos || new Map()
  const publicados = eventos.filter((e) => e.estado === EVENTO_ESTADOS.PUBLICADO)

  let vendidas = 0
  let libres = 0
  const ocupacion = []
  for (const e of publicados) {
    const d = cupos.get(String(e.id))
    if (!d) continue
    const sec = d.sectores || []
    const cap = sec.reduce((a, s) => a + num(s.capacidadTotal), 0)
    const ven = sec.reduce((a, s) => a + num(s.cantidadVendida), 0)
    vendidas += ven
    libres += num(d.disponibleTotal)
    if (cap > 0) ocupacion.push({ id: e.id, nombre: e.nombre, vendidas: ven, cap, pct: (ven / cap) * 100 })
  }
  ocupacion.sort((a, b) => b.pct - a.pct)

  const proximos = [...publicados]
    .filter((e) => new Date(e.fechaInicio) >= new Date())
    .sort((a, b) => new Date(a.fechaInicio) - new Date(b.fechaInicio))
    .slice(0, 6)

  const atencion = []
  for (const e of eventos) {
    if (e.estado === EVENTO_ESTADOS.SUSPENDIDO) {
      atencion.push({ key: `s-${e.id}`, texto: `«${e.nombre}» está suspendido`, to: '/gestion/eventos', accion: 'Revisar' })
    }
    if (e.estado === EVENTO_ESTADOS.PUBLICADO && cupos.get(String(e.id))?.agotado) {
      atencion.push({ key: `a-${e.id}`, texto: `«${e.nombre}» está agotado`, to: `/gestion/eventos/${e.id}/reportes`, accion: 'Ver reporte' })
    }
  }
  const pendientes = data?.cola?.devoluciones?.length || 0
  if (pendientes > 0) {
    atencion.unshift({ key: 'dev', texto: `${pendientes} devolución(es) o reclamo(s) pendientes`, to: '/gestion/ventas', accion: 'Resolver' })
  }

  const usuarios = data?.usuarios
  const activos = usuarios ? usuarios.filter((u) => u.activo !== false).length : null

  return (
    <>
      <PageHeader
        title="Resumen"
        subtitle={`Hola, ${user?.nombre || user?.email}. ${
          isAdmin ? 'Esto es lo que pasa en toda la plataforma.' : 'Esto es lo que pasa con tus eventos.'
        }`}
      >
        <Link className="btn btn-primary" to="/gestion/eventos/nuevo">
          Nuevo evento
        </Link>
      </PageHeader>

      {status === 'loading' && <LoadingState label="Cargando resumen…" />}
      {status === 'error' && <ErrorState error={error} onRetry={reload} />}

      {status === 'success' && (
        <>
          <div className="g-kpis">
            <article className="g-kpi">
              <span className="g-kpi__label">Eventos publicados</span>
              <span className="g-kpi__value">{formatEntero(publicados.length)}</span>
              <span className="g-kpi__hint">de {formatEntero(eventos.length)} en total</span>
            </article>
            <article className="g-kpi">
              <span className="g-kpi__label">Entradas vendidas</span>
              <span className="g-kpi__value">{formatEntero(vendidas)}</span>
              <span className="g-kpi__hint">en eventos publicados</span>
            </article>
            <article className="g-kpi">
              <span className="g-kpi__label">Cupo disponible</span>
              <span className="g-kpi__value">{formatEntero(libres)}</span>
              <span className="g-kpi__hint">entradas a la venta</span>
            </article>
            {activos != null && (
              <article className="g-kpi">
                <span className="g-kpi__label">Usuarios activos</span>
                <span className="g-kpi__value">{formatEntero(activos)}</span>
                <span className="g-kpi__hint">de {formatEntero(usuarios.length)} cuentas</span>
              </article>
            )}
            {data?.cola && (
              <article className="g-kpi">
                <span className="g-kpi__label">Pendientes</span>
                <span className="g-kpi__value">{formatEntero(pendientes)}</span>
                <span className="g-kpi__hint">devoluciones y reclamos</span>
              </article>
            )}
          </div>

          <div className="g-grid-2">
            <section className="g-card">
              <h2 className="g-card__title">Ocupación por evento</h2>
              {ocupacion.length === 0 ? (
                <p className="g-muted">Todavía no hay datos de ventas.</p>
              ) : (
                <ul className="g-bars">
                  {ocupacion.slice(0, 7).map((o) => (
                    <li key={o.id}>
                      <span className="g-bars__name">{o.nombre}</span>
                      <span className="g-bars__val">
                        {formatEntero(o.vendidas)} / {formatEntero(o.cap)} · {o.pct < 1 && o.pct > 0 ? '<1' : Math.round(o.pct)}%
                      </span>
                      <div className="g-bars__track" aria-hidden>
                        <span style={{ width: `${Math.max(o.pct, o.pct > 0 ? 2 : 0)}%` }} />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="g-card">
              <h2 className="g-card__title">Requiere atención</h2>
              {atencion.length === 0 ? (
                <p className="g-muted">Todo en orden. No hay nada pendiente.</p>
              ) : (
                <ul className="g-attn">
                  {atencion.slice(0, 8).map((a) => (
                    <li key={a.key}>
                      <span>{a.texto}</span>
                      <Link to={a.to}>{a.accion}</Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>

          <h2 className="g-card__title" style={{ margin: '1.5rem 0 0.75rem' }}>
            Próximos eventos
          </h2>
          {proximos.length === 0 ? (
            <div className="g-empty">No hay eventos publicados próximos.</div>
          ) : (
            <div className="g-table-wrap">
              <table className="g-table">
                <thead>
                  <tr>
                    <th>Evento</th>
                    <th>Fecha</th>
                    <th>Lugar</th>
                    <th>Estado</th>
                    <th className="is-num">Vendidas</th>
                  </tr>
                </thead>
                <tbody>
                  {proximos.map((e) => {
                    const d = cupos.get(String(e.id))
                    const ven = (d?.sectores || []).reduce((a, s) => a + num(s.cantidadVendida), 0)
                    return (
                      <tr key={e.id}>
                        <td>
                          <div className="g-cell-main">
                            <strong>
                              <Link to={`/gestion/eventos/${e.id}/reportes`}>{e.nombre}</Link>
                            </strong>
                          </div>
                        </td>
                        <td>{formatFecha(e.fechaInicio, { dateStyle: 'medium' })}</td>
                        <td>{e.lugar?.nombre || e.lugarCiudad || '—'}</td>
                        <td>
                          <Pill tone={TONO_ESTADO[e.estado] || 'neutral'}>
                            {EVENTO_ESTADO_LABELS[e.estado] || e.estado}
                          </Pill>
                        </td>
                        <td className="is-num">{formatEntero(ven)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  )
}

export default GestionPage
