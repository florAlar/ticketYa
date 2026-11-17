import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { deleteLugar, listLugares } from '../../api/lugares'
import { urlMapaLugar } from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import './GestionPage.css'

/**
 * Lugares (LUG-01) — solo ADMIN. Listado con filtro por ciudad, alta, edición (con sectores) y baja
 * (409 si el lugar tiene eventos: en ese caso solo se edita).
 */
const GestionLugaresPage = () => {
  const { token } = useAuth()
  const wired = FEATURE.lugares
  const [ciudad, setCiudad] = useState('')
  const [filtro, setFiltro] = useState('')
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(
    async (signal) => {
      const data = await listLugares({ ciudad: filtro }, { token, signal })
      return Array.isArray(data) ? data : []
    },
    [filtro, token]
  )

  const {
    data: items = [],
    status,
    error: loadError,
    reload,
  } = useAsync(load, [filtro, token], { enabled: wired })

  const onBuscar = (e) => {
    e.preventDefault()
    setFiltro(ciudad.trim())
  }

  const onEliminar = async (lugar) => {
    if (!window.confirm(`¿Eliminar el lugar «${lugar.nombre}» y sus sectores?`)) return
    setBusyId(lugar.id)
    setError('')
    setAviso('')
    try {
      await deleteLugar(lugar.id, { token })
      setAviso(`Lugar «${lugar.nombre}» eliminado.`)
      await reload()
    } catch (err) {
      setError(
        err.status === 409
          ? `«${lugar.nombre}» tiene eventos asociados: no se puede eliminar, solo editar.`
          : err.message || 'No se pudo eliminar'
      )
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> / lugares
        </p>
        <h1 className="gestion-page__title">Lugares</h1>
        <p className="gestion-page__sub">
          Estadios, teatros y salas donde se hacen los eventos, con sus sectores. Los organizadores
          eligen uno de estos lugares al crear un evento.
          {!wired && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <div className="gestion-card__actions gestion-card__actions--spaced">
        <GateLink enabled={wired} className="btn btn-primary" to="/gestion/lugares/nuevo">
          Nuevo lugar
        </GateLink>
      </div>

      {wired && (
        <form className="gestion-card gestion-card--spaced" onSubmit={onBuscar}>
          <div className="form-group">
            <label htmlFor="filtro-ciudad">Filtrar por ciudad</label>
            <input
              id="filtro-ciudad"
              className="form-input"
              value={ciudad}
              onChange={(e) => setCiudad(e.target.value)}
              placeholder="ej. Buenos Aires"
            />
          </div>
          <div className="gestion-card__actions">
            <button type="submit" className="btn">
              Buscar
            </button>
            {filtro && (
              <button
                type="button"
                className="btn"
                onClick={() => {
                  setCiudad('')
                  setFiltro('')
                }}
              >
                Ver todos
              </button>
            )}
          </div>
        </form>
      )}

      {!wired && (
        <div className="gestion-card">
          <p>La gestión de lugares necesita el backend real: no hay versión mock.</p>
        </div>
      )}
      {status === 'loading' && <LoadingState label="Cargando lugares…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {aviso && <p className="gestion-card__muted" role="status">✔ {aviso}</p>}
      {error && <p className="gestion-error" role="alert">{error}</p>}

      {wired && status === 'success' && items.length === 0 && (
        <div className="gestion-card">
          <p>{filtro ? `No hay lugares en «${filtro}».` : 'Todavía no hay lugares cargados.'}</p>
        </div>
      )}

      {status === 'success' && items.length > 0 && (
        <ul className="gestion-event-list">
          {items.map((l) => {
            const mapa = urlMapaLugar(l)
            return (
              <li key={l.id} className="gestion-event-row">
                <div className="gestion-event-row__main">
                  <h2>{l.nombre}</h2>
                  <p>
                    {[l.direccion, l.ciudad].filter(Boolean).join(' · ')}
                    {mapa && (
                      <>
                        {' · '}
                        <a href={mapa} target="_blank" rel="noopener noreferrer">
                          Ver en el mapa
                        </a>
                      </>
                    )}
                  </p>
                  <p className="gestion-card__muted">
                    {(l.sectores || []).length} sector(es) · capacidad total {l.capacidadTotal ?? 0}
                    {l.latitud == null || l.longitud == null ? ' · sin coordenadas' : ''}
                  </p>
                </div>
                <div className="gestion-event-row__actions">
                  <GateLink enabled={wired} className="btn" to={`/gestion/lugares/${l.id}`}>
                    Editar
                  </GateLink>
                  <GateButton
                    enabled={wired}
                    type="button"
                    className="btn"
                    disabled={busyId === l.id}
                    onClick={() => onEliminar(l)}
                  >
                    Eliminar
                  </GateButton>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <p className="gestion-page__foot">
        <Link to="/gestion">← Panel</Link>
      </p>
    </section>
  )
}

export default GestionLugaresPage
