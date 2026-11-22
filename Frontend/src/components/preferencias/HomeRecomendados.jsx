import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { FEATURE } from '../../config/featureGates'
import { getRecomendacionesMe } from '../../api/preferencias'
import RecomendacionesBlock from './RecomendacionesBlock'

/**
 * “Recomendados para vos” en la home.
 * Solo aparece para usuarios logueados, con datos y con la feature encendida.
 * Si falla o no hay datos, no bloquea el resto de la home: retorna null.
 */
const HomeRecomendados = () => {
  const { isAuthenticated, token } = useAuth()
  const enabled = isAuthenticated && FEATURE.recomendaciones && Boolean(token)

  const { data: items = [], status } = useAsync(
    // Igual que el resto de los carruseles de la home: nada que ya haya pasado
    (signal) =>
      getRecomendacionesMe({ token, limit: 8, signal }).then((lista) => {
        const ahora = Date.now()
        return (lista || []).filter(
          (e) => !e?.fechaInicio || new Date(e.fechaInicio).getTime() >= ahora
        )
      }),
    [token],
    { enabled }
  )

  if (!isAuthenticated || !FEATURE.recomendaciones) return null

  if (status === 'loading') {
    return (
      <RecomendacionesBlock
        title="Recomendados para vos"
        subtitle="Según tus gustos y lo que compraron clientes parecidos."
        loading
      />
    )
  }

  if (status !== 'success' || items.length === 0) return null

  return (
    <RecomendacionesBlock
      title="Recomendados para vos"
      subtitle="Según tus gustos y lo que compraron clientes parecidos."
      items={items}
      linkToAll="/eventos"
    />
  )
}

export default HomeRecomendados
