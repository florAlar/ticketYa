import { useEffect, useState } from 'react'

/**
 * Milisegundos que faltan hasta `expiraEn` (ISO o epoch). Se actualiza cada
 * segundo; devuelve 0 si ya venció y `null` si no hay fecha válida.
 */
export default function useCountdown(expiraEn) {
  const target = expiraEn ? new Date(expiraEn).getTime() : NaN
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!Number.isFinite(target)) return undefined
    const id = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(id)
  }, [target])

  if (!Number.isFinite(target)) return null
  return Math.max(0, target - now)
}
