import { useCallback, useMemo } from 'react'
import useAsync from '../../hooks/useAsync'
import { listMisEntradas } from '../../api/entradas'
import { listMisSolicitudes } from '../../api/soporteCliente'
import {
  isEntradaReintegroCancelacion,
  isEntradaVisibleEnListado,
} from '../../domain/entrada'

/** Entradas + trámites del cliente, con agrupaciones para la UI. */
export default function useMisEntradas({ user, token }) {
  const load = useCallback(
    async (signal) => {
      const ents = await listMisEntradas(user, { token, signal })
      const listEnts = Array.isArray(ents) ? ents : []
      const sols = await listMisSolicitudes(user, {
        token,
        signal,
        entradas: listEnts,
      })
      return {
        items: listEnts,
        solicitudes: Array.isArray(sols) ? sols : [],
      }
    },
    [user, token]
  )

  const { data, status, error, reload } = useAsync(
    load,
    [user?.id, token],
    { enabled: Boolean(user?.id && token) }
  )

  const items = useMemo(() => data?.items || [], [data])
  const solicitudes = useMemo(() => data?.solicitudes || [], [data])

  const listado = useMemo(() => {
    const visible = items.filter((e) => isEntradaVisibleEnListado(e))
    return visible.sort((a, b) => {
      const ac = isEntradaReintegroCancelacion(a) ? 1 : 0
      const bc = isEntradaReintegroCancelacion(b) ? 1 : 0
      return ac - bc
    })
  }, [items])

  const pendientePorEntrada = useMemo(() => {
    const map = new Map()
    for (const s of solicitudes) {
      if (s.estado !== 'PENDIENTE') continue
      map.set(s.idEntrada, s)
    }
    return map
  }, [solicitudes])

  const tramitePorEntrada = useMemo(() => {
    const map = new Map()
    for (const s of solicitudes) {
      if (!s.idEntrada) continue
      if (!map.has(s.idEntrada)) map.set(s.idEntrada, s)
    }
    return map
  }, [solicitudes])

  return {
    status,
    error,
    reload,
    listado,
    solicitudes,
    pendientePorEntrada,
    tramitePorEntrada,
  }
}
