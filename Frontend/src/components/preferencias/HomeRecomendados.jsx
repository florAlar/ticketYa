import { useEffect, useState } from 'react'
import useAuth from '../../hooks/useAuth'
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
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('idle')

  useEffect(() => {
    if (!isAuthenticated || !FEATURE.recomendaciones || !token) {
      return undefined
    }
    let cancelled = false
    getRecomendacionesMe({ token, limit: 8 })
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
  }, [isAuthenticated, token])

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

  if (status !== 'ok' || items.length === 0) return null

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
