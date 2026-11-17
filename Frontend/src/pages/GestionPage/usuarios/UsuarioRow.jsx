import GateButton from '../../../components/ui/GateButton'
import { ROLE_LABELS, ROLES, normalizeRol } from '../../../auth/roles'
import { FEATURE } from '../../../config/featureGates'

/** Fila de usuario con rol, estado y eventos asignados. */
export default function UsuarioRow({
  usuario: u,
  eventoOptions,
  busyId,
  currentUserId,
  onRol,
  onActivo,
  onToggleEvento,
  onDelete,
}) {
  const isAcceso = normalizeRol(u.rol) === ROLES.ACCESO
  const self = u.id === currentUserId
  const disabled = busyId === u.id || self

  return (
    <li className="gestion-event-row">
      <div className="gestion-event-row__main">
        <h2>
          {u.nombre} <span className="gestion-card__muted">· {u.email}</span>
        </h2>
        <p className="gestion-card__muted">
          id: {u.id}
          {!u.activo && !isAcceso ? ' · DESACTIVADO' : ''}
        </p>
        <div className="gestion-form__row gestion-form__row--compact">
          <div className="form-group">
            <label htmlFor={`rol-${u.id}`}>Rol</label>
            <select
              id={`rol-${u.id}`}
              className="form-input"
              value={normalizeRol(u.rol)}
              disabled={disabled || !FEATURE.adminUsuariosListado}
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
                disabled={disabled || !FEATURE.adminUsuariosListado}
                onChange={(e) => onActivo(u.id, e.target.value === '1')}
              >
                <option value="1">Sí</option>
                <option value="0">No</option>
              </select>
            </div>
          )}
        </div>

        {isAcceso && (
          <div className="gestion-user__asignaciones">
            <p className="gestion-card__muted">Eventos asignados (scan)</p>
            <div className="gestion-checks">
              {eventoOptions.map((ev) => (
                <label key={ev.id} className="gestion-check">
                  <input
                    type="checkbox"
                    checked={(u.eventosAsignados || []).includes(ev.id)}
                    disabled={busyId === u.id || !FEATURE.adminUsuarios}
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
          <GateButton
            enabled={FEATURE.adminUsuarios}
            type="button"
            className="btn"
            disabled={busyId === u.id}
            onClick={() => onDelete(u.id)}
          >
            Eliminar
          </GateButton>
        )}
      </div>
    </li>
  )
}
