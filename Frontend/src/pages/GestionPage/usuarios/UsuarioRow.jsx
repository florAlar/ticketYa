import { useState } from 'react'
import { ROLE_LABELS, ROLES, normalizeRol } from '../../../auth/roles'
import { FEATURE } from '../../../config/featureGates'
import { RowMenu } from '../../../components/gestion/gestionUi'

/** Fila de la tabla de usuarios: rol y estado editables; el personal de acceso suma sus eventos asignados. */
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
  const [abierto, setAbierto] = useState(false)
  const isAcceso = normalizeRol(u.rol) === ROLES.ACCESO
  const self = u.id === currentUserId
  const disabled = busyId === u.id || self
  const asignados = (u.eventosAsignados || []).length

  return (
    <>
      <tr>
        <td>
          <div className="g-cell-main">
            <strong>{u.nombre}</strong>
            <span>
              {u.email}
              {self ? ' · vos' : ''}
            </span>
          </div>
        </td>
        <td>
          <select
            aria-label={`Rol de ${u.nombre}`}
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
        </td>
        <td>
          {isAcceso ? (
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => setAbierto((v) => !v)}>
              {abierto ? 'Ocultar eventos' : `Eventos asignados (${asignados})`}
            </button>
          ) : (
            <select
              aria-label={`Estado de ${u.nombre}`}
              className="form-input"
              value={u.activo ? '1' : '0'}
              disabled={disabled || !FEATURE.adminUsuariosListado}
              onChange={(e) => onActivo(u.id, e.target.value === '1')}
            >
              <option value="1">Activo</option>
              <option value="0">Desactivado</option>
            </select>
          )}
        </td>
        <td className="is-actions">
          {isAcceso && (
            <RowMenu
              label={`Acciones de ${u.nombre}`}
              items={[
                {
                  label: 'Eliminar cuenta',
                  danger: true,
                  disabled: busyId === u.id || !FEATURE.adminUsuarios,
                  onClick: () => onDelete(u.id),
                },
              ]}
            />
          )}
        </td>
      </tr>
      {isAcceso && abierto && (
        <tr className="g-subrow">
          <td colSpan={4}>
            <p className="g-muted" style={{ margin: '0 0 0.5rem' }}>
              Eventos donde puede escanear entradas
            </p>
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
          </td>
        </tr>
      )}
    </>
  )
}
