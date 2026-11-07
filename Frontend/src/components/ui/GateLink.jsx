import { Link } from 'react-router-dom'
import { GATE_HINT } from '../../config/featureGates'

/**
 * Link que, si enabled=false, se muestra agrisado y no navega.
 * No elimina la ruta destino: solo corta el click desde este CTA.
 */
export default function GateLink({
  enabled,
  to,
  className = '',
  children,
  title,
  onClick,
  ...rest
}) {
  const hint = title || GATE_HINT
  const classes = [className, !enabled ? 'is-gated-off' : null].filter(Boolean).join(' ')

  if (enabled) {
    return (
      <Link to={to} className={classes} onClick={onClick} {...rest}>
        {children}
      </Link>
    )
  }

  return (
    <span
      className={classes}
      aria-disabled="true"
      title={hint}
      role="link"
      tabIndex={-1}
      onClick={(e) => {
        e.preventDefault()
        e.stopPropagation()
      }}
      {...rest}
    >
      {children}
    </span>
  )
}
