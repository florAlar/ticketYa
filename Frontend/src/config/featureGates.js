import {
  USE_MOCK_ENTRADAS,
  USE_MOCK_EVENTOS,
} from '../api/config'

/**
 * Features cableadas al gateway/BE real.
 * true  → CTA clickeable (flujo demockeado o API lista)
 * false → agrisar / sin navegación (el flujo mock/shell sigue en el repo)
 *
 * No borra pantallas: solo corta la entrada por UI.
 */
export const FEATURE = {
  /** Gestión eventos (list/create/publicar/estado) — E1-EVE write */
  eventosGestion: !USE_MOCK_EVENTOS,

  /** Devolución cliente + cola admin devoluciones — msvc-entradas */
  devoluciones: !USE_MOCK_ENTRADAS,

  /**
   * Cola admin ventas: con ENTRADAS real trae pendientes;
   * cancelados/reintegro UI aún parcial → igual se habilita para probar approve/reject.
   */
  adminColaVentas: !USE_MOCK_ENTRADAS,

  /** Admin usuarios / personal acceso — facade aún USE_MOCK global */
  adminUsuarios: false,

  /** Scan QR puerta — E2-ACC sin API */
  accesoScan: false,

  /** Transferencia de entradas — shell sin API */
  transferencia: false,
}

export const GATE_HINT = 'Sin backend aún — flujo no cableado'

export function isFeatureOn(key) {
  return Boolean(FEATURE[key])
}
