import { useMemo, useState } from 'react'
import { getPreferencias, putPreferencias } from '../../api/preferencias'
import { listEventos } from '../../api/eventos'
import { EVENTO_TIPOS, EVENTO_TIPO_LABELS } from '../../domain/evento'
import useAsync from '../../hooks/useAsync'
import { ErrorState, LoadingState } from '../ui/AsyncState'
import TagInput from './TagInput'
import ArtistaFavoritosPicker from './ArtistaFavoritosPicker'

function uniqueValues(values) {
  const seen = new Set()
  const out = []
  for (const raw of values || []) {
    const value = String(raw || '').trim()
    if (!value) continue
    const key = value.toLowerCase()
    if (seen.has(key)) continue
    seen.add(key)
    out.push(value)
  }
  return out
}

function toForm(prefs) {
  return {
    categorias: (prefs?.categorias || []).map((c) =>
      String(c || '').trim().toUpperCase()
    ),
    ciudades: Array.isArray(prefs?.ciudades) ? prefs.ciudades : [],
    artistas: Array.isArray(prefs?.artistas) ? prefs.artistas : [],
    rangoPrecio: {
      min: prefs?.rangoPrecio?.min != null ? String(prefs.rangoPrecio.min) : '',
      max: prefs?.rangoPrecio?.max != null ? String(prefs.rangoPrecio.max) : '',
    },
  }
}

/** Formulario “Mis preferencias” — GET/PUT /api/preferencias/me. */
const PreferenciasForm = ({ token }) => {
  const [form, setForm] = useState(toForm(null))
  const [syncedToken, setSyncedToken] = useState(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const prefsQuery = useAsync(
    (signal) => getPreferencias({ token, signal }),
    [token],
    { enabled: Boolean(token) }
  )

  const citiesQuery = useAsync(
    (signal) => listEventos({ size: 200 }, { signal }),
    [],
    { enabled: Boolean(token) }
  )

  // Inicializa el formulario cuando llegan las preferencias y no pisa
  // cambios del usuario en revalidaciones.
  if (prefsQuery.isSuccess && syncedToken !== token) {
    setSyncedToken(token)
    setForm(toForm(prefsQuery.data))
  }

  const ciudadesSugeridas = useMemo(() => {
    const list = citiesQuery.data || []
    return [
      ...new Set(
        list
          .map((e) => e?.lugar?.ciudad)
          .filter(Boolean)
          .map((c) => String(c).trim())
      ),
    ].sort((a, b) => a.localeCompare(b, 'es'))
  }, [citiesQuery.data])

  const toggleCategoria = (tipo) => {
    setForm((prev) => ({
      ...prev,
      categorias: prev.categorias.includes(tipo)
        ? prev.categorias.filter((c) => c !== tipo)
        : [...prev.categorias, tipo],
    }))
  }

  const setRango = (campo, value) => {
    setForm((prev) => ({
      ...prev,
      rangoPrecio: { ...prev.rangoPrecio, [campo]: value },
    }))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    setMsg('')
    try {
      const minRaw = String(form.rangoPrecio.min || '').trim()
      const maxRaw = String(form.rangoPrecio.max || '').trim()
      const min = minRaw ? Number(minRaw) : null
      const max = maxRaw ? Number(maxRaw) : null
      if (min != null && (!Number.isFinite(min) || min < 0)) {
        throw new Error('El precio mínimo debe ser un número mayor o igual a 0')
      }
      if (max != null && (!Number.isFinite(max) || max < 0)) {
        throw new Error('El precio máximo debe ser un número mayor o igual a 0')
      }
      if (min != null && max != null && min > max) {
        throw new Error('El precio mínimo no puede ser mayor que el máximo')
      }

      const body = {
        categorias: uniqueValues(
          form.categorias.map((c) => String(c).toUpperCase())
        ),
        ciudades: uniqueValues(form.ciudades),
        artistas: uniqueValues(form.artistas),
        // Se envía siempre el objeto: min/max null limpian el rango en el BE.
        rangoPrecio: { min, max, moneda: 'ARS' },
      }
      const saved = await putPreferencias(body, { token })
      setForm(toForm(saved))
      setMsg('Preferencias guardadas. Las recomendaciones se actualizan al instante.')
    } catch (err) {
      setError(err.message || 'No se pudieron guardar las preferencias')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="pref-embed">

      {!token && (
        <p className="perfil-page__error">
          Iniciá sesión para guardar tus preferencias.
        </p>
      )}
      {token && prefsQuery.status === 'loading' && (
        <LoadingState label="Cargando preferencias…" />
      )}
      {token && prefsQuery.status === 'error' && (
        <ErrorState error={prefsQuery.error} onRetry={prefsQuery.reload} />
      )}
      {error && <p className="perfil-page__error">{error}</p>}
      {msg && <p className="perfil-page__ok">{msg}</p>}

      {prefsQuery.status === 'success' && (
        <form className="pref-form" onSubmit={onSubmit} noValidate>
          <fieldset className="pref-fieldset">
            <legend>Tipos de evento</legend>
            <div className="pref-types">
              {Object.values(EVENTO_TIPOS).map((tipo) => (
                <label key={tipo} className="pref-type">
                  <input
                    type="checkbox"
                    checked={form.categorias.includes(tipo)}
                    onChange={() => toggleCategoria(tipo)}
                    disabled={saving}
                  />
                  <span>{EVENTO_TIPO_LABELS[tipo] || tipo}</span>
                </label>
              ))}
            </div>
          </fieldset>

          <TagInput
            id="pref-ciudades"
            label="Ciudades"
            values={form.ciudades}
            onChange={(ciudades) =>
              setForm((prev) => ({ ...prev, ciudades }))
            }
            suggestions={ciudadesSugeridas}
            placeholder="Ej. Buenos Aires"
            disabled={saving}
          />

          <ArtistaFavoritosPicker
            id="pref-artistas"
            artistas={form.artistas}
            onChange={(artistas) =>
              setForm((prev) => ({ ...prev, artistas }))
            }
            token={token}
            disabled={saving}
          />

          <fieldset className="pref-fieldset">
            <legend>Rango de precio (ARS) · opcional</legend>
            <div className="pref-range">
              <div className="form-group">
                <label htmlFor="pref-precio-min">Mínimo</label>
                <input
                  id="pref-precio-min"
                  className="form-input"
                  type="number"
                  min="0"
                  step="1000"
                  inputMode="numeric"
                  placeholder="20000"
                  value={form.rangoPrecio.min}
                  onChange={(e) => setRango('min', e.target.value)}
                  disabled={saving}
                />
              </div>
              <div className="form-group">
                <label htmlFor="pref-precio-max">Máximo</label>
                <input
                  id="pref-precio-max"
                  className="form-input"
                  type="number"
                  min="0"
                  step="1000"
                  inputMode="numeric"
                  placeholder="300000"
                  value={form.rangoPrecio.max}
                  onChange={(e) => setRango('max', e.target.value)}
                  disabled={saving}
                />
              </div>
            </div>
          </fieldset>

          <div className="pref-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}

export default PreferenciasForm
