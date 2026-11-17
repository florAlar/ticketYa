import { useMemo, useState } from 'react'
import useAsync from '../../hooks/useAsync'
import {
  listarMisNotificaciones,
  marcarNotificacionLeida,
} from '../../api/notificaciones'
import {
  getNotificacionesPreferencias,
  putNotificacionesPreferencias,
} from '../../api/preferencias'

export const PAGE_SIZE = 20
const EXTRA_VACIO = {
  key: '',
  items: [],
  page: 0,
  totalPages: 1,
  loading: false,
  error: '',
}

function normalizarPrefs(data) {
  return {
    push: data?.push !== false,
    email: data?.email !== false,
    tipos: Array.isArray(data?.tipos) ? data.tipos : [],
  }
}

/** Bandeja paginada: página 0 vía useAsync + “Ver más” acumulativo. */
export function useNotificaciones({ token, tab, enabled }) {
  const key = `${tab}|${token || ''}`
  const [extra, setExtra] = useState(EXTRA_VACIO)
  const [markingAll, setMarkingAll] = useState(false)

  if (extra.key !== key) {
    setExtra({ ...EXTRA_VACIO, key })
  }

  const query = useAsync(
    async (signal) => {
      const data = await listarMisNotificaciones({
        token,
        page: 0,
        size: PAGE_SIZE,
        leida: tab === 'no-leidas' ? false : undefined,
        signal,
      })
      return {
        content: Array.isArray(data?.content) ? data.content : [],
        page: Number(data?.page) || 0,
        totalPages: Number(data?.totalPages) || 1,
        noLeidas: Number(data?.noLeidas) || 0,
      }
    },
    [token, tab],
    { enabled }
  )

  const base = query.data
  const items = useMemo(() => {
    const content = base?.content || []
    if (extra.items.length === 0) return content
    const seen = new Set(content.map((n) => n.id))
    return [...content, ...extra.items.filter((n) => !seen.has(n.id))]
  }, [base, extra.items])

  const page = Math.max(Number(base?.page ?? 0), extra.page)
  const totalPages = Math.max(Number(base?.totalPages ?? 1), extra.totalPages)
  const noLeidas = Number(base?.noLeidas || 0)
  const error = extra.error || ''

  const cargarMas = async () => {
    if (extra.loading || !token) return
    setExtra((prev) => ({ ...prev, loading: true, error: '' }))
    try {
      const nextPage = page + 1
      const data = await listarMisNotificaciones({
        token,
        page: nextPage,
        size: PAGE_SIZE,
        leida: tab === 'no-leidas' ? false : undefined,
      })
      const content = Array.isArray(data?.content) ? data.content : []
      setExtra((prev) => ({
        ...prev,
        items: [...prev.items, ...content],
        page: nextPage,
        totalPages: Number(data?.totalPages) || prev.totalPages,
        loading: false,
      }))
    } catch (err) {
      setExtra((prev) => ({
        ...prev,
        loading: false,
        error: err.message || 'No se pudieron cargar más notificaciones',
      }))
    }
  }

  const marcar = (notificacion) => {
    const misma = (n) =>
      n.id === notificacion.id ? { ...n, leida: true } : n
    query.setData((prev) =>
      prev
        ? {
            ...prev,
            noLeidas: Math.max(0, prev.noLeidas - 1),
            content:
              tab === 'no-leidas'
                ? prev.content.filter((n) => n.id !== notificacion.id)
                : prev.content.map(misma),
          }
        : prev
    )
    setExtra((prev) => ({
      ...prev,
      items:
        tab === 'no-leidas'
          ? prev.items.filter((n) => n.id !== notificacion.id)
          : prev.items.map(misma),
    }))
  }

  const markRead = async (notificacion) => {
    if (notificacion.leida) return
    try {
      await marcarNotificacionLeida(notificacion.id, { token })
      marcar(notificacion)
    } catch (err) {
      setExtra((prev) => ({
        ...prev,
        error: err.message || 'No se pudo marcar como leída',
      }))
    }
  }

  const markAllVisible = async () => {
    const pendientes = items.filter((n) => !n.leida)
    if (pendientes.length === 0) return
    setMarkingAll(true)
    setExtra((prev) => ({ ...prev, error: '' }))
    try {
      await Promise.all(
        pendientes.map((n) => marcarNotificacionLeida(n.id, { token }))
      )
      query.setData((prev) =>
        prev
          ? {
              ...prev,
              noLeidas: Math.max(0, prev.noLeidas - pendientes.length),
              content:
                tab === 'no-leidas'
                  ? []
                  : prev.content.map((n) => ({ ...n, leida: true })),
            }
          : prev
      )
      setExtra((prev) => ({
        ...prev,
        items:
          tab === 'no-leidas'
            ? []
            : prev.items.map((n) => ({ ...n, leida: true })),
      }))
    } catch (err) {
      setExtra((prev) => ({
        ...prev,
        error: err.message || 'No se pudieron marcar las notificaciones',
      }))
    } finally {
      setMarkingAll(false)
    }
  }

  const reload = () => {
    setExtra({ ...EXTRA_VACIO, key })
    query.reload()
  }

  return {
    items,
    status: query.status,
    error: query.error?.message || error,
    noLeidas,
    page,
    totalPages,
    loadingMore: extra.loading,
    markingAll,
    cargarMas,
    markRead,
    markAllVisible,
    reload,
  }
}

/** Preferencias de canales (push/email). */
export function useNotificacionesPrefs({ token, enabled }) {
  const query = useAsync(
    async (signal) =>
      normalizarPrefs(await getNotificacionesPreferencias({ token, signal })),
    [token],
    { enabled }
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const toggle = async (canal) => {
    if (!query.data || saving) return
    const next = { ...query.data, [canal]: !query.data[canal] }
    query.setData(next)
    setSaving(true)
    setError('')
    setMsg('')
    try {
      const saved = await putNotificacionesPreferencias(
        { push: next.push, email: next.email },
        { token }
      )
      query.setData({
        ...normalizarPrefs(saved),
        tipos:
          Array.isArray(saved?.tipos) && saved.tipos.length
            ? saved.tipos
            : next.tipos,
      })
      setMsg('Preferencias guardadas')
    } catch (err) {
      setError(err.message || 'No se pudieron guardar las preferencias')
      query.reload()
    } finally {
      setSaving(false)
    }
  }

  return {
    prefs: query.data,
    status: query.status,
    error: error || query.error?.message || '',
    msg,
    saving,
    toggle,
    reload: query.reload,
  }
}
