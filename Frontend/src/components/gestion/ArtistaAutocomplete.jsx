import { useCallback, useEffect, useId, useRef, useState } from 'react'
import {
  createParticipante,
  listParticipantes,
} from '../../api/eventos'

/**
 * Autocomplete de artista del catálogo.
 * El valor estable es { id, nombre }. Escribir filtra; elegir fija id.
 * “Crear «…»” hace POST y selecciona el nuevo id.
 */
export default function ArtistaAutocomplete({
  id,
  label,
  value,
  onChange,
  token,
  disabled,
  required,
  excludeIds = [],
  placeholder = 'Buscar artista…',
}) {
  const autoId = useId()
  const inputId = id || autoId
  const wrapRef = useRef(null)
  const [query, setQuery] = useState(value?.nombre || '')
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [creating, setCreating] = useState(false)
  const [options, setOptions] = useState([])
  const [error, setError] = useState('')
  const reqSeq = useRef(0)

  // Sync display if parent sets a selected artist
  useEffect(() => {
    if (value?.id) {
      setQuery(value.nombre || '')
    } else if (!value?.id && !value?.nombre) {
      setQuery('')
    }
  }, [value?.id, value?.nombre])

  const search = useCallback(
    async (q) => {
      const seq = ++reqSeq.current
      const term = String(q || '').trim()
      setLoading(true)
      setError('')
      try {
        const data = await listParticipantes(term ? { q: term } : {}, { token })
        if (seq !== reqSeq.current) return
        const list = Array.isArray(data) ? data : []
        const ex = new Set((excludeIds || []).filter(Boolean).map(String))
        setOptions(
          list
            .filter((p) => p?.id && !ex.has(String(p.id)))
            .slice(0, 12)
        )
      } catch (err) {
        if (seq !== reqSeq.current) return
        setOptions([])
        setError(err.message || 'No se pudo buscar artistas')
      } finally {
        if (seq === reqSeq.current) setLoading(false)
      }
    },
    [token, excludeIds]
  )

  useEffect(() => {
    if (!open) return undefined
    const t = setTimeout(() => search(query), 220)
    return () => clearTimeout(t)
  }, [query, open, search])

  useEffect(() => {
    const onDoc = (e) => {
      if (!wrapRef.current?.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [])

  const select = (p) => {
    onChange({ id: p.id, nombre: p.nombre || '' })
    setQuery(p.nombre || '')
    setOpen(false)
    setError('')
  }

  const clear = () => {
    onChange({ id: null, nombre: '' })
    setQuery('')
    setOptions([])
    setError('')
  }

  const onType = (e) => {
    const next = e.target.value
    setQuery(next)
    // typing invalidates previous selection until pick/create
    if (value?.id) {
      onChange({ id: null, nombre: next })
    } else {
      onChange({ id: null, nombre: next })
    }
    setOpen(true)
  }

  const canCreate = (() => {
    const n = String(query || '').trim()
    if (n.length < 2) return false
    if (value?.id) return false
    const lower = n.toLowerCase()
    return !options.some((o) => String(o.nombre || '').trim().toLowerCase() === lower)
  })()

  const onCreate = async () => {
    const n = String(query || '').trim()
    if (n.length < 2) return
    setCreating(true)
    setError('')
    try {
      const created = await createParticipante(
        { nombre: n, tipo: 'BANDA' },
        { token }
      )
      select(created)
    } catch (err) {
      if (err.status === 409) {
        // PAR-01: ya existe (sin importar mayúsculas): se elige del catálogo
        setError('Ya existe en el catálogo: elegilo de la lista.')
        search(n)
      } else {
        setError(err.message || 'No se pudo crear el artista')
      }
    } finally {
      setCreating(false)
    }
  }

  const selected = Boolean(value?.id)

  return (
    <div className="artista-ac" ref={wrapRef}>
      {label && (
        <label htmlFor={inputId}>
          {label}
          {required ? ' *' : ''}
        </label>
      )}
      <div className={`artista-ac__control${selected ? ' artista-ac__control--selected' : ''}`}>
        <input
          id={inputId}
          className="form-input"
          role="combobox"
          aria-expanded={open}
          aria-autocomplete="list"
          aria-controls={`${inputId}-list`}
          autoComplete="off"
          placeholder={placeholder}
          value={query}
          onChange={onType}
          onFocus={() => {
            setOpen(true)
            search(query)
          }}
          disabled={disabled || creating}
          required={required && !selected}
        />
        {selected && (
          <button
            type="button"
            className="btn btn--ghost artista-ac__clear"
            onClick={clear}
            disabled={disabled || creating}
            aria-label="Quitar artista"
          >
            ×
          </button>
        )}
      </div>

      {selected && (
        <p className="gestion-card__muted artista-ac__hint">
          Seleccionado del catálogo (id guardado al asociar).
        </p>
      )}
      {!selected && query.trim() && (
        <p className="gestion-card__muted artista-ac__hint artista-ac__hint--warn">
          Elegí un resultado o creá el artista para guardar el id.
        </p>
      )}

      {open && !disabled && (
        <ul
          id={`${inputId}-list`}
          className="artista-ac__list"
          role="listbox"
        >
          {loading && (
            <li className="artista-ac__item artista-ac__item--muted">Buscando…</li>
          )}
          {!loading &&
            options.map((p) => (
              <li key={p.id} role="option">
                <button
                  type="button"
                  className="artista-ac__option"
                  onClick={() => select(p)}
                >
                  <span className="artista-ac__name">{p.nombre}</span>
                  {p.tipo && (
                    <span className="artista-ac__meta">{p.tipo}</span>
                  )}
                </button>
              </li>
            ))}
          {!loading && options.length === 0 && query.trim() && (
            <li className="artista-ac__item artista-ac__item--muted">
              Sin coincidencias en el catálogo
            </li>
          )}
          {canCreate && (
            <li role="option">
              <button
                type="button"
                className="artista-ac__option artista-ac__option--create"
                onClick={onCreate}
                disabled={creating}
              >
                {creating ? 'Creando…' : `Crear «${String(query).trim()}»`}
              </button>
            </li>
          )}
        </ul>
      )}

      {error && <p className="gestion-error artista-ac__error">{error}</p>}
    </div>
  )
}
