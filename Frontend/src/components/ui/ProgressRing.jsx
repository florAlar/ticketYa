/**
 * Anillo de progreso SVG (0–100) con degradé de marca. Los hijos se centran dentro.
 */
const ProgressRing = ({ value = 0, size = 168, stroke = 12, children, label }) => {
  const pct = Math.min(100, Math.max(0, Number(value) || 0))
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  return (
    <div
      className="progress-ring"
      style={{ width: size, height: size }}
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={pct}
      aria-label={label}
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} aria-hidden>
        <defs>
          <linearGradient id="ring-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" style={{ stopColor: "var(--brand)" }} />
            <stop offset="100%" style={{ stopColor: "var(--brand-3)" }} />
          </linearGradient>
        </defs>
        <circle
          className="progress-ring__track"
          cx={size / 2}
          cy={size / 2}
          r={r}
          strokeWidth={stroke}
          fill="none"
        />
        <circle
          className="progress-ring__bar"
          cx={size / 2}
          cy={size / 2}
          r={r}
          strokeWidth={stroke}
          fill="none"
          stroke="url(#ring-grad)"
          strokeLinecap="round"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - pct / 100)}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      <div className="progress-ring__center">{children}</div>
    </div>
  )
}

export default ProgressRing
