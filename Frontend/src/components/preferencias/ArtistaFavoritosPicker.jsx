import { useId, useState } from 'react'
import ArtistaAutocomplete from '../gestion/ArtistaAutocomplete'

/**
 * Lista de artistas favoritos. Reutiliza el autocomplete del catálogo para
 * sugerir nombres, pero permite agregar texto libre (no crea participantes).
 */
const ArtistaFavoritosPicker = ({
  id,
  label = 'Artistas favoritos',
  artistas = [],
  onChange,
  token,
  max = 12,
  disabled,
}) => {
  const autoId = useId()
  const inputId = id || autoId
  const [pending, setPending] = useState({ id: null, nombre: '' })

  const add = () => {
    const nombre = String(pending.nombre || '').trim()
    if (!nombre || artistas.length >= max) return
    if (artistas.some((a) => a.toLowerCase() === nombre.toLowerCase())) {
      setPending({ id: null, nombre: '' })
      return
    }
    onChange([...artistas, nombre])
    setPending({ id: null, nombre: '' })
  }

  const remove = (nombre) => {
    onChange(artistas.filter((a) => a !== nombre))
  }

  return (
    <div className="pref-field">
      <label htmlFor={inputId}>{label}</label>
      <div className="pref-chips" aria-live="polite">
        {artistas.map((nombre) => (
          <span key={nombre} className="pref-chip">
            {nombre}
            <button
              type="button"
              className="pref-chip__remove"
              onClick={() => remove(nombre)}
              aria-label={`Quitar ${nombre}`}
              disabled={disabled}
            >
              ×
            </button>
          </span>
        ))}
      </div>
      <div className="pref-artista-row">
        <div className="pref-artista-row__input">
          <ArtistaAutocomplete
            id={inputId}
            value={pending}
            onChange={setPending}
            token={token}
            label={null}
            allowCreate={false}
            placeholder="Buscar artista o escribir un nombre…"
            disabled={disabled || artistas.length >= max}
          />
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={add}
          disabled={
            disabled ||
            !String(pending.nombre || '').trim() ||
            artistas.length >= max
          }
        >
          Agregar
        </button>
      </div>
      <p className="pref-help">
        Se guardan los nombres; el motor de recomendaciones los cruza con los
        artistas de cada evento.
      </p>
    </div>
  )
}

export default ArtistaFavoritosPicker
