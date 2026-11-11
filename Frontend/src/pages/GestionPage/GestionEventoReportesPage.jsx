import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getReporteEvento } from '../../api/reportes'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import './GestionPage.css'

const MENSAJES_ERROR = {
  403: 'No tenés permiso para ver los reportes de este evento (solo su organizador o un ADMIN).',
  404: 'El evento no existe.',
  503: 'No se pudo armar el reporte: otro servicio no está disponible. Probá de nuevo en un momento.',
}

function dinero(valor, moneda) {
  const n = Number(valor)
  if (!Number.isFinite(n)) return '—'
  try {
    return n.toLocaleString('es-AR', {
      style: 'currency',
      currency: moneda || 'ARS',
      maximumFractionDigits: 0,
    })
  } catch {
    return `$${n}`
  }
}

const pct = (v) => `${Number(v || 0).toLocaleString('es-AR', { maximumFractionDigits: 2 })} %`
const entero = (v) => Number(v || 0).toLocaleString('es-AR')

const Metrica = ({ etiqueta, valor, nota }) => (
  <div className="reporte-metrica">
    <span className="reporte-metrica__valor">{valor}</span>
    <span className="reporte-metrica__etiqueta">{etiqueta}</span>
    {nota ? <span className="reporte-metrica__nota">{nota}</span> : null}
  </div>
)

/**
 * Reportes del organizador para un evento: vendidas/devueltas por sector, ocupación y recaudación.
 * GET /api/reportes/eventos/{id}.
 */
const GestionEventoReportesPage = () => {
  const { eventoId } = useParams()
  const { token } = useAuth()
  const wired = FEATURE.reportes
  const [reporte, setReporte] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(wired)

  useEffect(() => {
    if (!wired) return undefined
    let vivo = true
    getReporteEvento(eventoId, { token })
      .then((data) => {
        if (!vivo) return
        setReporte(data)
        setError('')
      })
      .catch((err) => {
        if (!vivo) return
        setReporte(null)
        setError(MENSAJES_ERROR[err.status] || err.message || 'No se pudo cargar el reporte')
      })
      .finally(() => {
        if (vivo) setCargando(false)
      })
    return () => {
      vivo = false
    }
  }, [eventoId, token, wired])

  const t = reporte?.total
  const moneda = reporte?.moneda

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / <Link to="/gestion/eventos">eventos</Link> / reportes
        </p>
        <h1 className="gestion-page__title">
          {reporte?.nombre ? `Reportes — ${reporte.nombre}` : 'Reportes del evento'}
        </h1>
        <p className="gestion-page__sub">
          Entradas vendidas y devueltas por sector, ocupación y recaudación.
          {!wired && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {!wired && (
        <div className="gestion-card">
          <p>Los reportes necesitan el backend real: no hay versión mock.</p>
        </div>
      )}

      {wired && cargando && <p className="gestion-card__muted">Cargando reporte…</p>}
      {wired && error && <p className="gestion-error" role="alert">{error}</p>}

      {reporte && t && (
        <>
          <div className="gestion-card reporte-metricas">
            <Metrica etiqueta="Compras aprobadas" valor={entero(reporte.compras)} />
            <Metrica etiqueta="Entradas vendidas" valor={entero(t.vendidas)} />
            <Metrica etiqueta="Devueltas" valor={entero(t.devueltas)} />
            <Metrica
              etiqueta="Vigentes"
              valor={entero(t.vigentes)}
              nota={`${entero(t.ingresaron)} ya ingresaron`}
            />
            <Metrica etiqueta="Ocupación" valor={pct(t.ocupacionPct)} nota={`de ${entero(t.capacidad)} lugares`} />
            <Metrica etiqueta="Recaudación neta" valor={dinero(t.recaudacionNeta, moneda)} />
          </div>

          <div className="gestion-card reporte-tabla-wrap">
            <h2>Por sector</h2>
            <table className="reporte-tabla">
              <thead>
                <tr>
                  <th>Sector</th>
                  <th>Capacidad</th>
                  <th>Vendidas</th>
                  <th>Devueltas</th>
                  <th>A reintegrar</th>
                  <th>Vigentes</th>
                  <th>Ocupación</th>
                  <th>Recaudación bruta</th>
                  <th>Recaudación neta</th>
                </tr>
              </thead>
              <tbody>
                {reporte.sectores.map((s) => (
                  <tr key={s.idSector}>
                    <td>{s.nombre}</td>
                    <td>{entero(s.capacidad)}</td>
                    <td>{entero(s.vendidas)}</td>
                    <td>{entero(s.devueltas)}</td>
                    <td>{entero(s.aReintegrar)}</td>
                    <td>{entero(s.vigentes)}</td>
                    <td>{pct(s.ocupacionPct)}</td>
                    <td>{dinero(s.recaudacionBruta, moneda)}</td>
                    <td>{dinero(s.recaudacionNeta, moneda)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <th>Total</th>
                  <th>{entero(t.capacidad)}</th>
                  <th>{entero(t.vendidas)}</th>
                  <th>{entero(t.devueltas)}</th>
                  <th>{entero(t.aReintegrar)}</th>
                  <th>{entero(t.vigentes)}</th>
                  <th>{pct(t.ocupacionPct)}</th>
                  <th>{dinero(t.recaudacionBruta, moneda)}</th>
                  <th>{dinero(t.recaudacionNeta, moneda)}</th>
                </tr>
              </tfoot>
            </table>
            <p className="gestion-card__muted">
              Vigentes = vendidas − devueltas − a reintegrar. Recaudación neta = bruta − devuelta − a
              reintegrar. Generado el{' '}
              {reporte.generadoEn ? new Date(`${reporte.generadoEn}Z`).toLocaleString('es-AR') : '—'}.
            </p>
          </div>
        </>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion/eventos">← Mis eventos</Link>
      </p>
    </section>
  )
}

export default GestionEventoReportesPage
