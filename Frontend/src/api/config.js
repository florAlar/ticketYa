const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// true por defecto en el simulacro hasta cablear el gateway
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/**
 * Flags por módulo (override fino sin apagar todo el mock).
 * Si la env no está, hereda USE_MOCK.
 * Compras reales requieren auth+eventos reales (JWT + ids de seed BE).
 */
function mockFlag(envKey) {
  const v = import.meta.env[envKey]
  if (v === 'false') return false
  if (v === 'true') return true
  return USE_MOCK
}

const USE_MOCK_AUTH = mockFlag('VITE_USE_MOCK_AUTH')
const USE_MOCK_EVENTOS = mockFlag('VITE_USE_MOCK_EVENTOS')
const USE_MOCK_COMPRAS = mockFlag('VITE_USE_MOCK_COMPRAS')
const USE_MOCK_ENTRADAS = mockFlag('VITE_USE_MOCK_ENTRADAS')

export {
  API_BASE_URL,
  USE_MOCK,
  USE_MOCK_AUTH,
  USE_MOCK_EVENTOS,
  USE_MOCK_COMPRAS,
  USE_MOCK_ENTRADAS,
}
