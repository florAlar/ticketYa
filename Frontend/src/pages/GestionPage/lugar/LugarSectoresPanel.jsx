import GateButton from '../../../components/ui/GateButton'
import SectorCupoFields from '../../../components/gestion/SectorCupoFields'
import { TIPOS_SECTOR, describirSector } from '../../../domain/lugar'

/** Listado y alta/edición de sectores de un lugar. */
export default function LugarSectoresPanel({
  lugar,
  wired,
  editando,
  sectorForm,
  sectorError,
  sectorSaving,
  onAbrirSector,
  onSectorChange,
  onGuardarSector,
  onCerrarSector,
}) {
  const sectores = lugar?.sectores || []

  return (
    <div className="gestion-card gestion-section">
      <h2>Sectores</h2>
      <p className="gestion-card__muted">
        Capacidad total del lugar: {lugar?.capacidadTotal ?? 0}. Un sector
        numerado define su capacidad con filas × asientos por fila. Los sectores
        no se eliminan; los cambios afectan a los eventos que usan este lugar.
      </p>

      {sectores.length === 0 && editando !== 'nuevo' && (
        <p className="gestion-card__muted">
          Este lugar todavía no tiene sectores.
        </p>
      )}

      <ul className="gestion-sectores-list">
        {sectores.map((s) => (
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
                onClick={() => onAbrirSector(s)}
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
            onClick={() => onAbrirSector(null)}
          >
            + Agregar sector
          </GateButton>
        </div>
      )}

      {editando !== null && (
        <form className="gestion-sector-row" onSubmit={onGuardarSector}>
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
              onClick={onCerrarSector}
            >
              Cancelar
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
