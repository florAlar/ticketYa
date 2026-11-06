import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import {
  createPersonalAcceso,
  listUsuarios,
  removeUsuario,
  updateAccesoEventos,
  updateUsuario,
} from '../../api/usuarios'
import { listEventosGestion } from '../../api/eventos'
import { ROLE_LABELS, ROLES, normalizeRol } from '../../auth/roles'
import '../GestionPage/GestionPage.css'

const GestionUsuariosPage = () => {
  const { user, token } = useAuth()
  const [items, setItems] = useState([])
  const [eventos, setEventos] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({
    nombre: '',
    email: '',
    password: '',
    eventosAsignados: [],
  })

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const [users, evs] = await Promise.all([
        listUsuarios(user, { token }),
        listEventosGestion(user, { isAdmin: true, token }),
      ])
      setItems(Array.isArray(users) ? users : [])
      setEventos(Array.isArray(evs) ? evs : [])
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar')
      setStatus('error')
    }
  }, [user, token])

  useEffect(() => {
    load()
  }, [load])

  const eventoOptions = useMemo(
    () =>
      eventos.map((e) => ({
        id: e.id,
        label: `${e.nombre} (${e.id})`,
      })),
    [eventos]
  )

  const onRol = async (id, rol) => {
    setBusyId(id)
    setError('')
    try {
      await updateUsuario(id, { rol }, user, { token })
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo cambiar el rol')
    } finally {
      setBusyId(null)
    }
  }

  const onActivo = async (id, activo) => {
    setBusyId(id)
    setError('')
    try {
      await updateUsuario(id, { activo }, user, { token })
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo cambiar el estado')
    } finally {
      setBusyId(null)
    }
  }

  const onDelete = async (id) => {
    if (!window.confirm('¿Eliminar este usuario? (personal de acceso no se desactiva)')) {
      return
    }
    setBusyId(id)
    setError('')
    try {
      await removeUsuario(id, user, { token })
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo eliminar')
    } finally {
      setBusyId(null)
    }
  }

  const onToggleEvento = async (u, eventoId) => {
    const cur = new Set(u.eventosAsignados || [])
    if (cur.has(eventoId)) cur.delete(eventoId)
    else cur.add(eventoId)
    setBusyId(u.id)
    setError('')
    try {
      await updateAccesoEventos(u.id, [...cur], user, { token })
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo actualizar asignaciones')
    } finally {
      setBusyId(null)
    }
  }

  const onCreate = async (e) => {
    e.preventDefault()
    setError('')
    setBusyId('create')
    try {
      await createPersonalAcceso(form, user, { token })
      setForm({ nombre: '', email: '', password: '', eventosAsignados: [] })
      setShowCreate(false)
      await load()
    } catch (err) {
      setError(err.message || 'No se pudo crear')
    } finally {
      setBusyId(null)
    }
  }

  const toggleFormEvento = (eventoId) => {
    setForm((prev) => {
      const set = new Set(prev.eventosAsignados)
      if (set.has(eventoId)) set.delete(eventoId)
      else set.add(eventoId)
      return { ...prev, eventosAsignados: [...set] }
    })
  }

  return (
    <section className="gestion-page container section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / usuarios
        </p>
        <h1 className="gestion-page__title">Usuarios</h1>
        <p className="gestion-page__sub">
          Admin: edita solo <strong>rol</strong> y <strong>activo</strong>. Personal
          de acceso se crea acá y se asigna a eventos (no se desactiva: se elimina).
        </p>
      </header>

      <div className="gestion-card__actions" style={{ marginBottom: '1rem' }}>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setShowCreate((v) => !v)}
        >
          {showCreate ? 'Cerrar alta' : 'Nuevo personal de acceso'}
        </button>
        <button type="button" className="btn" onClick={load}>
          Actualizar
        </button>
      </div>

      {error && <p className="gestion-error">{error}</p>}
      {status === 'loading' && <p className="gestion-card__muted">Cargando…</p>}

      {showCreate && (
        <form className="gestion-card gestion-form" onSubmit={onCreate}>
          <h2 style={{ margin: 0, fontSize: '1.05rem' }}>Alta ACCESO</h2>
          <div className="gestion-form__row">
            <div className="form-group">
              <label htmlFor="acc-nombre">Nombre</label>
              <input
                id="acc-nombre"
                className="form-input"
                value={form.nombre}
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="acc-email">Email</label>
              <input
                id="acc-email"
                type="email"
                className="form-input"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="acc-pass">Contraseña</label>
              <input
                id="acc-pass"
                type="text"
                className="form-input"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </div>
          </div>
          <p className="gestion-card__muted">Eventos asignados</p>
          <div className="gestion-checks">
            {eventoOptions.length === 0 && (
              <span className="gestion-card__muted">No hay eventos en el store.</span>
            )}
            {eventoOptions.map((ev) => (
              <label key={ev.id} className="gestion-check">
                <input
                  type="checkbox"
                  checked={form.eventosAsignados.includes(ev.id)}
                  onChange={() => toggleFormEvento(ev.id)}
                />
                {ev.label}
              </label>
            ))}
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={busyId === 'create'}
          >
            {busyId === 'create' ? 'Creando…' : 'Crear acceso'}
          </button>
        </form>
      )}

      {status === 'ok' && (
        <ul className="gestion-event-list">
          {items.map((u) => {
            const isAcceso = normalizeRol(u.rol) === ROLES.ACCESO
            return (
              <li key={u.id} className="gestion-event-row">
                <div className="gestion-event-row__main">
                  <h2>
                    {u.nombre}{' '}
                    <span className="gestion-card__muted">· {u.email}</span>
                  </h2>
                  <p className="gestion-card__muted">
                    id: {u.id}
                    {!u.activo && !isAcceso ? ' · DESACTIVADO' : ''}
                  </p>
                  <div className="gestion-form__row" style={{ marginTop: '0.5rem' }}>
                    <div className="form-group">
                      <label htmlFor={`rol-${u.id}`}>Rol</label>
                      <select
                        id={`rol-${u.id}`}
                        className="form-input"
                        value={normalizeRol(u.rol)}
                        disabled={busyId === u.id || u.id === user?.id}
                        onChange={(e) => onRol(u.id, e.target.value)}
                      >
                        {Object.values(ROLES).map((r) => (
                          <option key={r} value={r}>
                            {ROLE_LABELS[r] || r}
                          </option>
                        ))}
                      </select>
                    </div>
                    {!isAcceso && (
                      <div className="form-group">
                        <label htmlFor={`act-${u.id}`}>Activo</label>
                        <select
                          id={`act-${u.id}`}
                          className="form-input"
                          value={u.activo ? '1' : '0'}
                          disabled={busyId === u.id || u.id === user?.id}
                          onChange={(e) => onActivo(u.id, e.target.value === '1')}
                        >
                          <option value="1">Sí</option>
                          <option value="0">No</option>
                        </select>
                      </div>
                    )}
                  </div>

                  {isAcceso && (
                    <div style={{ marginTop: '0.65rem' }}>
                      <p className="gestion-card__muted">Eventos asignados (scan)</p>
                      <div className="gestion-checks">
                        {eventoOptions.map((ev) => (
                          <label key={ev.id} className="gestion-check">
                            <input
                              type="checkbox"
                              checked={(u.eventosAsignados || []).includes(ev.id)}
                              disabled={busyId === u.id}
                              onChange={() => onToggleEvento(u, ev.id)}
                            />
                            {ev.label}
                          </label>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
                <div className="gestion-event-row__actions">
                  {isAcceso && (
                    <button
                      type="button"
                      className="btn"
                      disabled={busyId === u.id}
                      onClick={() => onDelete(u.id)}
                    >
                      Eliminar
                    </button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion">← Panel</Link>
      </p>
    </section>
  )
}

export default GestionUsuariosPage
