import { useEffect, useState } from 'react'
import { FEATURE } from '../../config/featureGates'
import { getRecomendacionesEvento } from '../../api/preferencias'
import RecomendacionesBlock from './RecomendacionesBlock'

/** “Eventos similares” en el detalle. Público; se oculta si no hay datos. */
const EventosSimilares = ({ eventoId }) => {
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('idle')

  useEffect(() => {
    if (!eventoId || !FEATURE.recomendaciones) {
      return undefined
    }
    let cancelled = false
    getRecomendacionesEvento(eventoId, { limit: 6 })
      .then((data) => {
        if (cancelled) return
        setItems(Array.isArray(data) ? data : [])
        setStatus('ok')
      })
      .catch(() => {
        if (cancelled) return
        setItems([])
        setStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [eventoId])

  if (!FEATURE.recomendaciones || status !== 'ok' || items.length === 0) {
    return null
  }

  return (
    <RecomendacionesBlock
      title="Eventos similares"
      subtitle="Quienes compraron este evento también compraron…"
      items={items}
      className="evento-similares"
    />
  )
}

export default EventosSimilares
