import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { listLugares } from '../../api/lugares'
import { describirSector } from '../../domain/lugar'
import { urlMapaLugar } from '../../domain/evento'

/**
 * Lugar y sectores de un evento (LUG-04).
 * Los lugares y sus sectores los carga el ADMIN: el organizador solo elige un lugar y, de sus
 * sectores, cuáles se venden en el evento, con su precio y su cupo.
 *
 * Controlado por la página:
 * - `idLugar` / `onElegirLugar(lugar | null)`: lugar elegido (al cambiar, la página rearma las filas).
 * - `rows` / `onRowChange(idSector, campo, valor)`: sectores del lugar (ver `rowsDeLugar`).
 */
const EventoLugarSectores = ({
  token,
  idLugar,
  onElegirLugar,
  rows,
  onRowChange,
  publishing = false,
  disabled = false,
  isAdmin = false,
}) => {
  const [lugares, setLugares] = useState([])
  const [estado, setEstado] = useState('loading')
  const [error, setError] = useState('')
  const [filtro, setFiltro] = useState('')

  useEffect(() => {
    let vivo = true
    listLugares({}, { token })
      .then((data) => {
        if (!vivo) return
        setLugares(Array.isArray(data) ? data : [])
        setEstado('ok')
      })
      .catch((err) => {
        if (!vivo) return
        setError(err.message || 'No se pudieron cargar los lugares')
        setEstado('error')
      })
    return () => {
      vivo = false
    }
  }, [token])

  const elegido = useMemo(
    () => lugares.find((l) => l.id === idLugar) || null,
    [lugares, idLugar]
  )

  const visibles = useMemo(() => {
    const q = filtro.trim().toLowerCase()
    const lista = q
      ? lugares.filter((l) =>
          `${l.nombre} ${l.ciudad} ${l.direccion || ''}`.toLowerCase().includes(q)
        )
      : lugares
    // el lugar ya elegido siempre se ve, aunque el filtro no lo incluya
    return elegido && !lista.includes(elegido) ? [elegido, ...lista] : lista
  }, [lugares, filtro, elegido])

  const mapa = elegido ? urlMapaLugar(elegido) : null

  return (
    <>
      <fieldset className="gestion-fieldset">
        <legend>Lugar {publishing ? '*' : '(se elige para publicar)'}</legend>

        {estado === 'loading' && <p className="gestion-card__muted">Cargando lugares…</p>}
        {estado === 'error' && (
          <p className="gestion-error" role="alert">
            {error}
          </p>
        )}

        {estado === 'ok' && (
          <>
            <div className="gestion-form__row">
              <div className="form-group">
                <label htmlFor="filtro-lugar">Buscar por nombre o ciudad</label>
                <input
                  id="filtro-lugar"
                  className="form-input"
                  value={filtro}
                  onChange={(e) => setFiltro(e.target.value)}
                  placeholder="ej. Buenos Aires"
                  disabled={disabled}
                />
              </div>
              <div className="form-group">
                <label htmlFor="idLugar">Lugar</label>
                <select
                  id="idLugar"
                  className="form-input"
                  value={idLugar || ''}
                  onChange={(e) =>
                    onElegirLugar(lugares.find((l) => l.id === e.target.value) || null)
                  }
                  disabled={disabled}
                >
                  <option value="">Elegí un lugar…</option>
                  {visibles.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.nombre} — {l.ciudad}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {elegido && (
              <p className="gestion-card__muted">
                {[elegido.direccion, elegido.ciudad].filter(Boolean).join(' · ')}
                {mapa && (
                  <>
                    {' · '}
                    <a href={mapa} target="_blank" rel="noopener noreferrer">
                      Ver en el mapa
                    </a>
                  </>
                )}
              </p>
            )}

            <p className="gestion-card__muted">
              Los lugares y sus sectores los carga el administrador.
              {isAdmin ? (
                <>
                  {' '}
                  <Link to="/gestion/lugares/nuevo">Crear un lugar</Link>
                </>
              ) : (
                ' Si no encontrás el tuyo, pedile que lo agregue.'
              )}
            </p>
          </>
        )}
      </fieldset>

      <fieldset className="gestion-fieldset">
        <legend>Sectores del lugar {publishing ? '*' : '(opcionales en borrador)'}</legend>
        {!idLugar && (
          <p className="gestion-card__muted">Elegí un lugar para ver sus sectores.</p>
        )}
        {idLugar && rows.length === 0 && estado === 'ok' && (
          <p className="gestion-card__muted">
            Este lugar todavía no tiene sectores: el administrador tiene que cargarlos.
          </p>
        )}
        {rows.length > 0 && (
          <>
            <p className="gestion-card__muted" style={{ marginBottom: '0.75rem' }}>
              Marcá los sectores que se venden en este evento y poné su precio. El cupo es opcional:
              vacío vende todo el sector. Los sectores numerados se venden completos y el cliente
              elige su asiento. Nombre y layout los define el lugar.
            </p>
            <ul className="gestion-sectores-list">
              {rows.map((r) => (
                <li key={r.id} className="gestion-sector-row">
                  <div className="gestion-sector-row__head">
                    <label className="gestion-check">
                      <input
                        type="checkbox"
                        checked={r.incluido}
                        onChange={(e) => onRowChange(r.id, 'incluido', e.target.checked)}
                        disabled={disabled}
                      />
                      <strong>{r.nombre}</strong>{' '}
                      <span className="gestion-card__muted">
                        {r.tipo ? `${r.tipo} · ` : ''}
                        {describirSector(r)}
                      </span>
                    </label>
                  </div>
                  {r.incluido && (
                    <div className="gestion-form__row">
                      <div className="form-group">
                        <label htmlFor={`sec-${r.id}-precio`}>Precio</label>
                        <input
                          id={`sec-${r.id}-precio`}
                          type="number"
                          min="0"
                          className="form-input"
                          value={r.precio}
                          onChange={(e) => onRowChange(r.id, 'precio', e.target.value)}
                          disabled={disabled}
                          placeholder="0"
                        />
                      </div>
                      <div className="form-group">
                        <label htmlFor={`sec-${r.id}-cupo`}>
                          {r.esNumerado ? 'Cupo' : `Cupo (máx. ${r.capacidad})`}
                        </label>
                        {r.esNumerado ? (
                          <input
                            id={`sec-${r.id}-cupo`}
                            className="form-input"
                            value={`${r.capacidad} (se vende completo)`}
                            readOnly
                            disabled
                          />
                        ) : (
                          <input
                            id={`sec-${r.id}-cupo`}
                            type="number"
                            min="1"
                            max={r.capacidad}
                            className="form-input"
                            value={r.cupo}
                            onChange={(e) => onRowChange(r.id, 'cupo', e.target.value)}
                            disabled={disabled}
                            placeholder={`Todo (${r.capacidad})`}
                          />
                        )}
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          </>
        )}
      </fieldset>
    </>
  )
}

export default EventoLugarSectores
