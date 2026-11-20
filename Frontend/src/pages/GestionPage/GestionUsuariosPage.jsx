import { useState } from 'react'
import useAuth from '../../hooks/useAuth'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import { ROLE_LABELS, ROLES, normalizeRol } from '../../auth/roles'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { PageHeader, Toolbar, VacioTabla } from '../../components/gestion/gestionUi'
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

  const [q, setQ] = useState('')
  const [fRol, setFRol] = useState('')
  const filtrados = (items || []).filter((u) => {
    if (fRol && normalizeRol(u.rol) !== fRol) return false
    const txt = `${u.nombre || ''} ${u.email || ''}`.toLowerCase()
    return !q || txt.includes(q.trim().toLowerCase())
  })

  return (
    <>
      <PageHeader
        title="Usuarios"
        subtitle="Cambiá el rol o el estado de una cuenta. El personal de acceso se crea acá, se asigna a eventos y se elimina (no se desactiva)."
      >
        <GateButton
          enabled={FEATURE.adminUsuarios}
          type="button"
          className="btn btn-primary"
          onClick={() => setShowCreate((v) => !v)}
        >
          {showCreate ? 'Cerrar alta' : 'Nuevo personal de acceso'}
        </GateButton>
      </PageHeader>

      {!FEATURE.adminUsuariosListado && (
        <VacioTabla>
          La gestión de usuarios no está habilitada en esta build. {GATE_HINT}
        </VacioTabla>
      )}

      {error && <p className="g-error">{error}</p>}
      {status === 'loading' && <LoadingState label="Cargando usuarios…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

      {showCreate && (
        <div style={{ marginBottom: '1.25rem' }}>
          <AltaAccesoForm
            form={form}
            setForm={setForm}
            eventoOptions={eventoOptions}
            busyId={busyId}
            onSubmit={onCreate}
            onToggleEvento={toggleFormEvento}
          />
        </div>
      )}

      {status === 'success' && (
        <>
          <Toolbar
            buscar={q}
            onBuscar={setQ}
            placeholder="Buscar por nombre o email…"
            derecha={
              <button type="button" className="btn btn-secondary btn-sm" onClick={reload}>
                Actualizar
              </button>
            }
          >
            <select className="form-input" value={fRol} onChange={(e) => setFRol(e.target.value)} aria-label="Filtrar por rol">
              <option value="">Todos los roles</option>
              {Object.values(ROLES).map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABELS[r] || r}
                </option>
              ))}
            </select>
          </Toolbar>

          {filtrados.length === 0 ? (
            <VacioTabla>Ninguna cuenta coincide con el filtro.</VacioTabla>
          ) : (
            <div className="g-table-wrap">
              <table className="g-table">
                <thead>
                  <tr>
                    <th>Usuario</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th className="is-actions">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((u) => (
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
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  )
}

export default GestionUsuariosPage
