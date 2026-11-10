import {
  MAX_ASIENTOS_POR_FILA,
  MAX_FILAS,
  capacidadDeLayout,
  parseEntero,
} from '../../domain/asiento'

/**
 * Cupo de un tipo de entrada en los formularios de gestión.
 * - Sector general: input «Cupo».
 * - Sector numerado: «Filas» y «Asientos por fila»; el cupo se deriva (solo lectura) y nunca se
 *   toma de un valor aparte. En borrador el layout puede quedar vacío.
 *
 * Devuelve fragmentos de `form-group` para insertar dentro de la fila del sector.
 */
const SectorCupoFields = ({ sector, onChange, disabled, idPrefix, groupStyle }) => {
  if (!sector.esNumerado) {
    return (
      <div className="form-group" style={groupStyle}>
        <label htmlFor={`${idPrefix}-cap`}>Cupo</label>
        <input
          id={`${idPrefix}-cap`}
          type="number"
          min="1"
          className="form-input"
          value={sector.capacidad}
          onChange={(e) => onChange('capacidad', e.target.value)}
          disabled={disabled}
          placeholder="ej. 500"
        />
      </div>
    )
  }

  const derivada = capacidadDeLayout(
    parseEntero(sector.filas),
    parseEntero(sector.asientosPorFila)
  )

  return (
    <>
      <div className="form-group" style={groupStyle}>
        <label htmlFor={`${idPrefix}-filas`}>Filas</label>
        <input
          id={`${idPrefix}-filas`}
          type="number"
          min="1"
          max={MAX_FILAS}
          step="1"
          className="form-input"
          value={sector.filas}
          onChange={(e) => onChange('filas', e.target.value)}
          disabled={disabled}
          placeholder="ej. 10"
        />
      </div>
      <div className="form-group" style={groupStyle}>
        <label htmlFor={`${idPrefix}-porfila`}>Asientos por fila</label>
        <input
          id={`${idPrefix}-porfila`}
          type="number"
          min="1"
          max={MAX_ASIENTOS_POR_FILA}
          step="1"
          className="form-input"
          value={sector.asientosPorFila}
          onChange={(e) => onChange('asientosPorFila', e.target.value)}
          disabled={disabled}
          placeholder="ej. 15"
        />
      </div>
      <div className="form-group" style={groupStyle}>
        <label htmlFor={`${idPrefix}-cupo`}>Cupo (filas × asientos)</label>
        <input
          id={`${idPrefix}-cupo`}
          type="text"
          className="form-input"
          value={derivada != null ? String(derivada) : '—'}
          readOnly
          disabled
          title="Se calcula: filas × asientos por fila"
        />
      </div>
    </>
  )
}

export default SectorCupoFields
