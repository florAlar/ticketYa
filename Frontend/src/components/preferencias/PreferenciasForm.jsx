import { useEffect, useState } from 'react'
import { getPreferencias, putPreferencias } from '../../api/preferencias'
import { listEventos } from '../../api/eventos'
import { EVENTO_TIPOS, EVENTO_TIPO_LABELS } from '../../domain/evento'
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
  const [status, setStatus] = useState('loading')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const [ciudadesSugeridas, setCiudadesSugeridas] = useState([])

  useEffect(() => {
    if (!token) {
      setStatus('error')
      setError('Iniciá sesión para guardar tus preferencias.')
      return undefined
    }
    let cancelled = false
    setStatus('loading')
    setError('')
    getPreferencias({ token })
      .then((prefs) => {
        if (cancelled) return
        setForm(toForm(prefs))
        setStatus('ok')
      })
      .catch((err) => {
        if (cancelled) return
        setError(err.message || 'No se pudieron cargar tus preferencias')
        setStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [token])

  useEffect(() => {
    let cancelled = false
    listEventos({ size: 200 })
      .then((list) => {
        if (cancelled) return
        const ciudades = [
          ...new Set(
            (list || [])
              .map((e) => e?.lugar?.ciudad)
              .filter(Boolean)
              .map((c) => String(c).trim())
          ),
        ].sort((a, b) => a.localeCompare(b, 'es'))
        setCiudadesSugeridas(ciudades)
      })
      .catch(() => {
        /* las sugerencias son opcionales */
      })
    return () => {
      cancelled = true
    }
  }, [])

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
    <article className="perfil-card pref-card">
      <h2 className="perfil-card__heading">Mis preferencias</h2>
      <p className="perfil-page__muted">
        Contanos qué te gusta. Se combina con tu historial de compras para
        recomendarte eventos.
      </p>

      {status === 'loading' && (
        <p className="perfil-page__muted">Cargando preferencias…</p>
      )}
      {error && <p className="perfil-page__error">{error}</p>}
      {msg && <p className="perfil-page__ok">{msg}</p>}

      {status === 'ok' && (
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
            help="Agregá una o más ciudades de tu interés."
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
            <legend>Rango de precio (ARS)</legend>
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
            <p className="pref-help">
              Dejá ambos vacíos para no limitar las recomendaciones por precio.
            </p>
          </fieldset>

          <div className="perfil-card__actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar preferencias'}
            </button>
          </div>
        </form>
      )}
    </article>
  )
}

export default PreferenciasForm
