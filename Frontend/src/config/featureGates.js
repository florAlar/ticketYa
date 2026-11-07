import {
  USE_MOCK_ENTRADAS,
  USE_MOCK_EVENTOS,
  USE_MOCK,
} from '../api/config'

/**
 * Features cableadas al gateway/BE real.
 * true  → CTA clickeable
 * false → agrisar / sin navegación (flujos mock/shell quedan en el repo)
 *
 * Regla: solo ON si el contrato BE+gateway deja usar el flujo de punta a punta
 * para el rol que lo ve. Si el msvc existe pero el GW bloquea o no hay API, OFF.
 */
export const FEATURE = {
  /** Listado + alta + publicar (POST …/publicacion) — E1-EVE */
  eventosGestion: !USE_MOCK_EVENTOS,

  /**
   * Suspender (PATCH …/estado SUSPENDIDO) — ORG dueño o ADMIN.
   * GW ya no bloquea el path; msvc-eventos valida permiso.
   */
  eventosSuspender: !USE_MOCK_EVENTOS,

  /**
   * Cancelar evento (PATCH CANCELADO + reintegro Feign) — solo ADMIN en BE.
   * ORG no debe ver el CTA habilitado.
   */
  eventosCancelar: !USE_MOCK_EVENTOS,

  /** Devolución cliente + approve/reject admin — msvc-entradas */
  devoluciones: !USE_MOCK_ENTRADAS,

  /** Cola admin: listar pendientes + aprobar/rechazar */
  adminColaVentas: !USE_MOCK_ENTRADAS,

  /**
   * Reintegro masivo por evento cancelado (POST …/reintegro-cancelacion).
   * Hay API; la UI debe pasar idEvento (no idEntrada suelta).
   */
  adminReintegroEvento: !USE_MOCK_ENTRADAS,

  /** Reintegro por una sola entrada — no hay endpoint BE (501 en facade) */
  adminReintegroEntrada: false,

  /**
   * Admin usuarios / alta personal acceso.
   * List/estado/roles existen en BE, pero alta acceso + eventosAsignados
   * siguen solo mock → OFF hasta contrato completo.
   */
  adminUsuarios: false,

  /** Scan QR puerta — E2-ACC sin API */
  accesoScan: false,

  /** Transferencia de entradas — shell sin API */
  transferencia: false,

  /** Reportes organizador — E2-REP sin API */
  reportes: false,

  /** Preferencias / recs cliente — E2-PRE dominio incompleto */
  preferencias: false,
}

export const GATE_HINT = 'Sin backend cableado — flujo no disponible'

export function isFeatureOn(key) {
  return Boolean(FEATURE[key])
}

/** ¿Hay algún mock global residual? (útil para copy de demo) */
export function isAnyMockOn() {
  return Boolean(USE_MOCK)
}
