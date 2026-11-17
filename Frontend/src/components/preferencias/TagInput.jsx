import { useId, useState } from 'react'

/**
 * Input de etiquetas (ciudades, etc.):
 * - Enter o coma agrega el texto.
 * - Chips con botón para quitar.
 * - Sugerencias opcionales con <datalist>.
 */
const TagInput = ({
  id,
  label,
  values = [],
  onChange,
  placeholder = 'Escribí y presioná Enter',
  suggestions = [],
  max = 10,
  help,
  disabled,
}) => {
  const autoId = useId()
  const inputId = id || autoId
  const listId = `${inputId}-suggestions`
  const [draft, setDraft] = useState('')

  const add = (raw) => {
    const parts = String(raw || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
    if (!parts.length) return
    const next = [...values]
    for (const part of parts) {
      if (next.length >= max) break
      if (!next.some((v) => v.toLowerCase() === part.toLowerCase())) {
        next.push(part)
      }
    }
    onChange(next)
    setDraft('')
  }

  const remove = (value) => {
    onChange(values.filter((v) => v !== value))
  }

  const onKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      add(draft)
    }
  }

  return (
    <div className="pref-field">
      {label && <label htmlFor={inputId}>{label}</label>}
      <div className="pref-chips" aria-live="polite">
        {values.map((value) => (
          <span key={value} className="pref-chip">
            {value}
            <button
              type="button"
              className="pref-chip__remove"
              onClick={() => remove(value)}
              aria-label={`Quitar ${value}`}
              disabled={disabled}
            >
              ×
            </button>
          </span>
        ))}
      </div>
      <div className="pref-tag-input">
        <input
          id={inputId}
          className="form-input"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={placeholder}
          list={suggestions.length ? listId : undefined}
          disabled={disabled || values.length >= max}
          autoComplete="off"
        />
        {suggestions.length > 0 && (
          <datalist id={listId}>
            {suggestions.map((s) => (
              <option key={s} value={s} />
            ))}
          </datalist>
        )}
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => add(draft)}
          disabled={disabled || !draft.trim() || values.length >= max}
        >
          Agregar
        </button>
      </div>
      {help && <p className="pref-help">{help}</p>}
    </div>
  )
}

export default TagInput
