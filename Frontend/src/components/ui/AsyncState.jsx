import './AsyncState.css'

/** Estado de carga accesible y consistente. */
export function LoadingState({ label = 'Cargando…', className = '' }) {
  return (
    <div className={`async-state async-state--loading ${className}`.trim()} role="status">
      <span className="async-state__spinner" aria-hidden />
      <span>{label}</span>
    </div>
  )
}

/** Error con acción opcional de reintento. */
export function ErrorState({ error, message, onRetry, className = '' }) {
  const text =
    message ||
    error?.message ||
    'No se pudo cargar la información. Intentá de nuevo.'

  return (
    <div className={`async-state async-state--error ${className}`.trim()} role="alert">
      <p className="async-state__text">{text}</p>
      {onRetry ? (
        <button type="button" className="btn" onClick={onRetry}>
          Reintentar
        </button>
      ) : null}
    </div>
  )
}

/** Estado vacío reutilizable. */
export function EmptyState({ title, children, action, className = '' }) {
  return (
    <div className={`async-state async-state--empty ${className}`.trim()}>
      {title ? <p className="async-state__title">{title}</p> : null}
      {children ? <div className="async-state__text">{children}</div> : null}
      {action ? <div className="async-state__actions">{action}</div> : null}
    </div>
  )
}
