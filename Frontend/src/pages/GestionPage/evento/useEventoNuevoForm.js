import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { createEvento, getEvento, syncEventoParticipantes } from '../../../api/eventos'
import { getLugar } from '../../../api/lugares'
import { isAbortError } from '../../../api/http'
import {
  EVENTO_ESTADOS,
  LIMITE_ALTA_DEMANDA,
  LUGAR_POR_DEFINIR_ID,
  cupoTotalSectores,
  rowsDeLugar,
  validateEventoGestionForm,
} from '../../../domain/evento'
import { FEATURE } from '../../../config/featureGates'
import {
  newTeloneroRow,
  splitParticipantesForm,
} from '../../../components/gestion/eventoParticipantes'

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

const draftKey = (userId) => `ticketya_evento_borrador:${userId || 'anon'}`

function leerBorrador(userId) {
  try {
    const raw = localStorage.getItem(draftKey(userId))
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** ¿El borrador tiene algo escrito que valga la pena recuperar? */
function conContenido(d) {
  const f = d?.form
  return Boolean(
    f &&
      (f.nombre?.trim() ||
        f.descripcion?.trim() ||
        f.imagenUrl?.trim() ||
        f.principal?.id ||
        f.idLugar ||
        f.fechaInicio)
  )
}

/**
 * Estado y envío del asistente de alta de evento.
 * - Guarda el avance en el navegador (borrador local) y lo ofrece al volver.
 * - Permite arrancar copiando otro evento (`location.state.duplicarDe`), sin la fecha.
 */
export default function useEventoNuevoForm({ user, token }) {
  const navigate = useNavigate()
  const location = useLocation()
  const userId = user?.id
  const duplicarDe = location.state?.duplicarDe || null

  const [form, setForm] = useState(INITIAL_FORM)
  const [sectores, setSectores] = useState([])
  const [teloneros, setTeloneros] = useState([])
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [paso, setPaso] = useState(0)
  const [duplicando, setDuplicando] = useState(Boolean(duplicarDe))
  const [borradorPendiente, setBorradorPendiente] = useState(() =>
    duplicarDe ? null : (() => {
      const d = leerBorrador(userId)
      return conContenido(d) ? d : null
    })()
  )
  const hizoCambios = useRef(false)

  // Duplicar: trae el evento, copia todo salvo la fecha y arranca como borrador
  useEffect(() => {
    if (!duplicarDe || !token) return undefined
    const ctrl = new AbortController()
    ;(async () => {
      try {
        const ev = await getEvento(duplicarDe, { token, signal: ctrl.signal })
        const parts = splitParticipantesForm(ev.participantes)
        const idLugar = ev.lugar?.id || ev.idLugar
        const lugarOk = idLugar && idLugar !== LUGAR_POR_DEFINIR_ID ? idLugar : ''
        let rows = []
        if (lugarOk) {
          try {
            const lugar = await getLugar(lugarOk, { token, signal: ctrl.signal })
            rows = rowsDeLugar(lugar, ev.sectores)
          } catch (err) {
            if (isAbortError(err)) throw err
          }
        }
        setForm({
          ...INITIAL_FORM,
          nombre: ev.nombre || '',
          descripcion: ev.descripcion || '',
          tipo: String(ev.tipo || 'RECITAL').toUpperCase(),
          esAltaDemanda: Boolean(ev.esAltaDemanda),
          destacado: Boolean(ev.destacado),
          principal: parts.principal || { id: null, nombre: '' },
          idLugar: lugarOk,
          imagenUrl: ev.imagenUrl || '',
        })
        setTeloneros(parts.teloneros || [])
        setSectores(rows)
        hizoCambios.current = true
      } catch (err) {
        if (!isAbortError(err)) setError(err.message || 'No se pudo copiar el evento')
      } finally {
        setDuplicando(false)
      }
    })()
    return () => ctrl.abort()
  }, [duplicarDe, token])

  // Autoguardado del borrador local
  useEffect(() => {
    if (!hizoCambios.current) return undefined
    const id = setTimeout(() => {
      try {
        localStorage.setItem(
          draftKey(userId),
          JSON.stringify({ form, sectores, teloneros, paso, guardado: Date.now() })
        )
      } catch {
        /* sin espacio o modo privado: no se guarda y listo */
      }
    }, 400)
    return () => clearTimeout(id)
  }, [form, sectores, teloneros, paso, userId])

  const limpiarBorrador = () => {
    try {
      localStorage.removeItem(draftKey(userId))
    } catch {
      /* nada */
    }
  }

  const restaurarBorrador = () => {
    const d = borradorPendiente
    if (!d) return
    setForm({ ...INITIAL_FORM, ...d.form })
    setSectores(d.sectores || [])
    setTeloneros(d.teloneros || [])
    setPaso(Number.isInteger(d.paso) ? d.paso : 0)
    hizoCambios.current = true
    setBorradorPendiente(null)
  }

  const descartarBorrador = () => {
    limpiarBorrador()
    setBorradorPendiente(null)
  }

  const marcar = () => {
    hizoCambios.current = true
  }

  const onChange = (e) => {
    marcar()
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const onRowChange = (id, field, value) => {
    marcar()
    setSectores((prev) =>
      prev.map((s) => (s.id === id ? { ...s, [field]: value } : s))
    )
  }

  /** Aplica el mismo precio a todos los sectores que se venden. */
  const onPrecioATodos = (precio) => {
    marcar()
    setSectores((prev) =>
      prev.map((s) => (s.incluido ? { ...s, precio: String(precio) } : s))
    )
  }

  const onElegirLugar = (lugar) => {
    marcar()
    setForm((prev) => ({ ...prev, idLugar: lugar?.id || '' }))
    setSectores(lugar ? rowsDeLugar(lugar) : [])
  }

  const onPrincipalChange = (value) => {
    marcar()
    setForm((prev) => ({ ...prev, principal: value }))
  }

  const onTeloneroChange = (key, value) => {
    marcar()
    setTeloneros((prev) =>
      prev.map((t) =>
        t.key === key
          ? { ...t, id: value?.id || null, nombre: value?.nombre || '' }
          : t
      )
    )
  }

  const onAddTelonero = () => {
    marcar()
    setTeloneros((prev) => [...prev, newTeloneroRow()])
  }

  const onRemoveTelonero = (key) => {
    marcar()
    setTeloneros((prev) => prev.filter((t) => t.key !== key))
  }

  const crear = async (payload) => {
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
    limpiarBorrador()
    navigate('/gestion/eventos', {
      replace: true,
      state: { createdId: created.id },
    })
  }

  /** Guarda como borrador (`publicar = false`) o publica (`publicar = true`). */
  const enviar = async (publicar) => {
    if (!FEATURE.eventosGestion) return false
    setError('')
    const mode = publicar ? 'publicacion' : 'borrador'
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
      return false
    }

    const cupoTotal = cupoTotalSectores(sectores)
    const payload = {
      ...form,
      estado: publicar ? EVENTO_ESTADOS.PUBLICADO : EVENTO_ESTADOS.BORRADOR,
      // Con cupo total > LIMITE_ALTA_DEMANDA el backend lo fuerza a alta demanda
      esAltaDemanda: Boolean(form.esAltaDemanda) || cupoTotal > LIMITE_ALTA_DEMANDA,
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

    setStatus('loading')
    try {
      await crear(payload)
      return true
    } catch (err) {
      setStatus('error')
      setError(err.message || 'No se pudo crear')
      return false
    }
  }

  return {
    form,
    sectores,
    teloneros,
    status,
    error,
    setError,
    loading: status === 'loading',
    gated: !FEATURE.eventosGestion,
    paso,
    setPaso,
    duplicando,
    borradorPendiente,
    restaurarBorrador,
    descartarBorrador,
    onChange,
    onRowChange,
    onPrecioATodos,
    onElegirLugar,
    onPrincipalChange,
    onTeloneroChange,
    onAddTelonero,
    onRemoveTelonero,
    enviar,
  }
}
