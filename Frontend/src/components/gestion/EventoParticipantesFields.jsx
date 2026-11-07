import { useId, useMemo } from 'react'
import ArtistaAutocomplete from './ArtistaAutocomplete'

let teloneroKeySeq = 1

export function newTeloneroRow(partial = {}) {
  return {
    key: `t_${Date.now()}_${teloneroKeySeq++}`,
    id: partial.id || null,
    nombre: partial.nombre || '',
  }
}

/** Mapea respuesta BE/mock → principal + teloneros (con id). */
export function splitParticipantesForm(list) {
  const arr = Array.isArray(list) ? list : []
  const objs = arr
    .map((p) => {
      if (p && typeof p === 'object') {
        return {
          id: p.id || null,
          nombre: String(p.nombre || '').trim(),
          rol: String(p.rol || '').toUpperCase(),
        }
      }
      return { id: null, nombre: String(p || '').trim(), rol: '' }
    })
    .filter((p) => p.nombre || p.id)

  let principal = objs.find((p) => p.rol === 'PRINCIPAL')
  if (!principal && objs.length) principal = objs[0]
  const teloneros = objs
    .filter((p) => !principal || p.id !== principal.id || p.nombre !== principal.nombre)
    .filter((p) => {
      if (!principal) return true
      if (principal.id && p.id) return p.id !== principal.id
      return p !== principal
    })
    .filter((p) => p.rol === 'TELONERO' || p.rol !== 'PRINCIPAL')
    .map((p) => newTeloneroRow({ id: p.id, nombre: p.nombre }))

  return {
    principal: {
      id: principal?.id || null,
      nombre: principal?.nombre || '',
    },
    // legacy aliases
    principalNombre: principal?.nombre || '',
    principalId: principal?.id || null,
    teloneros,
  }
}

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
      <p className="gestion-card__muted" style={{ marginBottom: '0.65rem' }}>
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
