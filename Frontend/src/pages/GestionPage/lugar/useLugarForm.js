import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useAsync from '../../../hooks/useAsync'
import {
  createLugar,
  createSector,
  geocodificarLugar,
  getLugar,
  updateLugar,
  updateSector,
} from '../../../api/lugares'
import { isAbortError } from '../../../api/http'
import {
  FORM_LUGAR_VACIO,
  FORM_SECTOR_VACIO,
  lugarFormToPayload,
  lugarToForm,
  sectorFormToPayload,
  sectorToForm,
  validateLugarForm,
  validateSectorForm,
} from '../../../domain/lugar'

/** Estado y acciones del alta/edición de lugar + sectores (LUG-01). */
export default function useLugarForm({ lugarId, esNuevo, wired, token }) {
  const navigate = useNavigate()
  const [form, setForm] = useState(FORM_LUGAR_VACIO)
  const [formFor, setFormFor] = useState(esNuevo ? 'nuevo' : null)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [saving, setSaving] = useState(false)
  const [geocoding, setGeocoding] = useState(false)

  const [editando, setEditando] = useState(null) // null | 'nuevo' | idSector
  const [sectorForm, setSectorForm] = useState(FORM_SECTOR_VACIO)
  const [sectorError, setSectorError] = useState('')
  const [sectorSaving, setSectorSaving] = useState(false)

  const query = useAsync(
    async (signal) => {
      const data = await getLugar(lugarId, { token, signal })
      return data
    },
    [lugarId, token],
    { enabled: !esNuevo && wired }
  )

  const lugar = query.data || null
  const status = esNuevo || !wired ? 'success' : query.status
  const loadError = query.error?.message || ''

  if (lugar && formFor !== lugar.id) {
    setFormFor(lugar.id)
    setForm(lugarToForm(lugar))
  }

  const onChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const buscarCoordenadas = async () => {
    if (!wired) return
    const ciudad = form.ciudad.trim()
    if (!ciudad) {
      setError('Ingresá al menos la ciudad para buscar coordenadas')
      return
    }
    setGeocoding(true)
    setError('')
    setAviso('')
    try {
      const res = await geocodificarLugar(
        { direccion: form.direccion, ciudad },
        { token }
      )
      setForm((prev) => ({
        ...prev,
        latitud: res?.latitud != null ? String(res.latitud) : prev.latitud,
        longitud: res?.longitud != null ? String(res.longitud) : prev.longitud,
      }))
      setAviso('Coordenadas autocompletadas.')
    } catch (err) {
      setError(
        err.status === 404
          ? 'No se encontraron coordenadas para esa dirección; podés cargarlas manualmente.'
          : err.message || 'No se pudieron obtener las coordenadas'
      )
    } finally {
      setGeocoding(false)
    }
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!wired) return
    const check = validateLugarForm(form)
    if (!check.ok) {
      setError(check.error)
      return
    }
    setSaving(true)
    setError('')
    setAviso('')
    try {
      if (esNuevo) {
        const creado = await createLugar(lugarFormToPayload(form), { token })
        navigate(`/gestion/lugares/${creado.id}`, { replace: true })
      } else {
        const tenia = lugar?.latitud != null && lugar?.longitud != null
        const actualizado = await updateLugar(
          lugarId,
          lugarFormToPayload(form, { esEdicion: true, teniaCoordenadas: tenia }),
          { token }
        )
        query.setData(actualizado)
        setForm(lugarToForm(actualizado))
        setAviso('Lugar guardado.')
      }
    } catch (err) {
      setError(err.message || 'No se pudo guardar el lugar')
    } finally {
      setSaving(false)
    }
  }

  const abrirSector = (sector) => {
    setSectorError('')
    if (sector) {
      setEditando(sector.id)
      setSectorForm(sectorToForm(sector))
    } else {
      setEditando('nuevo')
      setSectorForm(FORM_SECTOR_VACIO)
    }
  }

  const onSectorChange = (field, value) => {
    setSectorForm((prev) => ({ ...prev, [field]: value }))
  }

  const cerrarSector = () => {
    setEditando(null)
    setSectorError('')
  }

  const guardarSector = async (e) => {
    e.preventDefault()
    const check = validateSectorForm(sectorForm)
    if (!check.ok) {
      setSectorError(check.error)
      return
    }
    setSectorSaving(true)
    setSectorError('')
    try {
      const payload = sectorFormToPayload(sectorForm)
      if (editando === 'nuevo') {
        await createSector(lugarId, payload, { token })
      } else {
        await updateSector(editando, payload, { token })
      }
      setEditando(null)
      setAviso('Sector guardado.')
      // Refresca el lugar sin ocultar el formulario con un estado loading.
      try {
        const fresh = await getLugar(lugarId, { token })
        query.setData(fresh)
      } catch (err) {
        if (isAbortError(err)) throw err
        /* el save ya se confirmó; el refresh es best-effort */
      }
    } catch (err) {
      setSectorError(err.message || 'No se pudo guardar el sector')
    } finally {
      setSectorSaving(false)
    }
  }

  return {
    form,
    lugar,
    status,
    error: error || loadError,
    actionError: error,
    loadError,
    aviso,
    saving,
    geocoding,
    editando,
    sectorForm,
    sectorError,
    sectorSaving,
    onChange,
    buscarCoordenadas,
    onSubmit,
    abrirSector,
    onSectorChange,
    cerrarSector,
    guardarSector,
    reload: query.reload,
  }
}
