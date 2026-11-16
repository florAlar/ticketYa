import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento, updateEvento, syncEventoParticipantes } from '../../api/eventos'
import { getLugar } from '../../api/lugares'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPOS,
  EVENTO_TIPO_LABELS,
  LUGAR_POR_DEFINIR_ID,
  rowsDeLugar,
  validateEventoGestionForm,
} from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import EventoLugarSectores from '../../components/gestion/EventoLugarSectores'
import EventoParticipantesFields, {
  newTeloneroRow,
  splitParticipantesForm,
} from '../../components/gestion/EventoParticipantesFields'
import './GestionPage.css'

function toDatetimeLocalValue(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return String(iso).slice(0, 16)
  }
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
      // el lugar reservado "Por definir" de los borradores sin lugar no es un lugar elegido
      idLugar:
        (ev.lugar?.id || ev.idLugar) && (ev.lugar?.id || ev.idLugar) !== LUGAR_POR_DEFINIR_ID
          ? ev.lugar?.id || ev.idLugar
          : '',
      imagenUrl: ev.imagenUrl || '',
    },
    teloneros: parts.teloneros,
  }
}

/**
 * Edición de evento en BORRADOR (PUT /api/eventos/{id}).
 * Borrador laxo; la publicación completa se hace desde el listado.
 */
const GestionEventoEditarPage = () => {
  const { eventoId } = useParams()
  const { user, token, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(null)
  const [sectores, setSectores] = useState([])
  const [teloneros, setTeloneros] = useState([])
  const [loadStatus, setLoadStatus] = useState('loading')
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!FEATURE.eventosGestion) {
      setLoadStatus('gated')
      return
    }
    setLoadStatus('loading')
    setError('')
    try {
      const ev = await getEvento(eventoId, { token })
      const est = String(ev?.estado || '').toUpperCase()
      if (est !== EVENTO_ESTADOS.BORRADOR) {
        setError(
          `Solo se edita en borrador (estado actual: ${EVENTO_ESTADO_LABELS[est] || est}).`
        )
        setLoadStatus('blocked')
        setForm(null)
        return
      }
      if (
        !isAdmin &&
        ev.idOrganizador &&
        user?.id &&
        String(ev.idOrganizador) !== String(user.id)
      ) {
        setError('No sos el organizador de este evento.')
        setLoadStatus('blocked')
        setForm(null)
        return
      }
      const mapped = formFromDetalle(ev)
      setForm(mapped.form)
      setTeloneros(mapped.teloneros)
      // los sectores del lugar (con lo que el evento ya eligió: precio y cupo)
      let lugar = null
      if (mapped.form.idLugar) {
        try {
          lugar = await getLugar(mapped.form.idLugar, { token })
        } catch {
          lugar = null
        }
      }
      setSectores(lugar ? rowsDeLugar(lugar, ev.sectores) : [])
      setLoadStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar el evento')
      setLoadStatus('error')
    }
  }, [eventoId, token, user?.id, isAdmin])

  useEffect(() => {
    load()
  }, [load])

  const onChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  // Los sectores los define el lugar: acá solo se elige cuáles se venden y su precio/cupo.
  const onRowChange = (id, field, value) => {
    setSectores((prev) => prev.map((s) => (s.id === id ? { ...s, [field]: value } : s)))
  }

  const onElegirLugar = (lugar) => {
    setForm((prev) => ({ ...prev, idLugar: lugar?.id || '' }))
    setSectores(lugar ? rowsDeLugar(lugar) : [])
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
        esAltaDemanda: false,
        fechaInicio: form.fechaInicio ? form.fechaInicio : null,
        idLugar: form.idLugar || undefined,
        // lista completa de sectores del evento (en borrador reemplaza a la anterior);
        // solo id + precio (+ cupo si se cargó): nombre, tipo y layout los define el lugar
        sectores: check.sectores.map((s) => ({ ...s, precio: s.precio == null ? 0 : s.precio })),
      }
      delete payload.principal
      delete payload.principalNombre
      delete payload.principalId
      delete payload.participantes
      delete payload.teloneros

      await updateEvento(eventoId, payload, {
        token,
        userId: user?.id,
      })
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

  const loading = status === 'loading'
  const gated = !FEATURE.eventosGestion

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> /{' '}
          <Link to="/gestion/eventos">eventos</Link> / editar
        </p>
        <h1 className="gestion-page__title">Editar borrador</h1>
        <p className="gestion-page__sub">
          Solo borrador. Podés dejar el lugar y los sectores sin elegir; para publicar
          desde el listado hace falta fecha, un lugar y al menos un sector con precio.
          {gated && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {loadStatus === 'loading' && (
        <p className="gestion-card__muted">Cargando evento…</p>
      )}
      {error && <p className="gestion-error">{error}</p>}

      {(loadStatus === 'blocked' || loadStatus === 'error' || loadStatus === 'gated') && (
        <div className="gestion-card">
          <Link className="btn" to="/gestion/eventos">
            ← Volver al listado
          </Link>
        </div>
      )}

      {loadStatus === 'ok' && form && (
        <form className="gestion-form gestion-card" onSubmit={onSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="nombre">Nombre *</label>
            <input
              id="nombre"
              name="nombre"
              className="form-input"
              value={form.nombre}
              onChange={onChange}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="descripcion">Descripción</label>
            <textarea
              id="descripcion"
              name="descripcion"
              className="form-textarea"
              rows={3}
              value={form.descripcion}
              onChange={onChange}
              disabled={loading}
            />
          </div>

          <div className="gestion-form__row">
            <div className="form-group">
              <label htmlFor="tipo">Tipo de evento</label>
              <select
                id="tipo"
                name="tipo"
                className="form-input"
                value={form.tipo}
                onChange={onChange}
                disabled={loading}
              >
                {Object.values(EVENTO_TIPOS).map((t) => (
                  <option key={t} value={t}>
                    {EVENTO_TIPO_LABELS[t] || t}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Estado</label>
              <input
                className="form-input"
                value={EVENTO_ESTADO_LABELS.BORRADOR}
                disabled
                readOnly
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="fechaInicio">Fecha y hora (futura al publicar)</label>
            <input
              id="fechaInicio"
              name="fechaInicio"
              type="datetime-local"
              className="form-input"
              value={form.fechaInicio}
              onChange={onChange}
              disabled={loading}
            />
          </div>

          <EventoParticipantesFields
            principal={form.principal}
            onPrincipalChange={(v) =>
              setForm((prev) => ({ ...prev, principal: v }))
            }
            teloneros={teloneros}
            onTeloneroChange={(key, value) =>
              setTeloneros((prev) =>
                prev.map((t) =>
                  t.key === key
                    ? { ...t, id: value?.id || null, nombre: value?.nombre || '' }
                    : t
                )
              )
            }
            onAddTelonero={() => setTeloneros((prev) => [...prev, newTeloneroRow()])}
            onRemoveTelonero={(key) =>
              setTeloneros((prev) => prev.filter((t) => t.key !== key))
            }
            token={token}
            disabled={loading}
          />

          <EventoLugarSectores
            token={token}
            idLugar={form.idLugar}
            onElegirLugar={onElegirLugar}
            rows={sectores}
            onRowChange={onRowChange}
            publishing={false}
            disabled={loading}
            isAdmin={isAdmin}
          />

          <div className="form-group">
            <label htmlFor="imagenUrl">URL imagen (opcional)</label>
            <input
              id="imagenUrl"
              name="imagenUrl"
              className="form-input"
              value={form.imagenUrl}
              onChange={onChange}
              disabled={loading}
            />
          </div>

          <div className="gestion-card__actions">
            <GateButton
              enabled={FEATURE.eventosGestion}
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Guardando…' : 'Guardar borrador'}
            </GateButton>
            <Link className="btn" to="/gestion/eventos">
              Cancelar
            </Link>
          </div>
        </form>
      )}
    </section>
  )
}

export default GestionEventoEditarPage
