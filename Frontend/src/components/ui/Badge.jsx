/**
 * Etiqueta compacta. tone: brand | violet | success | warning | danger | info | neutral
 * `solid` la hace opaca (para superponer sobre imágenes); `hot` (alta demanda) es solo un rayito amarillo, sin texto, borde ni fondo.
 */
const Badge = ({
  tone = 'neutral',
  solid = false,
  hot = false,
  size = 'md',
  icon: Icon,
  className = '',
  children,
  ...rest
}) => {
  if (hot) {
    const etiqueta = rest.title || 'Alta demanda'
    return (
      <span
        className={['badge--hot', className].filter(Boolean).join(' ')}
        role="img"
        aria-label={etiqueta}
        title={etiqueta}
      >
        {Icon ? <Icon aria-hidden /> : null}
      </span>
    )
  }
  const cls = [
    'badge',
    `badge--${tone}`,
    solid && 'badge--solid',
    size === 'sm' && 'badge--sm',
    className,
  ]
    .filter(Boolean)
    .join(' ')
  return (
    <span className={cls} {...rest}>
      {Icon && <Icon aria-hidden />}
      {children}
    </span>
  )
}

export default Badge
