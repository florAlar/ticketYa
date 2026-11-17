import './RouteFallback.css'

/** Fallback de Suspense para rutas cargadas con React.lazy. */
export default function RouteFallback() {
  return (
    <div className="route-fallback container section-v-padding" role="status" aria-live="polite">
      <span className="route-fallback__spinner" aria-hidden />
      <span>Cargando…</span>
    </div>
  )
}
