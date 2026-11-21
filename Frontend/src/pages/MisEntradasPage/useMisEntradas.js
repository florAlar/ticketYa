import { useCallback, useMemo } from 'react'
import useAsync from '../../hooks/useAsync'
import { listMisEntradas } from '../../api/entradas'
import { getEvento } from '../../api/eventos'
import { listMisSolicitudes } from '../../api/soporteCliente'
import {
  isEntradaReintegroCancelacion,
  isEntradaVisibleEnListado,
} from '../../domain/entrada'

/** La entrada no trae la portada: se completa con la del evento (una consulta por evento distinto). */
async function conPortadas(entradas, opts) {
  const ids = [...new Set(entradas.map((e) => e.idEvento).filter(Boolean))]
  const eventos = new Map()
  await Promise.all(
    ids.map(async (id) => {
      try {
        eventos.set(id, await getEvento(id, opts))
      } catch (err) {
        if (err?.name === 'AbortError') throw err
      }
    })
  )
  return entradas.map((e) => {
    const ev = eventos.get(e.idEvento)
    if (!ev) return e
    return {
      ...e,
      eventoImagenUrl: e.eventoImagenUrl || ev.imagenUrl,
      eventoTipo: e.eventoTipo || ev.tipo,
    }
  })
}

/** Entradas + trámites del cliente, con agrupaciones para la UI. */
export default function useMisEntradas({ user, token }) {
  const load = useCallback(
    async (signal) => {
      const ents = await listMisEntradas(user, { token, signal })
      const listEnts = await conPortadas(
        Array.isArray(ents) ? ents : [],
        { token, signal }
      )
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
