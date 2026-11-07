import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getMe, patchMe } from '../../api/auth'
import { ROLE_LABELS, normalizeRol } from '../../auth/roles'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
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
 * Panel: solo «Editar datos» (resto de flujos en el nav).
 * CLIENTE: 2 columnas — datos | CTA organizador.
 */
const PerfilPage = () => {
  const { token, user, updateUser, isCliente } = useAuth()

  const [profile, setProfile] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const me = await getMe({ token })
      const next = me || user
      if (!next) {
        throw new Error('No se pudo cargar el perfil')
      }
      setProfile(next)
      setForm(formFromProfile(next))
      if (me) updateUser(me)
      setStatus('ok')
    } catch (err) {
      if (user) {
        setProfile(user)
        setForm(formFromProfile(user))
        setStatus('ok')
        setError('')
        return
      }
      setError(err.message || 'No se pudo cargar el perfil')
      setStatus('error')
    }
  }, [token, user, updateUser])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

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

  const datosCard = profile && !editing && (
    <article className="perfil-card perfil-card--datos">
      <h2 className="perfil-card__heading">Datos personales</h2>
      <dl className="perfil-dl">
        <div>
          <dt>Nombre</dt>
          <dd>{displayName || profile.nombre || '—'}</dd>
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
          <dt>Email</dt>
          <dd className="perfil-dl__mono">{profile.email || '—'}</dd>
        </div>
        <div>
          <dt>Rol</dt>
          <dd>
            <span className="perfil-badge">{rolLabel}</span>
          </dd>
        </div>
        <div>
          <dt>Estado</dt>
          <dd>{profile.activo === false ? 'Desactivada' : 'Activa'}</dd>
        </div>
      </dl>

      <div className="perfil-card__actions">
        <button type="button" className="btn btn-primary" onClick={startEdit}>
          Editar datos
        </button>
      </div>
      <p className="perfil-page__muted perfil-page__hint">
        Email y rol no se cambian desde acá. El resto de acciones está en el menú.
      </p>
    </article>
  )

  const orgCard = isCliente && profile && !editing && (
    <aside className="perfil-card perfil-card--org" aria-label="Cuenta organizador">
      <h2 className="perfil-org__title">¿Querés organizar eventos?</h2>
      <p className="perfil-page__muted">
        Los organizadores no se registran solos: un cliente pide la extensión y un
        admin la autoriza. Por ahora el pedido no está cableado al backend.
      </p>
      <div className="perfil-card__actions perfil-card__actions--stack">
        <GateButton
          enabled={FEATURE.solicitudOrganizador}
          className="btn btn-primary"
          title={
            FEATURE.solicitudOrganizador
              ? 'Enviar solicitud de organizador'
              : GATE_HINT
          }
        >
          Solicitar cuenta de organizador
        </GateButton>
        <Link className="btn btn-secondary" to="/cuenta/organizador">
          Ver guía de roles
        </Link>
      </div>
    </aside>
  )

  return (
    <section className="perfil-page container-slim section-v-padding">
      <header className="perfil-page__head">
        <h1 className="perfil-page__title">Mi perfil</h1>
        <p className="perfil-page__sub">
          Datos de la cuenta con la que estás logueado.
        </p>
      </header>

      {status === 'loading' && (
        <p className="perfil-page__muted">Cargando…</p>
      )}
      {error && <p className="perfil-page__error">{error}</p>}
      {msg && <p className="perfil-page__ok">{msg}</p>}

      {status === 'ok' && profile && !editing && (
        <div
          className={
            isCliente
              ? 'perfil-layout perfil-layout--two'
              : 'perfil-layout perfil-layout--one'
          }
        >
          <div className="perfil-layout__main">{datosCard}</div>
          {isCliente && (
            <div className="perfil-layout__side">{orgCard}</div>
          )}
        </div>
      )}

      {status === 'ok' && profile && editing && (
        <form
          className="perfil-card perfil-card--datos perfil-card--edit"
          onSubmit={onSubmit}
          noValidate
        >
          <h2 className="perfil-card__heading">Editar datos</h2>
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

          <div className="perfil-card__actions">
            <button
              type="submit"
              className="btn btn-primary"
              disabled={saving}
            >
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
      )}
    </section>
  )
}

export default PerfilPage
