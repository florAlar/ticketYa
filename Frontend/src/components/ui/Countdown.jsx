import { useEffect, useRef } from 'react'
import { HiOutlineClock } from 'react-icons/hi'
import { toast } from 'sonner'
import useCountdown from '../../hooks/useCountdown'

const AVISO_MS = 2 * 60 * 1000

function mmss(ms) {
  const total = Math.ceil(ms / 1000)
  const m = String(Math.floor(total / 60)).padStart(2, '0')
  const s = String(total % 60).padStart(2, '0')
  return `${m}:${s}`
}

/**
 * Cuenta regresiva visible hasta `expiraEn` (p. ej. el pase de la fila virtual).
 * A los 2 minutos pasa a color de alerta y avisa una vez con un toast.
 */
const Countdown = ({ expiraEn, label = 'Tiempo restante', avisoToast }) => {
  const restante = useCountdown(expiraEn)
  const avisado = useRef(false)
  const critico = restante != null && restante > 0 && restante <= AVISO_MS

  useEffect(() => {
    if (critico && avisoToast && !avisado.current) {
      avisado.current = true
      toast.warning(avisoToast)
    }
  }, [critico, avisoToast])

  if (restante == null) return null
  const vencido = restante === 0
  return (
    <div
      className={`countdown${critico ? ' countdown--warn' : ''}${vencido ? ' countdown--over' : ''}`}
      role="timer"
      aria-live="off"
    >
      <HiOutlineClock aria-hidden />
      <span className="countdown__label">{vencido ? 'El pase venció' : label}</span>
      {!vencido && <strong className="countdown__time">{mmss(restante)}</strong>}
    </div>
  )
}

export default Countdown
