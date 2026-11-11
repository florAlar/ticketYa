import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import {
  createLugar,
  createSector,
  getLugar,
  updateLugar,
  updateSector,
} from '../../api/lugares'
import {
  FORM_LUGAR_VACIO,
  FORM_SECTOR_VACIO,
  TIPOS_SECTOR,
  describirSector,
  lugarFormToPayload,
  lugarToForm,
  sectorFormToPayload,
  sectorToForm,
  validateLugarForm,
  validateSectorForm,
} from '../../domain/lugar'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import SectorCupoFields from '../../components/gestion/SectorCupoFields'
import './GestionPage.css'

/**
 * Alta y edición de un lugar (LUG-01) — solo ADMIN. En edición muestra además los sectores del
 * lugar (alta y edición; los sectores no se eliminan). Ruta: /gestion/lugares/nuevo | /:lugarId.
 */
const GestionLugarFormPage = () => {
  const { lugarId } = useParams()
  const esNuevo = !lugarId
  const navigate = useNavigate()
  const { token } = useAuth()
  const wired = FEATURE.lugares

  const [form, setForm] = useState(FORM_LUGAR_VACIO)
  const [lugar, setLugar] = useState(null)
  const [status, setStatus] = useState(esNuevo || !wired ? 'ok' : 'loading')
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [saving, setSaving] = useState(false)

  const [editando, setEditando] = useState(null) // null | 'nuevo' | idSector
  const [sectorForm, setSectorForm] = useState(FORM_SECTOR_VACIO)
  const [sectorError, setSectorError] = useState('')
  const [sectorSaving, setSectorSaving] = useState(false)

  const cargar = useCallback(async () => {
    if (esNuevo || !wired) return
    setStatus('loading')
    setError('')
    try {
      const data = await getLugar(lugarId, { token })
      setLugar(data)
      setForm(lugarToForm(data))
      setStatus('ok')
    } catch (err) {
      setError(err.status === 404 ? 'El lugar no existe.' : err.message || 'No se pudo cargar el lugar')
      setStatus('error')
    }
  }, [esNuevo, wired, lugarId, token])

  useEffect(() => {
    cargar()
  }, [cargar])

  const onChange = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!wired) return
    const v = validateLugarForm(form)
    if (!v.ok) {
      setError(v.error)
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
        setLugar(actualizado)
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
    setSectorForm((f) => ({ ...f, [field]: value }))
  }

  const guardarSector = async (e) => {
    e.preventDefault()
    const v = validateSectorForm(sectorForm)
    if (!v.ok) {
      setSectorError(v.error)
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
      await cargar()
    } catch (err) {
      setSectorError(err.message || 'No se pudo guardar el sector')
    } finally {
      setSectorSaving(false)
    }
  }

  const titulo = esNuevo ? 'Nuevo lugar' : lugar?.nombre || 'Editar lugar'

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / <Link to="/gestion/lugares">lugares</Link> /{' '}
          {esNuevo ? 'nuevo' : 'editar'}
        </p>
        <h1 className="gestion-page__title">{titulo}</h1>
        <p className="gestion-page__sub">
          Nombre, ciudad y dirección; la ubicación (latitud y longitud) es opcional.
          {!wired && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {status === 'loading' && <p className="gestion-card__muted">Cargando…</p>}
      {status === 'error' && <p className="gestion-error" role="alert">{error}</p>}

      {status === 'ok' && (
        <form className="gestion-card gestion-form" onSubmit={onSubmit}>
          <div className="form-group">
            <label htmlFor="nombre">Nombre</label>
            <input
              id="nombre"
              name="nombre"
              className="form-input"
              value={form.nombre}
              onChange={onChange}
              disabled={saving || !wired}
              placeholder="Estadio Más Monumental"
            />
          </div>
          <div className="gestion-form__row">
            <div className="form-group">
              <label htmlFor="ciudad">Ciudad</label>
              <input
                id="ciudad"
                name="ciudad"
                className="form-input"
                value={form.ciudad}
                onChange={onChange}
                disabled={saving || !wired}
                placeholder="Buenos Aires"
              />
            </div>
            <div className="form-group">
              <label htmlFor="direccion">Dirección</label>
              <input
                id="direccion"
                name="direccion"
                className="form-input"
                value={form.direccion}
                onChange={onChange}
                disabled={saving || !wired}
                placeholder="Av. Figueroa Alcorta 7597"
              />
            </div>
          </div>
          <div className="gestion-form__row">
            <div className="form-group">
              <label htmlFor="latitud">Latitud (opcional)</label>
              <input
                id="latitud"
                name="latitud"
                inputMode="decimal"
                className="form-input"
                value={form.latitud}
                onChange={onChange}
                disabled={saving || !wired}
                placeholder="-34.5453"
              />
            </div>
            <div className="form-group">
              <label htmlFor="longitud">Longitud (opcional)</label>
              <input
                id="longitud"
                name="longitud"
                inputMode="decimal"
                className="form-input"
                value={form.longitud}
                onChange={onChange}
                disabled={saving || !wired}
                placeholder="-58.4497"
              />
            </div>
          </div>
          <p className="gestion-card__muted">
            Latitud entre -90 y 90, longitud entre -180 y 180. Se completan las dos o ninguna. Si el
            lugar tenía coordenadas y vaciás los dos campos, se borran.
          </p>

          {error && <p className="gestion-error" role="alert">{error}</p>}
          {aviso && <p className="gestion-card__muted" role="status">✔ {aviso}</p>}

          <div className="gestion-card__actions">
            <GateButton enabled={wired} type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : esNuevo ? 'Crear lugar' : 'Guardar cambios'}
            </GateButton>
            <Link className="btn" to="/gestion/lugares">
              {esNuevo ? 'Cancelar' : 'Volver'}
            </Link>
          </div>
        </form>
      )}

      {status === 'ok' && !esNuevo && lugar && (
        <div className="gestion-card" style={{ marginTop: '1rem' }}>
          <h2>Sectores</h2>
          <p className="gestion-card__muted">
            Capacidad total del lugar: {lugar.capacidadTotal ?? 0}. Un sector numerado define su
            capacidad con filas × asientos por fila. Los sectores no se eliminan; los cambios afectan a
            los eventos que usan este lugar.
          </p>

          {(lugar.sectores || []).length === 0 && editando !== 'nuevo' && (
            <p className="gestion-card__muted">Este lugar todavía no tiene sectores.</p>
          )}

          <ul className="gestion-sectores-list">
            {(lugar.sectores || []).map((s) => (
              <li key={s.id} className="gestion-sector-row">
                <div className="gestion-sector-row__head">
                  <div>
                    <strong>{s.nombre}</strong>{' '}
                    <span className="gestion-card__muted">
                      {s.tipo ? `${s.tipo} · ` : ''}
                      {describirSector(s)}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="btn"
                    disabled={editando !== null}
                    onClick={() => abrirSector(s)}
                  >
                    Editar
                  </button>
                </div>
              </li>
            ))}
          </ul>

          {editando === null && (
            <div className="gestion-card__actions">
              <GateButton
                enabled={wired}
                type="button"
                className="btn btn-primary"
                onClick={() => abrirSector(null)}
              >
                + Agregar sector
              </GateButton>
            </div>
          )}

          {editando !== null && (
            <form className="gestion-sector-row" onSubmit={guardarSector}>
              <div className="gestion-sector-row__head">
                <strong>{editando === 'nuevo' ? 'Nuevo sector' : 'Editar sector'}</strong>
              </div>
              <div className="gestion-form__row">
                <div className="form-group">
                  <label htmlFor="sec-nombre">Nombre</label>
                  <input
                    id="sec-nombre"
                    className="form-input"
                    value={sectorForm.nombre}
                    onChange={(e) => onSectorChange('nombre', e.target.value)}
                    disabled={sectorSaving}
                    placeholder="Platea baja"
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="sec-tipo">Tipo</label>
                  <select
                    id="sec-tipo"
                    className="form-input"
                    value={sectorForm.tipo}
                    onChange={(e) => onSectorChange('tipo', e.target.value)}
                    disabled={sectorSaving}
                  >
                    {[...new Set([...TIPOS_SECTOR, sectorForm.tipo].filter(Boolean))].map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>
                <SectorCupoFields
                  sector={sectorForm}
                  idPrefix="sec"
                  disabled={sectorSaving}
                  onChange={onSectorChange}
                />
              </div>
              <div className="gestion-checks">
                <label className="gestion-check">
                  <input
                    type="checkbox"
                    checked={sectorForm.esNumerado}
                    onChange={(e) => onSectorChange('esNumerado', e.target.checked)}
                    disabled={sectorSaving}
                  />
                  Numerado (asientos)
                </label>
              </div>
              {sectorError && <p className="gestion-error" role="alert">{sectorError}</p>}
              <div className="gestion-card__actions">
                <button type="submit" className="btn btn-primary" disabled={sectorSaving}>
                  {sectorSaving ? 'Guardando…' : 'Guardar sector'}
                </button>
                <button
                  type="button"
                  className="btn"
                  disabled={sectorSaving}
                  onClick={() => setEditando(null)}
                >
                  Cancelar
                </button>
              </div>
            </form>
          )}
        </div>
      )}
    </section>
  )
}

export default GestionLugarFormPage
