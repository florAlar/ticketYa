import { Link } from 'react-router-dom'

/**
 * Botón del sistema de diseño. `to` lo renderiza como <Link>.
 * variant: primary | secondary | ghost | danger — size: sm | md | lg
 */
const Button = ({
  variant = 'primary',
  size = 'md',
  block = false,
  loading = false,
  to,
  className = '',
  children,
  ...rest
}) => {
  const cls = [
    'btn',
    `btn-${variant}`,
    size !== 'md' && `btn-${size}`,
    block && 'btn-block',
    loading && 'is-loading',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  if (to) {
    return (
      <Link to={to} className={cls} {...rest}>
        {children}
      </Link>
    )
  }
  return (
    <button
      type="button"
      className={cls}
      disabled={rest.disabled || loading}
      {...rest}
    >
      {children}
    </button>
  )
}

export default Button
