const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// true por defecto en el simulacro hasta cablear el gateway
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export { API_BASE_URL, USE_MOCK }
