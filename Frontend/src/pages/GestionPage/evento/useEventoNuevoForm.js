import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createEvento, syncEventoParticipantes } from '../../../api/eventos'
import {
  EVENTO_ESTADOS,
  rowsDeLugar,
  validateEventoGestionForm,
} from '../../../domain/evento'
import { FEATURE } from '../../../config/featureGates'
import { newTeloneroRow } from '../../../components/gestion/eventoParticipantes'

const INITIAL_FORM = {
  nombre: '',
  descripcion: '',
  tipo: 'RECITAL',
  fechaInicio: '',
  estado: EVENTO_ESTADOS.BORRADOR,
  esAltaDemanda: false,
  destacado: false,
  principal: { id: null, nombre: '' },
  idLugar: '',
  imagenUrl: '',
}

/** Estado y submit del alta de evento. */
export default function useEventoNuevoForm({ user, token }) {
  const navigate = useNavigate()
  const [form, setForm] = useState(INITIAL_FORM)
  const [sectores, setSectores] = useState([])
  const [teloneros, setTeloneros] = useState([])
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')

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
    if (!FEATURE.eventosGestion) return
    setError('')
    setStatus('loading')
    try {
      const wantsPublish =
        String(form.estado || '').toUpperCase() === EVENTO_ESTADOS.PUBLICADO
      const mode = wantsPublish ? 'publicacion' : 'borrador'
      const check = validateEventoGestionForm(
        {
          ...form,
          principalId: form.principal?.id,
          principal: form.principal,
          teloneros,
        },
        sectores,
        mode
      )
      if (!check.ok) {
        setError(check.error)
        setStatus('error')
        return
      }

      const payload = {
        ...form,
        estado: wantsPublish
          ? EVENTO_ESTADOS.PUBLICADO
          : EVENTO_ESTADOS.BORRADOR,
        esAltaDemanda: false,
        fechaInicio: form.fechaInicio ? form.fechaInicio : null,
        idLugar: form.idLugar || undefined,
        sectores: check.sectores.map((s) => ({
          ...s,
          precio: s.precio == null ? 0 : s.precio,
        })),
      }
      delete payload.principal
      delete payload.principalId
      delete payload.principalNombre
      delete payload.participantes
      delete payload.teloneros

      const created = await createEvento(payload, user, { token })
      await syncEventoParticipantes(
        created.id,
        {
          principalId: form.principal?.id,
          principalNombre: form.principal?.nombre,
          teloneros,
        },
        { token }
      )
      setStatus('ok')
      navigate('/gestion/eventos', {
        replace: true,
        state: { createdId: created.id },
      })
    } catch (err) {
      setStatus('error')
      setError(err.message || 'No se pudo crear')
    }
  }

  const publishing =
    String(form.estado || '').toUpperCase() === EVENTO_ESTADOS.PUBLICADO

  return {
    form,
    sectores,
    teloneros,
    status,
    error,
    loading: status === 'loading',
    publishing,
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
