const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * Único interruptor de mock (sin mezclar mock y backend):
 *   VITE_USE_MOCK=false (default) → TODO contra el gateway/BE real. Lo que el BE no implementa
 *                                   queda deshabilitado (ver config/featureGates.js).
 *   VITE_USE_MOCK=true            → TODO en mock local (localStorage), sin backend. Solo demo offline.
 */
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export { API_BASE_URL, USE_MOCK }
