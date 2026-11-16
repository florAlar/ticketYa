import { http } from './http'
import { USE_MOCK } from './config'

/**
 * Reportes del organizador (E2-REP-01) — msvc-ventas.
 * GET /api/reportes/eventos/{id}: totales y detalle por sector (ventas, devoluciones, ocupación,
 * recaudación). Solo ORGANIZADOR dueño del evento o ADMIN. No hay versión mock: sin backend real
 * el feature queda deshabilitado (FEATURE.reportes).
 */
export async function getReporteEvento(idEvento, opts = {}) {
  if (USE_MOCK) {
    const err = new Error('Los reportes requieren el backend real (VITE_USE_MOCK=false)')
    err.status = 501
    throw err
  }
  return http(`/api/reportes/eventos/${encodeURIComponent(idEvento)}`, {
    token: opts.token,
  })
}
