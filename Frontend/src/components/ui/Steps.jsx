import { HiCheck } from 'react-icons/hi'

/** Pasos horizontales. `current` es el índice activo (0-based); los anteriores quedan completos. */
const Steps = ({ steps, current = 0, className = '' }) => (
  <ol className={`steps ${className}`.trim()} aria-label="Pasos">
    {steps.map((s, i) => {
      const label = s.label || s
      const state = i < current ? 'done' : i === current ? 'current' : 'todo'
      return (
        <li
          key={label}
          className={`steps__item steps__item--${state}`}
          aria-current={state === 'current' ? 'step' : undefined}
        >
          <span className="steps__dot">
            {state === 'done' ? <HiCheck aria-hidden /> : i + 1}
          </span>
          <span className="steps__label">{label}</span>
        </li>
      )
    })}
  </ol>
)

export default Steps
