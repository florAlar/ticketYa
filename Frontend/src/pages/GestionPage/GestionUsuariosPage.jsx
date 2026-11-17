import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import useUsuariosAdmin from './usuarios/useUsuariosAdmin'
import AltaAccesoForm from './usuarios/AltaAccesoForm'
import UsuarioRow from './usuarios/UsuarioRow'
import '../GestionPage/GestionPage.css'

const GestionUsuariosPage = () => {
  const { user, token } = useAuth()
  const {
    items,
    status,
    error,
    loadError,
    reload,
    busyId,
    showCreate,
    setShowCreate,
    form,
    setForm,
    eventoOptions,
    onRol,
    onActivo,
    onDelete,
    onToggleEvento,
    onCreate,
    toggleFormEvento,
  } = useUsuariosAdmin({ user, token })

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
          {!FEATURE.adminUsuarios && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <div className="gestion-card__actions gestion-card__actions--spaced">
        <GateButton
          enabled={FEATURE.adminUsuarios}
          type="button"
          className="btn btn-primary"
          onClick={() => setShowCreate((v) => !v)}
        >
          {showCreate ? 'Cerrar alta' : 'Nuevo personal de acceso'}
        </GateButton>
        <GateButton
          enabled={FEATURE.adminUsuariosListado}
          type="button"
          className="btn"
          onClick={reload}
        >
          Actualizar
        </GateButton>
      </div>

      {!FEATURE.adminUsuariosListado && (
        <div className="gestion-card">
          <p>
            Gestión de usuarios no habilitada en el front: el alta de personal
            de acceso y asignación a eventos aún no están en el contrato BE
            completo.
          </p>
          <p className="gestion-card__muted">{GATE_HINT}</p>
          <Link className="btn" to="/gestion">
            ← Panel
          </Link>
        </div>
      )}

      {error && <p className="gestion-error">{error}</p>}
      {status === 'loading' && <LoadingState label="Cargando usuarios…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

      {showCreate && (
        <AltaAccesoForm
          form={form}
          setForm={setForm}
          eventoOptions={eventoOptions}
          busyId={busyId}
          onSubmit={onCreate}
          onToggleEvento={toggleFormEvento}
        />
      )}

      {status === 'success' && (
        <ul className="gestion-event-list">
          {items.map((u) => (
            <UsuarioRow
              key={u.id}
              usuario={u}
              eventoOptions={eventoOptions}
              busyId={busyId}
              currentUserId={user?.id}
              onRol={onRol}
              onActivo={onActivo}
              onToggleEvento={onToggleEvento}
              onDelete={onDelete}
            />
          ))}
        </ul>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion">← Panel</Link>
      </p>
    </section>
  )
}

export default GestionUsuariosPage
