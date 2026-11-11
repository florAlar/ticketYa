import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import {
  actualizarParticipante,
  crearParticipanteAdmin,
  eliminarParticipante,
  listarParticipantes,
} from '../../api/participantes'
import {
  TIPOS_PARTICIPANTE,
  labelTipoParticipante,
  participantePayload,
  participanteVacio,
  validarParticipante,
} from '../../domain/participante'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import './GestionPage.css'

/**
 * Participantes (PAR-01) — solo ADMIN. Listado con búsqueda y filtro por tipo, alta, edición y baja
 * (409 si el participante está en algún evento: en ese caso solo se edita).
 * La banda principal NO es un atributo del participante: es el rol PRINCIPAL/TELONERO que se elige
 * por evento en el formulario del evento.
 */
const GestionParticipantesPage = () => {
  const { token } = useAuth()
  const wired = FEATURE.participantes
  const [items, setItems] = useState([])
  const [q, setQ] = useState('')
  const [tipo, setTipo] = useState('')
  const [filtro, setFiltro] = useState({ q: '', tipo: '' })
  const [status, setStatus] = useState(wired ? 'loading' : 'gated')
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [form, setForm] = useState(null) // null = cerrado; { id?, ...campos }
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    if (!wired) {
      setStatus('gated')
      return
    }
    setStatus('loading')
    setError('')
    try {
      setItems(await listarParticipantes(filtro, { token }))
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudieron cargar los participantes')
      setStatus('error')
    }
  }, [wired, filtro, token])

  useEffect(() => {
    load()
  }, [load])

  const onBuscar = (e) => {
    e.preventDefault()
    setFiltro({ q: q.trim(), tipo })
  }

  const onGuardar = async (e) => {
    e.preventDefault()
    const invalido = validarParticipante(form)
    if (invalido) {
      setError(invalido)
      return
    }
    setSaving(true)
    setError('')
    setAviso('')
    try {
      const payload = participantePayload(form)
      if (form.id) {
        await actualizarParticipante(form.id, payload, { token })
        setAviso(`«${payload.nombre}» actualizado.`)
      } else {
        await crearParticipanteAdmin(payload, { token })
        setAviso(`«${payload.nombre}» creado.`)
      }
      setForm(null)
      await load()
    } catch (err) {
      setError(
        err.status === 409
          ? 'Ya existe un participante con ese nombre: buscalo en la lista y editalo.'
          : err.message || 'No se pudo guardar'
      )
    } finally {
      setSaving(false)
    }
  }

  const onEliminar = async (p) => {
    if (!window.confirm(`¿Eliminar a «${p.nombre}» del catálogo?`)) return
    setBusyId(p.id)
    setError('')
    setAviso('')
    try {
      await eliminarParticipante(p.id, { token })
      setAviso(`«${p.nombre}» eliminado.`)
      await load()
    } catch (err) {
      setError(
        err.status === 409
          ? `«${p.nombre}» participa en eventos: no se puede eliminar, solo editar.`
          : err.message || 'No se pudo eliminar'
      )
    } finally {
      setBusyId(null)
    }
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / participantes
        </p>
        <h1 className="gestion-page__title">Participantes</h1>
        <p className="gestion-page__sub">
          Bandas, artistas, elencos, equipos y pilotos del catálogo. Los organizadores los eligen al
          armar el evento y ahí definen cuál es el principal y cuáles los teloneros.
          {!wired && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <div className="gestion-card__actions" style={{ marginBottom: '1rem' }}>
        <GateButton
          enabled={wired}
          type="button"
          className="btn btn-primary"
          onClick={() => {
            setError('')
            setForm(participanteVacio())
          }}
        >
          Nuevo participante
        </GateButton>
      </div>

      {wired && form && (
        <form className="gestion-card" onSubmit={onGuardar} style={{ marginBottom: '1rem' }}>
          <h2>{form.id ? 'Editar participante' : 'Nuevo participante'}</h2>
          <div className="form-group">
            <label htmlFor="par-nombre">Nombre *</label>
            <input
              id="par-nombre"
              className="form-input"
              value={form.nombre}
              onChange={set('nombre')}
              maxLength={200}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="par-tipo">Tipo *</label>
            <select id="par-tipo" className="form-input" value={form.tipo} onChange={set('tipo')}>
              {TIPOS_PARTICIPANTE.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="par-desc">Descripción</label>
            <textarea
              id="par-desc"
              className="form-input"
              rows={3}
              value={form.descripcion}
              onChange={set('descripcion')}
            />
          </div>
          <div className="gestion-card__actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
            <button type="button" className="btn" onClick={() => setForm(null)} disabled={saving}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {wired && (
        <form className="gestion-card" onSubmit={onBuscar} style={{ marginBottom: '1rem' }}>
          <div className="form-group">
            <label htmlFor="filtro-q">Buscar por nombre</label>
            <input
              id="filtro-q"
              className="form-input"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="ej. Metallica"
            />
          </div>
          <div className="form-group">
            <label htmlFor="filtro-tipo">Tipo</label>
            <select
              id="filtro-tipo"
              className="form-input"
              value={tipo}
              onChange={(e) => setTipo(e.target.value)}
            >
              <option value="">Todos</option>
              {TIPOS_PARTICIPANTE.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
          <div className="gestion-card__actions">
            <button type="submit" className="btn">
              Buscar
            </button>
            {(filtro.q || filtro.tipo) && (
              <button
                type="button"
                className="btn"
                onClick={() => {
                  setQ('')
                  setTipo('')
                  setFiltro({ q: '', tipo: '' })
                }}
              >
                Ver todos
              </button>
            )}
          </div>
        </form>
      )}

      {status === 'gated' && (
        <div className="gestion-card">
          <p>La gestión de participantes necesita el backend real: no hay versión mock.</p>
        </div>
      )}
      {status === 'loading' && <p className="gestion-card__muted">Cargando participantes…</p>}
      {aviso && <p className="gestion-card__muted" role="status">✔ {aviso}</p>}
      {error && <p className="gestion-error" role="alert">{error}</p>}

      {status === 'ok' && items.length === 0 && (
        <div className="gestion-card">
          <p>
            {filtro.q || filtro.tipo
              ? 'No hay participantes con ese filtro.'
              : 'Todavía no hay participantes cargados.'}
          </p>
        </div>
      )}

      {status === 'ok' && items.length > 0 && (
        <ul className="gestion-event-list">
          {items.map((p) => (
            <li key={p.id} className="gestion-event-row">
              <div className="gestion-event-row__main">
                <h2>{p.nombre}</h2>
                <p>{labelTipoParticipante(p.tipo)}</p>
                {p.descripcion && <p className="gestion-card__muted">{p.descripcion}</p>}
              </div>
              <div className="gestion-event-row__actions">
                <GateButton
                  enabled={wired}
                  type="button"
                  className="btn"
                  onClick={() => {
                    setError('')
                    setForm({
                      id: p.id,
                      nombre: p.nombre || '',
                      tipo: p.tipo || 'BANDA',
                      descripcion: p.descripcion || '',
                    })
                  }}
                >
                  Editar
                </GateButton>
                <GateButton
                  enabled={wired}
                  type="button"
                  className="btn"
                  disabled={busyId === p.id}
                  onClick={() => onEliminar(p)}
                >
                  Eliminar
                </GateButton>
              </div>
            </li>
          ))}
        </ul>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion">← Panel</Link>
      </p>
    </section>
  )
}

export default GestionParticipantesPage
