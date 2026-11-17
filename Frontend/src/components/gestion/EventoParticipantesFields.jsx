import { useId, useMemo } from 'react'
import ArtistaAutocomplete from './ArtistaAutocomplete'

/**
 * Principal obligatorio (id de catálogo) + teloneros opcionales de a uno.
 */
export default function EventoParticipantesFields({
  principal,
  onPrincipalChange,
  teloneros,
  onTeloneroChange,
  onAddTelonero,
  onRemoveTelonero,
  token,
  disabled,
}) {
  const baseId = useId()

  const excludeForPrincipal = useMemo(
    () => (teloneros || []).map((t) => t.id).filter(Boolean),
    [teloneros]
  )

  return (
    <fieldset className="gestion-fieldset gestion-participantes">
      <legend>Artistas</legend>
      <p className="gestion-card__muted gestion-hint--spaced">
        Buscá y elegí del catálogo (se guarda el id). Si no existe, crealo con el
        botón del listado. Principal obligatorio; teloneros opcionales (+).
      </p>

      <div className="form-group">
        <ArtistaAutocomplete
          id={`${baseId}-principal`}
          label="Artista / banda principal"
          value={principal || { id: null, nombre: '' }}
          onChange={onPrincipalChange}
          token={token}
          disabled={disabled}
          required
          excludeIds={excludeForPrincipal}
          placeholder="Buscar principal…"
        />
      </div>

      <div className="gestion-participantes__teloneros">
        <div className="gestion-participantes__teloneros-head">
          <span className="gestion-participantes__label">Teloneros (opcional)</span>
          <button
            type="button"
            className="btn btn--ghost gestion-participantes__add"
            onClick={onAddTelonero}
            disabled={disabled}
          >
            + Telonero
          </button>
        </div>

        {teloneros.length === 0 && (
          <p className="gestion-card__muted">Sin teloneros. Usá + para agregar.</p>
        )}

        {teloneros.map((row, idx) => {
          const exclude = [
            principal?.id,
            ...(teloneros || [])
              .filter((t) => t.key !== row.key)
              .map((t) => t.id),
          ].filter(Boolean)
          return (
            <div key={row.key} className="gestion-participantes__row">
              <div className="gestion-participantes__ac">
                <ArtistaAutocomplete
                  id={`${baseId}-t-${row.key}`}
                  label={`Telonero ${idx + 1}`}
                  value={{ id: row.id, nombre: row.nombre }}
                  onChange={(v) => onTeloneroChange(row.key, v)}
                  token={token}
                  disabled={disabled}
                  excludeIds={exclude}
                  placeholder={`Buscar telonero ${idx + 1}…`}
                />
              </div>
              <button
                type="button"
                className="btn btn--ghost"
                onClick={() => onRemoveTelonero(row.key)}
                disabled={disabled}
                aria-label={`Quitar telonero ${idx + 1}`}
              >
                Quitar
              </button>
            </div>
          )
        })}
      </div>
    </fieldset>
  )
}
