import { GATE_HINT } from '../../config/featureGates'

/**
 * Botón que con enabled=false no dispara onClick y se ve disabled.
 * Mantiene el handler en código para cuando se cablee la feature.
 */
export default function GateButton({
  enabled,
  type = 'button',
  className = '',
  children,
  title,
  onClick,
  disabled,
  ...rest
}) {
  const off = !enabled || disabled
  const classes = [className, off ? 'is-gated-off' : null].filter(Boolean).join(' ')

  return (
    <button
      type={type}
      className={classes}
      disabled={off}
      title={off ? title || GATE_HINT : title}
      aria-disabled={off}
      onClick={off ? undefined : onClick}
      {...rest}
    >
      {children}
    </button>
  )
}
