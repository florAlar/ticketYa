import { useCallback, useMemo, useState } from 'react'
import useAsync from '../../../hooks/useAsync'
import {
  createPersonalAcceso,
  listUsuarios,
  removeUsuario,
  updateAccesoEventos,
  updateUsuario,
} from '../../../api/usuarios'
import { listEventosGestion } from '../../../api/eventos'
import { FEATURE } from '../../../config/featureGates'

export const FORM_VACIO = {
  nombre: '',
  email: '',
  password: '',
  eventosAsignados: [],
}

/** Listado y acciones de usuarios (solo ADMIN). */
export default function useUsuariosAdmin({ user, token }) {
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState(FORM_VACIO)

  const load = useCallback(
    async (signal) => {
      const [users, evs] = await Promise.all([
        listUsuarios(user, { token, signal }),
        listEventosGestion(user, { isAdmin: true, token, signal }),
      ])
      return {
        items: Array.isArray(users) ? users : [],
        eventos: Array.isArray(evs) ? evs : [],
      }
    },
    [user, token]
  )

  const {
    data,
    status,
    error: loadError,
    reload,
  } = useAsync(load, [user?.id, token], {
    enabled: FEATURE.adminUsuariosListado,
  })

  const items = data?.items || []
  const eventos = useMemo(() => data?.eventos || [], [data])

  const eventoOptions = useMemo(
    () =>
      eventos.map((e) => ({
        id: e.id,
        label: `${e.nombre} (${e.id})`,
      })),
    [eventos]
  )

  const onRol = async (id, rol) => {
    if (!FEATURE.adminUsuariosListado) return
    setBusyId(id)
    setError('')
    try {
      await updateUsuario(id, { rol }, user, { token })
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo cambiar el rol')
    } finally {
      setBusyId(null)
    }
  }

  const onActivo = async (id, activo) => {
    if (!FEATURE.adminUsuariosListado) return
    setBusyId(id)
    setError('')
    try {
      await updateUsuario(id, { activo }, user, { token })
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo cambiar el estado')
    } finally {
      setBusyId(null)
    }
  }

  const onDelete = async (id) => {
    if (!FEATURE.adminUsuarios) return
    if (!window.confirm('¿Eliminar este usuario? (personal de acceso no se desactiva)')) {
      return
    }
    setBusyId(id)
    setError('')
    try {
      await removeUsuario(id, user, { token })
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo eliminar')
    } finally {
      setBusyId(null)
    }
  }

  const onToggleEvento = async (u, eventoId) => {
    if (!FEATURE.adminUsuarios) return
    const cur = new Set(u.eventosAsignados || [])
    if (cur.has(eventoId)) cur.delete(eventoId)
    else cur.add(eventoId)
    setBusyId(u.id)
    setError('')
    try {
      await updateAccesoEventos(u.id, [...cur], user, { token })
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo actualizar asignaciones')
    } finally {
      setBusyId(null)
    }
  }

  const onCreate = async (e) => {
    e.preventDefault()
    if (!FEATURE.adminUsuarios) return
    setError('')
    setBusyId('create')
    try {
      await createPersonalAcceso(form, user, { token })
      setForm(FORM_VACIO)
      setShowCreate(false)
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo crear')
    } finally {
      setBusyId(null)
    }
  }

  const toggleFormEvento = (eventoId) => {
    setForm((prev) => {
      const set = new Set(prev.eventosAsignados)
      if (set.has(eventoId)) set.delete(eventoId)
      else set.add(eventoId)
      return { ...prev, eventosAsignados: [...set] }
    })
  }

  return {
    items,
    status,
    error: error || loadError?.message || '',
    loadError,
    reload,
    busyId,
    showCreate,
    setShowCreate,
    form,
    setForm,
    eventoOptions,
    onRol,
    onActivo,
    onDelete,
    onToggleEvento,
    onCreate,
    toggleFormEvento,
  }
}
