import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useConfirm from '../../hooks/useConfirm'
import useAsync from '../../hooks/useAsync'
import { deleteLugar, listLugares } from '../../api/lugares'
import { urlMapaLugar } from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { formatEntero } from '../../lib/format'
import { PageHeader, Pill, RowMenu, Toolbar, VacioTabla } from '../../components/gestion/gestionUi'
import './GestionPage.css'

/**
 * Lugares (LUG-01) — solo ADMIN. Listado con filtro por ciudad, alta, edición (con sectores) y baja
 * (409 si el lugar tiene eventos: en ese caso solo se edita).
 */
const GestionLugaresPage = () => {
  const { token } = useAuth()
  const confirm = useConfirm()
  const wired = FEATURE.lugares
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(
    async (signal) => {
      const data = await listLugares({}, { token, signal })
      return Array.isArray(data) ? data : []
    },
    [token]
  )

  const {
    data: items = [],
    status,
    error: loadError,
    reload,
  } = useAsync(load, [token], { enabled: wired })

  const onEliminar = async (lugar) => {
    const ok = await confirm({
      title: `¿Eliminar el lugar «${lugar.nombre}»?`,
      description: 'Se eliminan también sus sectores. Esta acción no se puede deshacer.',
      confirmLabel: 'Eliminar',
      tone: 'danger',
    })
    if (!ok) return
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

  const [q, setQ] = useState('')
  const lista = (items || []).filter((l) => {
    const txt = [l.nombre, l.ciudad, l.direccion].join(' ').toLowerCase()
    return !q || txt.includes(q.trim().toLowerCase())
  })

  return (
    <>
      <PageHeader
        title="Lugares"
        subtitle="Estadios, teatros y salas con sus sectores. Los organizadores eligen uno al crear un evento."
      >
        <GateLink enabled={wired} className="btn btn-primary" to="/gestion/lugares/nuevo">
          Nuevo lugar
        </GateLink>
      </PageHeader>

      {!wired && <VacioTabla>La gestión de lugares necesita el backend real: no hay versión mock.</VacioTabla>}
      {status === 'loading' && <LoadingState label="Cargando lugares…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {aviso && <p className="g-ok" role="status">{aviso}</p>}
      {error && <p className="g-error" role="alert">{error}</p>}

      {wired && status === 'success' && (
        <>
          <Toolbar buscar={q} onBuscar={setQ} placeholder="Buscar por nombre, ciudad o dirección…" />
          {lista.length === 0 ? (
            <VacioTabla>{items.length === 0 ? 'Todavía no hay lugares cargados.' : 'Ningún lugar coincide con la búsqueda.'}</VacioTabla>
          ) : (
            <div className="g-table-wrap">
              <table className="g-table">
                <thead>
                  <tr>
                    <th>Lugar</th>
                    <th>Ciudad</th>
                    <th className="is-num">Sectores</th>
                    <th className="is-num">Capacidad</th>
                    <th>Mapa</th>
                    <th className="is-actions">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {lista.map((l) => {
                    const mapa = urlMapaLugar(l)
                    return (
                      <tr key={l.id}>
                        <td>
                          <div className="g-cell-main">
                            <strong>
                              <Link to={'/gestion/lugares/' + l.id}>{l.nombre}</Link>
                            </strong>
                            <span>{l.direccion || '—'}</span>
                          </div>
                        </td>
                        <td>{l.ciudad || '—'}</td>
                        <td className="is-num">{(l.sectores || []).length}</td>
                        <td className="is-num">{formatEntero(l.capacidadTotal ?? 0)}</td>
                        <td>
                          {mapa ? (
                            <a href={mapa} target="_blank" rel="noopener noreferrer">
                              Ver mapa
                            </a>
                          ) : (
                            <Pill tone="warning">Sin coordenadas</Pill>
                          )}
                        </td>
                        <td className="is-actions">
                          <RowMenu
                            label={'Acciones de ' + l.nombre}
                            items={[
                              { label: 'Editar', to: '/gestion/lugares/' + l.id },
                              {
                                label: 'Eliminar',
                                danger: true,
                                disabled: busyId === l.id,
                                onClick: () => onEliminar(l),
                              },
                            ]}
                          />
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  )
}

export default GestionLugaresPage
