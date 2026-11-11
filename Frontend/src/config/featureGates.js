import { USE_MOCK } from '../api/config'

/**
 * Features cableadas al gateway/BE real.
 * true  → CTA clickeable
 * false → deshabilitado, sin navegación (los flujos mock quedan en el repo para modo mock)
 *
 * Regla: solo ON si el contrato BE+gateway deja usar el flujo de punta a punta
 * para el rol que lo ve. Si el msvc existe pero el GW bloquea o no hay API, OFF.
 */
export const FEATURE = {
  /** Listado + alta + publicar (POST …/publicacion) — E1-EVE */
  eventosGestion: !USE_MOCK,

  /**
   * Suspender (PATCH …/estado SUSPENDIDO) — ORG dueño o ADMIN.
   * GW ya no bloquea el path; msvc-eventos valida permiso.
   */
  eventosSuspender: !USE_MOCK,

  /**
   * Cancelar evento (PATCH CANCELADO + reintegro Feign) — solo ADMIN en BE.
   * ORG no debe ver el CTA habilitado.
   */
  eventosCancelar: !USE_MOCK,

  /** Devolución cliente + approve/reject admin — msvc-entradas */
  devoluciones: !USE_MOCK,

  /** Cola admin: listar pendientes + aprobar/rechazar */
  adminColaVentas: !USE_MOCK,

  /**
   * Reintegro masivo por evento cancelado (POST …/reintegro-cancelacion).
   * Hay API; la UI debe pasar idEvento (no idEntrada suelta).
   */
  adminReintegroEvento: !USE_MOCK,

  /** Reintegro por una sola entrada — no hay endpoint BE (501 en facade) */
  adminReintegroEntrada: false,

  /** Admin usuarios: listar, activar/desactivar y cambiar rol (GET /usuarios, PATCH …/estado, PUT …/roles). */
  adminUsuariosListado: !USE_MOCK,

  /**
   * Admin usuarios: alta de personal de acceso, eliminar y asignar eventos.
   * No existen en el BE → OFF siempre.
   */
  adminUsuarios: false,

  /** Validar QR en la puerta: POST …/validaciones (PERSONAL_ACCESO / ADMIN) — E2-ACC-01 */
  accesoScan: !USE_MOCK,

  /** Transferencia de entradas — shell sin API */
  transferencia: false,

  /** Reportes del organizador: GET /api/reportes/eventos/{id} (ORGANIZADOR dueño o ADMIN) — E2-REP-01 */
  reportes: !USE_MOCK,

  /** Preferencias / recs cliente — E2-PRE dominio incompleto */
  preferencias: false,

  /**
   * Solicitud CLIENTE → ORGANIZADOR (X-ORG-01).
   * CTA visible en /perfil; OFF hasta API de solicitudes + cola admin.
   */
  solicitudOrganizador: false,
}

export const GATE_HINT = 'Sin backend cableado — flujo no disponible'

export function isFeatureOn(key) {
  return Boolean(FEATURE[key])
}

/** ¿Hay algún mock global residual? (útil para copy de demo) */
export function isAnyMockOn() {
  return Boolean(USE_MOCK)
}
