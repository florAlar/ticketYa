import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useAsync from '../../../hooks/useAsync'
import {
  getEvento,
  updateEvento,
  syncEventoParticipantes,
} from '../../../api/eventos'
import { getLugar } from '../../../api/lugares'
import { isAbortError } from '../../../api/http'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPOS,
  LUGAR_POR_DEFINIR_ID,
  rowsDeLugar,
  validateEventoGestionForm,
} from '../../../domain/evento'
import { FEATURE } from '../../../config/featureGates'
import {
  newTeloneroRow,
  splitParticipantesForm,
} from '../../../components/gestion/eventoParticipantes'

function toDatetimeLocalValue(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso).slice(0, 16)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formFromDetalle(ev) {
  const parts = splitParticipantesForm(ev.participantes)
  return {
    form: {
      nombre: ev.nombre || '',
      descripcion: ev.descripcion || '',
      tipo: String(ev.tipo || EVENTO_TIPOS.RECITAL).toUpperCase(),
      fechaInicio: toDatetimeLocalValue(ev.fechaInicio || ev.fechaHora),
      estado: EVENTO_ESTADOS.BORRADOR,
      esAltaDemanda: Boolean(ev.esAltaDemanda),
      destacado: Boolean(ev.destacado),
      principal: parts.principal,
      idLugar:
        (ev.lugar?.id || ev.idLugar) &&
        (ev.lugar?.id || ev.idLugar) !== LUGAR_POR_DEFINIR_ID
          ? ev.lugar?.id || ev.idLugar
          : '',
      imagenUrl: ev.imagenUrl || '',
    },
    teloneros: parts.teloneros,
  }
}

/** Carga y edición de un evento en BORRADOR (PUT /api/eventos/{id}). */
export default function useEventoEditarForm({ eventoId, user, token, isAdmin }) {
  const navigate = useNavigate()
  const userId = user?.id
  const [form, setForm] = useState(null)
  const [sectores, setSectores] = useState([])
  const [teloneros, setTeloneros] = useState([])
  const [loadedFor, setLoadedFor] = useState(null)
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')

  const load = useCallback(
    async (signal) => {
      if (!FEATURE.eventosGestion) return { status: 'gated' }

      const ev = await getEvento(eventoId, { token, signal })
      const est = String(ev?.estado || '').toUpperCase()
      if (est !== EVENTO_ESTADOS.BORRADOR) {
        return {
          status: 'blocked',
          message: `Solo se edita en borrador (estado actual: ${
            EVENTO_ESTADO_LABELS[est] || est
          }).`,
        }
      }
      if (
        !isAdmin &&
        ev.idOrganizador &&
        userId &&
        String(ev.idOrganizador) !== String(userId)
      ) {
        return { status: 'blocked', message: 'No sos el organizador de este evento.' }
      }

      const mapped = formFromDetalle(ev)
      let lugar = null
      if (mapped.form.idLugar) {
        try {
          lugar = await getLugar(mapped.form.idLugar, { token, signal })
        } catch (err) {
          if (isAbortError(err)) throw err
          lugar = null
        }
      }
      return {
        status: 'ok',
        form: mapped.form,
        teloneros: mapped.teloneros,
        sectores: lugar ? rowsDeLugar(lugar, ev.sectores) : [],
      }
    },
    [eventoId, token, isAdmin, userId]
  )

  const {
    data: loaded,
    status: loadStatus,
    error: loadError,
    reload,
  } = useAsync(load, [eventoId, token, isAdmin, userId], {
    enabled: FEATURE.eventosGestion,
  })

  // Inicializa el formulario una sola vez por evento, sin pisar ediciones
  // posteriores si la consulta se revalida.
  if (loaded?.status === 'ok' && loadedFor !== eventoId) {
    setLoadedFor(eventoId)
    setForm(loaded.form)
    setTeloneros(loaded.teloneros)
    setSectores(loaded.sectores)
  }

  const onChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const onRowChange = (id, field, value) => {
    setSectores((prev) =>
      prev.map((s) => (s.id === id ? { ...s, [field]: value } : s))
    )
  }

  const onElegirLugar = (lugar) => {
    setForm((prev) => ({ ...prev, idLugar: lugar?.id || '' }))
    setSectores(lugar ? rowsDeLugar(lugar) : [])
  }

  const onPrincipalChange = (value) => {
    setForm((prev) => ({ ...prev, principal: value }))
  }

  const onTeloneroChange = (key, value) => {
    setTeloneros((prev) =>
      prev.map((t) =>
        t.key === key
          ? { ...t, id: value?.id || null, nombre: value?.nombre || '' }
          : t
      )
    )
  }

  const onAddTelonero = () => {
    setTeloneros((prev) => [...prev, newTeloneroRow()])
  }

  const onRemoveTelonero = (key) => {
    setTeloneros((prev) => prev.filter((t) => t.key !== key))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!FEATURE.eventosGestion || !form) return
    setError('')
    setStatus('loading')
    try {
      const check = validateEventoGestionForm(
        {
          ...form,
          principalId: form.principal?.id,
          principal: form.principal,
          teloneros,
        },
        sectores,
        'borrador'
      )
      if (!check.ok) {
        setError(check.error)
        setStatus('error')
        return
      }

      const payload = {
        ...form,
        estado: EVENTO_ESTADOS.BORRADOR,
        fechaInicio: form.fechaInicio ? form.fechaInicio : null,
        idLugar: form.idLugar || undefined,
        sectores: check.sectores.map((s) => ({
          ...s,
          precio: s.precio == null ? 0 : s.precio,
        })),
      }
      delete payload.principal
      delete payload.principalNombre
      delete payload.principalId
      delete payload.participantes
      delete payload.teloneros

      await updateEvento(eventoId, payload, { token, userId })
      await syncEventoParticipantes(
        eventoId,
        {
          principalId: form.principal?.id,
          principalNombre: form.principal?.nombre,
          teloneros,
        },
        { token }
      )
      setStatus('ok')
      navigate('/gestion/eventos', { replace: true })
    } catch (err) {
      setStatus('error')
      setError(err.message || 'No se pudo guardar')
    }
  }

  return {
    form,
    sectores,
    teloneros,
    loaded,
    loadStatus,
    loadError,
    reload,
    status,
    error,
    gated: !FEATURE.eventosGestion,
    onChange,
    onRowChange,
    onElegirLugar,
    onPrincipalChange,
    onTeloneroChange,
    onAddTelonero,
    onRemoveTelonero,
    onSubmit,
  }
}
