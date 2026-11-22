import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { getMe, patchMe } from '../../api/auth'
import { isAbortError } from '../../api/http'
import { ROLE_LABELS, normalizeRol } from '../../auth/roles'
import { FEATURE } from '../../config/featureGates'
import Badge from '../../components/ui/Badge'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import PreferenciasForm from '../../components/preferencias/PreferenciasForm'
import HistorialCliente from '../../components/preferencias/HistorialCliente'
import './PerfilPage.css'

function emptyForm() {
  return { nombre: '', apellido: '', ciudad: '' }
}

function formFromProfile(p) {
  return {
    nombre: p?.nombre || '',
    apellido: p?.apellido || '',
    ciudad: p?.ciudad || '',
  }
}

/**
 * Mi perfil — GET/PATCH /api/usuarios/me
 * Dos columnas: identidad + edición (+ CTA organizador) | gustos y recomendaciones.
 */
const PerfilPage = () => {
  const [tab, setTab] = useState('prefs')
  const { token, user, updateUser, isCliente } = useAuth()

  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  const load = useCallback(
    async (signal) => {
      try {
        const me = await getMe({ token, signal })
        const next = me || user
        if (!next) throw new Error('No se pudo cargar el perfil')
        if (me) updateUser(me)
        return next
      } catch (err) {
        if (isAbortError(err)) throw err
        // Sin red, se usa la sesión local para no bloquear el perfil.
        if (user) return user
        throw err
      }
    },
    [token, user, updateUser]
  )

  const {
    data: profile,
    status,
    error: loadError,
    reload,
    setData: setProfile,
  } = useAsync(load, [token], { enabled: Boolean(token) })

  // El formulario se inicializa desde el perfil la primera vez que llega.
  const [formFor, setFormFor] = useState(null)
  if (profile && formFor !== profile.id && !editing) {
    setFormFor(profile.id)
    setForm(formFromProfile(profile))
  }

  const onChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const startEdit = () => {
    setMsg('')
    setError('')
    setForm(formFromProfile(profile))
    setEditing(true)
  }

  const cancelEdit = () => {
    setForm(formFromProfile(profile))
    setEditing(false)
    setError('')
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    setMsg('')
    try {
      const nombre = String(form.nombre || '').trim()
      if (nombre.length < 2) {
        throw new Error('El nombre debe tener al menos 2 caracteres')
      }
      const body = {
        nombre,
        apellido: String(form.apellido || '').trim(),
        ciudad: String(form.ciudad || '').trim(),
      }
      const updated = await patchMe(body, { token })
      setProfile(updated)
      setForm(formFromProfile(updated))
      updateUser(updated)
      setEditing(false)
      setMsg('Datos actualizados')
    } catch (err) {
      setError(err.message || 'No se pudieron guardar los cambios')
    } finally {
      setSaving(false)
    }
  }

  const rolLabel =
    ROLE_LABELS[normalizeRol(profile?.rol)] || profile?.rol || '—'
  const displayName = [profile?.nombre, profile?.apellido]
    .filter(Boolean)
    .join(' ')
    .trim()

  const iniciales =
    [profile?.nombre, profile?.apellido]
      .filter(Boolean)
      .map((p) => p[0].toUpperCase())
      .join('')
      .slice(0, 2) || (profile?.email || '?')[0].toUpperCase()

  // Cabecera de cuenta: identidad · datos · acción, repartidos a lo ancho
  const identidad = profile && !editing && (
    <article className="perfil-card perfil-hero">
      <div className="perfil-hero__id">
        <div className="perfil-identity__avatar" aria-hidden>
          {iniciales}
        </div>
        <div className="perfil-hero__who">
          <h2 className="perfil-hero__name">
            {displayName || profile.nombre || 'Mi cuenta'}
          </h2>
          <p className="perfil-hero__email">{profile.email || '—'}</p>
          <div className="perfil-identity__badges">
            <Badge tone="violet">{rolLabel}</Badge>
            <Badge tone={profile.activo === false ? 'danger' : 'success'}>
              {profile.activo === false ? 'Desactivada' : 'Activa'}
            </Badge>
          </div>
        </div>
      </div>

      <dl className="perfil-hero__facts">
        <div>
          <dt>Nombre</dt>
          <dd>{profile.nombre || '—'}</dd>
        </div>
        <div>
          <dt>Apellido</dt>
          <dd>{profile.apellido || '—'}</dd>
        </div>
        <div>
          <dt>Ciudad</dt>
          <dd>{profile.ciudad || '—'}</dd>
        </div>
        <div>
          <dt>Rol</dt>
          <dd>{rolLabel}</dd>
        </div>
      </dl>

      <div className="perfil-hero__actions">
        <button type="button" className="btn btn-primary" onClick={startEdit}>
          Editar datos
        </button>
        <p className="perfil-page__muted">Email y rol no se cambian desde acá.</p>
      </div>
    </article>
  )

  const edicion = profile && editing && (
    <form className="perfil-card perfil-edit" onSubmit={onSubmit} noValidate>
      <h2 className="perfil-card__heading">Editar datos</h2>
      <div className="perfil-edit__grid">
        <div className="form-group">
          <label htmlFor="perfil-nombre">Nombre</label>
          <input
            id="perfil-nombre"
            className="form-input"
            name="nombre"
            value={form.nombre}
            onChange={onChange}
            required
            minLength={2}
            maxLength={100}
            autoComplete="given-name"
          />
        </div>
        <div className="form-group">
          <label htmlFor="perfil-apellido">Apellido</label>
          <input
            id="perfil-apellido"
            className="form-input"
            name="apellido"
            value={form.apellido}
            onChange={onChange}
            maxLength={100}
            autoComplete="family-name"
          />
        </div>
        <div className="form-group">
          <label htmlFor="perfil-ciudad">Ciudad</label>
          <input
            id="perfil-ciudad"
            className="form-input"
            name="ciudad"
            value={form.ciudad}
            onChange={onChange}
            maxLength={120}
            autoComplete="address-level2"
          />
        </div>
        <div className="form-group">
          <label htmlFor="perfil-email">Email</label>
          <input
            id="perfil-email"
            className="form-input"
            value={profile.email || ''}
            disabled
            readOnly
          />
        </div>
        <div className="form-group">
          <label htmlFor="perfil-rol">Rol</label>
          <input
            id="perfil-rol"
            className="form-input"
            value={rolLabel}
            disabled
            readOnly
          />
        </div>
      </div>

      <div className="perfil-card__actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Guardando…' : 'Guardar'}
        </button>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={cancelEdit}
          disabled={saving}
        >
          Cancelar
        </button>
      </div>
    </form>
  )

  const orgCard = isCliente && profile && !editing && (
    <aside className="perfil-card perfil-org" aria-label="Cuenta organizador">
      <p>
        <strong>¿Querés organizar eventos?</strong> Un administrador puede asignarte el rol de
        organizador sobre tu cuenta actual.
      </p>
      <div className="perfil-org__actions">
        <Link className="btn btn-secondary btn-sm" to="/cuenta/organizador">
          Cómo ser organizador
        </Link>
      </div>
    </aside>
  )

  const conPrefs = FEATURE.preferencias

  return (
    <section className="perfil-page container section-v-padding">
      <header className="perfil-page__head">
        <h1 className="perfil-page__title">Mi perfil</h1>
        <p className="perfil-page__sub">
          Tu cuenta, tus gustos y lo que te recomendamos.
        </p>
      </header>

      {status === 'loading' && <LoadingState label="Cargando perfil…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {error && <p className="perfil-page__error">{error}</p>}
      {msg && <p className="perfil-page__ok">{msg}</p>}

      {status === 'success' && profile && (
        <div className="perfil-cols">
          <aside className="perfil-cols__side">
            {identidad}
            {edicion}
            {orgCard}
          </aside>

          <div className="perfil-cols__main">
            {conPrefs && (
              <section className="perfil-card perfil-panel">
                {isCliente && (
                  <div className="perfil-tabs" role="tablist" aria-label="Preferencias e historial">
                    {[
                      ['prefs', 'Mis gustos'],
                      ['historial', 'Mis compras'],
                    ].map(([id, label]) => (
                      <button
                        key={id}
                        type="button"
                        role="tab"
                        aria-selected={tab === id}
                        className={`perfil-tabs__tab${tab === id ? ' is-active' : ''}`}
                        onClick={() => setTab(id)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                )}
                {(!isCliente || tab === 'prefs') && <PreferenciasForm token={token} />}
                {isCliente && tab === 'historial' && (
                  <HistorialCliente idCliente={user?.id} token={token} />
                )}
              </section>
            )}
          </div>
        </div>
      )}
    </section>
  )
}

export default PerfilPage
