import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEvento, updateEvento, syncEventoParticipantes } from '../../api/eventos'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPOS,
  EVENTO_TIPO_LABELS,
  validateEventoGestionForm,
} from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import SectorCupoFields from '../../components/gestion/SectorCupoFields'
import EventoParticipantesFields, {
  newTeloneroRow,
  splitParticipantesForm,
} from '../../components/gestion/EventoParticipantesFields'
import './GestionPage.css'

const PRESETS_ENTRADA = [
  { nombre: 'Campo', precio: '15000', capacidad: '500', esNumerado: false },
  { nombre: 'Palco', precio: '45000', esNumerado: true, filas: '4', asientosPorFila: '10' },
  { nombre: 'Platea', precio: '25000', esNumerado: true, filas: '8', asientosPorFila: '15' },
  { nombre: 'General', precio: '10000', capacidad: '200', esNumerado: false },
  { nombre: 'VIP', precio: '60000', capacidad: '30', esNumerado: false },
]

let sectorKeySeq = 1
function newSectorRow(partial = {}) {
  return {
    key: `s_${Date.now()}_${sectorKeySeq++}`,
    id: partial.id || null,
    nombre: partial.nombre || '',
    precio: partial.precio != null ? String(partial.precio) : '',
    capacidad: partial.capacidad != null ? String(partial.capacidad) : '',
    esNumerado: Boolean(partial.esNumerado),
    filas: partial.filas != null ? String(partial.filas) : '',
    asientosPorFila:
      partial.asientosPorFila != null ? String(partial.asientosPorFila) : '',
  }
}

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
  const sectoresSrc =
    (Array.isArray(ev.sectores) && ev.sectores.length
      ? ev.sectores
      : ev.lugar?.sectores) || []
  const sectores = sectoresSrc.length
    ? sectoresSrc.map((s) =>
        newSectorRow({
          id: s.id,
          nombre: s.nombre,
          precio: s.precio ?? s.precioUnitario ?? '',
          capacidad: s.capacidadTotal ?? s.capacidad ?? '',
          esNumerado: s.esNumerado,
          filas: s.filas,
          asientosPorFila: s.asientosPorFila,
        })
      )
    : [newSectorRow()]

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
      idLugar: ev.lugar?.id || ev.idLugar || null,
      lugarNombre: ev.lugar?.nombre || '',
      lugarCiudad: ev.lugar?.ciudad || '',
      lugarDireccion: ev.lugar?.direccion || '',
      imagenUrl: ev.imagenUrl || '',
    },
    sectores,
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
      setSectores(mapped.sectores)
      setTeloneros(mapped.teloneros)
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

  const onSectorChange = (key, field, value) => {
    setSectores((prev) =>
      prev.map((s) => (s.key === key ? { ...s, [field]: value } : s))
    )
  }

  const addSector = (preset) => {
    setSectores((prev) => [
      ...prev,
      newSectorRow(
        preset || {
          nombre: '',
          precio: '',
          capacidad: '',
          esNumerado: false,
        }
      ),
    ])
  }

  const removeSector = (key) => {
    setSectores((prev) => (prev.length <= 1 ? prev : prev.filter((s) => s.key !== key)))
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
        sectores: check.sectores.map((s) => ({
          ...s,
          precio: s.precio == null ? 0 : s.precio,
          capacidadTotal: s.capacidadTotal == null ? 0 : s.capacidadTotal,
          sinCupo: false,
        })),
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
          Solo borrador. Podés dejar lugar/cupos incompletos; al publicar desde el
          listado el backend exige todo completo y fecha futura.
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

          <div className="form-group">
            <label htmlFor="lugarNombre">Lugar</label>
            <input
              id="lugarNombre"
              name="lugarNombre"
              className="form-input"
              value={form.lugarNombre}
              onChange={onChange}
              disabled={loading}
            />
          </div>

          <div className="gestion-form__row">
            <div className="form-group">
              <label htmlFor="lugarCiudad">Ciudad</label>
              <input
                id="lugarCiudad"
                name="lugarCiudad"
                className="form-input"
                value={form.lugarCiudad}
                onChange={onChange}
                disabled={loading}
              />
            </div>
            <div className="form-group">
              <label htmlFor="lugarDireccion">Dirección</label>
              <input
                id="lugarDireccion"
                name="lugarDireccion"
                className="form-input"
                value={form.lugarDireccion}
                onChange={onChange}
                disabled={loading}
              />
            </div>
          </div>

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

          <h2 className="gestion-page__title" style={{ fontSize: '1.05rem' }}>
            Tipos de entrada
          </h2>
          <p className="gestion-card__muted" style={{ marginBottom: '0.75rem' }}>
            Presets solo al click. Cupo obligatorio (≥ 1) al publicar; acá podés
            dejarlo incompleto. No hay cupo ilimitado.
          </p>
          <div className="gestion-card__actions" style={{ marginBottom: '0.75rem' }}>
            {PRESETS_ENTRADA.map((p) => (
              <button
                key={p.nombre}
                type="button"
                className="btn"
                disabled={loading}
                onClick={() => addSector(p)}
              >
                + {p.nombre}
              </button>
            ))}
            <button
              type="button"
              className="btn"
              disabled={loading}
              onClick={() => addSector()}
            >
              + Personalizado
            </button>
          </div>

          {sectores.map((s) => (
            <div
              key={s.key}
              className="gestion-form__row"
              style={{
                alignItems: 'flex-end',
                marginBottom: '0.65rem',
                gap: '0.5rem',
              }}
            >
              <div className="form-group" style={{ flex: 1.2 }}>
                <label>Nombre</label>
                <input
                  className="form-input"
                  value={s.nombre}
                  onChange={(e) => onSectorChange(s.key, 'nombre', e.target.value)}
                  disabled={loading}
                />
              </div>
              <div className="form-group" style={{ flex: 0.7 }}>
                <label>Precio</label>
                <input
                  className="form-input"
                  type="number"
                  min="0"
                  value={s.precio}
                  onChange={(e) => onSectorChange(s.key, 'precio', e.target.value)}
                  disabled={loading}
                />
              </div>
              <SectorCupoFields
                sector={s}
                idPrefix={`sec-${s.key}`}
                disabled={loading}
                groupStyle={{ flex: 0.7 }}
                onChange={(field, value) => onSectorChange(s.key, field, value)}
              />
              <label className="gestion-check" style={{ marginBottom: '0.85rem' }}>
                <input
                  type="checkbox"
                  checked={s.esNumerado}
                  disabled={loading}
                  onChange={(e) =>
                    onSectorChange(s.key, 'esNumerado', e.target.checked)
                  }
                />
                Numerado
              </label>
              <button
                type="button"
                className="btn"
                disabled={loading || sectores.length <= 1}
                onClick={() => removeSector(s.key)}
              >
                Quitar
              </button>
            </div>
          ))}

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
