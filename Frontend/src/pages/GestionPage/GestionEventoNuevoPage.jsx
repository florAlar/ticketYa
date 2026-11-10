import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { createEvento, syncEventoParticipantes } from '../../api/eventos'
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
} from '../../components/gestion/EventoParticipantesFields'
import './GestionPage.css'

/** Presets solo al click (no prearmados en el form). */
const PRESETS_ENTRADA = [
  { key: 'campo', nombre: 'Campo', precio: '15000', capacidad: '500', esNumerado: false },
  { key: 'palco', nombre: 'Palco', precio: '45000', esNumerado: true, filas: '4', asientosPorFila: '10' },
  { key: 'platea', nombre: 'Platea', precio: '25000', esNumerado: true, filas: '8', asientosPorFila: '15' },
  { key: 'general', nombre: 'General', precio: '10000', capacidad: '200', esNumerado: false },
  { key: 'vip', nombre: 'VIP', precio: '60000', capacidad: '30', esNumerado: false },
]

let sectorKeySeq = 1
function newSectorRow(partial = {}) {
  return {
    key: `s_${Date.now()}_${sectorKeySeq++}`,
    nombre: partial.nombre || '',
    precio: partial.precio != null ? String(partial.precio) : '',
    capacidad: partial.capacidad != null ? String(partial.capacidad) : '',
    esNumerado: Boolean(partial.esNumerado),
    filas: partial.filas != null ? String(partial.filas) : '',
    asientosPorFila:
      partial.asientosPorFila != null ? String(partial.asientosPorFila) : '',
  }
}

const initialForm = {
  nombre: '',
  descripcion: '',
  tipo: EVENTO_TIPOS.RECITAL,
  fechaInicio: '',
  estado: EVENTO_ESTADOS.BORRADOR,
  esAltaDemanda: false,
  destacado: false,
  principal: { id: null, nombre: '' },
  lugarNombre: '',
  lugarCiudad: '',
  lugarDireccion: '',
  imagenUrl: '',
}

const GestionEventoNuevoPage = () => {
  const { user, token, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [sectores, setSectores] = useState([newSectorRow()])
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
        estado: wantsPublish ? EVENTO_ESTADOS.PUBLICADO : EVENTO_ESTADOS.BORRADOR,
        esAltaDemanda: false,
        fechaInicio: form.fechaInicio ? form.fechaInicio : null,
        sectores: check.sectores.map((s) => ({
          ...s,
          // BE espera números; borrador puede mandar null → 0 en apply
          precio: s.precio == null ? 0 : s.precio,
          capacidadTotal: s.capacidadTotal == null ? 0 : s.capacidadTotal,
          sinCupo: false,
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

  const loading = status === 'loading'
  const gated = !FEATURE.eventosGestion
  const publishing =
    String(form.estado || '').toUpperCase() === EVENTO_ESTADOS.PUBLICADO

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> /{' '}
          <Link to="/gestion/eventos">eventos</Link> / nuevo
        </p>
        <h1 className="gestion-page__title">Crear evento</h1>
        <p className="gestion-page__sub">
          Borrador: alcanza con el nombre (el resto se puede completar después).
          Publicar: fecha futura, lugar, y tipos de entrada con cupo ≥ 1. No hay
          cupo ilimitado (sí podés marcar no numerado).
          {isAdmin ? ' · Alta como admin.' : ` · org: ${user?.id}`}
          {gated && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {gated ? (
        <div className="gestion-card">
          <p>Alta de eventos no disponible: backend de eventos en mock o no cableado.</p>
          <Link className="btn" to="/gestion/eventos">
            ← Volver al listado
          </Link>
        </div>
      ) : (
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
              <label htmlFor="estado">Al guardar como</label>
              <select
                id="estado"
                name="estado"
                className="form-input"
                value={form.estado}
                onChange={onChange}
                disabled={loading}
              >
                {[EVENTO_ESTADOS.BORRADOR, EVENTO_ESTADOS.PUBLICADO].map((s) => (
                  <option key={s} value={s}>
                    {EVENTO_ESTADO_LABELS[s]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="fechaInicio">
              Fecha y hora {publishing ? '*' : '(obligatoria al publicar)'}
            </label>
            <input
              id="fechaInicio"
              name="fechaInicio"
              type="datetime-local"
              className="form-input"
              value={form.fechaInicio}
              onChange={onChange}
              disabled={loading}
            />
            <p className="gestion-card__muted" style={{ marginTop: '0.35rem' }}>
              En borrador podés dejarla vacía y completarla después. Al publicar
              es obligatoria.
            </p>
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

          <fieldset className="gestion-fieldset">
            <legend>Lugar {publishing ? '*' : '(completo al publicar)'}</legend>
            <div className="form-group">
              <label htmlFor="lugarNombre">Nombre del lugar</label>
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
          </fieldset>

          <fieldset className="gestion-fieldset">
            <legend>Tipos de entrada {publishing ? '*' : '(opcionales en borrador)'}</legend>
            <p className="gestion-card__muted" style={{ marginBottom: '0.75rem' }}>
              Una fila vacía al inicio. Usá presets solo si querés. Cupo ≥ 1 al
              publicar (siempre hay cupo). Numerado = filas × asientos por fila:
              el cupo se calcula solo y el cliente elige su asiento al comprar.
              En borrador podés dejar el layout incompleto.
            </p>

            <div className="gestion-presets">
              <span className="gestion-card__muted">Agregar preset:</span>
              {PRESETS_ENTRADA.map((p) => (
                <button
                  key={p.key}
                  type="button"
                  className="btn gestion-preset-btn"
                  disabled={loading}
                  onClick={() => addSector(p)}
                >
                  + {p.nombre}
                </button>
              ))}
              <button
                type="button"
                className="btn btn-primary gestion-preset-btn"
                disabled={loading}
                onClick={() => addSector(null)}
              >
                + Otro tipo
              </button>
            </div>

            <ul className="gestion-sectores-list">
              {sectores.map((s, idx) => (
                <li key={s.key} className="gestion-sector-row">
                  <div className="gestion-sector-row__head">
                    <strong>Tipo {idx + 1}</strong>
                    <button
                      type="button"
                      className="btn"
                      disabled={loading || sectores.length <= 1}
                      onClick={() => removeSector(s.key)}
                    >
                      Quitar
                    </button>
                  </div>
                  <div className="gestion-form__row">
                    <div className="form-group">
                      <label htmlFor={`sec-nom-${s.key}`}>Nombre</label>
                      <input
                        id={`sec-nom-${s.key}`}
                        className="form-input"
                        value={s.nombre}
                        onChange={(e) =>
                          onSectorChange(s.key, 'nombre', e.target.value)
                        }
                        placeholder="Campo, Palco, VIP…"
                        disabled={loading}
                      />
                    </div>
                    <div className="form-group">
                      <label htmlFor={`sec-pre-${s.key}`}>Precio</label>
                      <input
                        id={`sec-pre-${s.key}`}
                        type="number"
                        min="0"
                        className="form-input"
                        value={s.precio}
                        onChange={(e) =>
                          onSectorChange(s.key, 'precio', e.target.value)
                        }
                        disabled={loading}
                        placeholder="0"
                      />
                    </div>
                    <SectorCupoFields
                      sector={s}
                      idPrefix={`sec-${s.key}`}
                      disabled={loading}
                      onChange={(field, value) =>
                        onSectorChange(s.key, field, value)
                      }
                    />
                  </div>
                  <div className="gestion-checks">
                    <label className="gestion-check">
                      <input
                        type="checkbox"
                        checked={s.esNumerado}
                        onChange={(e) =>
                          onSectorChange(s.key, 'esNumerado', e.target.checked)
                        }
                        disabled={loading}
                      />
                      Numerado (asientos)
                    </label>
                  </div>
                </li>
              ))}
            </ul>
          </fieldset>

          <div className="gestion-checks">
            <label className="gestion-check">
              <input
                type="checkbox"
                name="esAltaDemanda"
                checked={form.esAltaDemanda}
                onChange={onChange}
                disabled={loading || true}
                title="Fila virtual E2 — no cableada"
              />
              Alta demanda (fila virtual — no disponible aún)
            </label>
            <label className="gestion-check">
              <input
                type="checkbox"
                name="destacado"
                checked={form.destacado}
                onChange={onChange}
                disabled={loading}
              />
              Destacado en home
            </label>
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

          {error && <p className="gestion-error">{error}</p>}

          <div className="gestion-card__actions">
            <GateButton
              enabled={FEATURE.eventosGestion}
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading
                ? 'Guardando…'
                : publishing
                  ? 'Crear y publicar'
                  : 'Guardar borrador'}
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

export default GestionEventoNuevoPage
