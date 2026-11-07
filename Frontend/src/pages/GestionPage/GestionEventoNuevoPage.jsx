import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { createEvento } from '../../api/eventos'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPOS,
  EVENTO_TIPO_LABELS,
} from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import './GestionPage.css'

/** Presets de tipos de entrada (admin/org puede editar o agregar más). */
const PRESETS_ENTRADA = [
  { key: 'campo', nombre: 'Campo', precio: '15000', capacidad: '500', sinCupo: false, esNumerado: false },
  { key: 'palco', nombre: 'Palco', precio: '45000', capacidad: '40', sinCupo: false, esNumerado: true },
  { key: 'platea', nombre: 'Platea', precio: '25000', capacidad: '120', sinCupo: false, esNumerado: true },
  { key: 'general', nombre: 'General', precio: '10000', capacidad: '200', sinCupo: false, esNumerado: false },
  { key: 'vip', nombre: 'VIP', precio: '60000', capacidad: '30', sinCupo: false, esNumerado: false },
]

let sectorKeySeq = 1
function newSectorRow(partial = {}) {
  return {
    key: `s_${Date.now()}_${sectorKeySeq++}`,
    nombre: partial.nombre || '',
    precio: partial.precio != null ? String(partial.precio) : '0',
    capacidad: partial.capacidad != null ? String(partial.capacidad) : '',
    sinCupo: Boolean(partial.sinCupo),
    esNumerado: Boolean(partial.esNumerado),
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
  participantes: '',
  lugarNombre: '',
  lugarCiudad: '',
  lugarDireccion: '',
  imagenUrl: '',
}

const GestionEventoNuevoPage = () => {
  const { user, token, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [sectores, setSectores] = useState([
    newSectorRow({ nombre: 'Campo', precio: '15000', capacidad: '500' }),
    newSectorRow({ nombre: 'Palco', precio: '45000', capacidad: '40', esNumerado: true }),
  ])
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
          precio: '0',
          capacidad: '',
          sinCupo: false,
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
      const cleaned = sectores
        .map((s) => ({
          nombre: String(s.nombre || '').trim(),
          precio: Number(s.precio) || 0,
          sinCupo: Boolean(s.sinCupo),
          capacidadTotal: s.sinCupo ? null : Number(s.capacidad) || 0,
          esNumerado: Boolean(s.esNumerado),
        }))
        .filter((s) => s.nombre)

      if (!cleaned.length) {
        setError('Agregá al menos un tipo de entrada con nombre')
        setStatus('error')
        return
      }

      const payload = {
        ...form,
        sectores: cleaned,
      }
      const created = await createEvento(payload, user, { token })
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

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> /{' '}
          <Link to="/gestion/eventos">eventos</Link> / nuevo
        </p>
        <h1 className="gestion-page__title">Crear evento</h1>
        <p className="gestion-page__sub">
          Podés cargar varios tipos de entrada (Campo, Palco, etc.), con o sin
          cupo, y sumar más filas.
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
      <form className="gestion-form gestion-card" onSubmit={onSubmit}>
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
            <label htmlFor="estado">Estado</label>
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
          <label htmlFor="fechaInicio">Fecha y hora *</label>
          <input
            id="fechaInicio"
            name="fechaInicio"
            type="datetime-local"
            className="form-input"
            value={form.fechaInicio}
            onChange={onChange}
            required
            disabled={loading}
          />
        </div>

        <div className="form-group">
          <label htmlFor="participantes">Participantes (coma)</label>
          <input
            id="participantes"
            name="participantes"
            className="form-input"
            placeholder="Artista 1, Artista 2"
            value={form.participantes}
            onChange={onChange}
            disabled={loading}
          />
        </div>

        <fieldset className="gestion-fieldset">
          <legend>Lugar</legend>
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
          <legend>Tipos de entrada</legend>
          <p className="gestion-card__muted" style={{ marginBottom: '0.75rem' }}>
            Cada fila es un tipo vendible (Campo, Palco, Platea…). Sin cupo =
            capacidad ilimitada en el mock. Numerado = asientos (UI de mapa
            después).
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
                    <label htmlFor={`sec-nom-${s.key}`}>Nombre *</label>
                    <input
                      id={`sec-nom-${s.key}`}
                      className="form-input"
                      value={s.nombre}
                      onChange={(e) =>
                        onSectorChange(s.key, 'nombre', e.target.value)
                      }
                      placeholder="Campo, Palco, VIP…"
                      required
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
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor={`sec-cap-${s.key}`}>
                      Cupo {s.sinCupo ? '(ilimitado)' : ''}
                    </label>
                    <input
                      id={`sec-cap-${s.key}`}
                      type="number"
                      min="0"
                      className="form-input"
                      value={s.sinCupo ? '' : s.capacidad}
                      onChange={(e) =>
                        onSectorChange(s.key, 'capacidad', e.target.value)
                      }
                      disabled={loading || s.sinCupo}
                      placeholder={s.sinCupo ? 'Sin límite' : 'ej. 500'}
                    />
                  </div>
                </div>
                <div className="gestion-checks">
                  <label className="gestion-check">
                    <input
                      type="checkbox"
                      checked={s.sinCupo}
                      onChange={(e) =>
                        onSectorChange(s.key, 'sinCupo', e.target.checked)
                      }
                      disabled={loading}
                    />
                    Sin cupo
                  </label>
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
            {loading ? 'Guardando…' : 'Guardar evento'}
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
