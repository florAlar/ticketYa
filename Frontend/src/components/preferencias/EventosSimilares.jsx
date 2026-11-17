import useAsync from '../../hooks/useAsync'
import { FEATURE } from '../../config/featureGates'
import { getRecomendacionesEvento } from '../../api/preferencias'
import RecomendacionesBlock from './RecomendacionesBlock'

/** “Eventos similares” en el detalle. Público; se oculta si no hay datos. */
const EventosSimilares = ({ eventoId }) => {
  const { data: items = [], status } = useAsync(
    (signal) => getRecomendacionesEvento(eventoId, { limit: 6, signal }),
    [eventoId],
    { enabled: Boolean(eventoId) && FEATURE.recomendaciones }
  )

  if (!FEATURE.recomendaciones || status !== 'success' || items.length === 0) {
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
