import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import {
  getEvento,
  listEventosGestion,
  patchEventoAltaDemanda,
  patchEventoEstado,
} from '../../api/eventos'
import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPO_LABELS,
  cupoTotalSectores,
} from '../../domain/evento'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import { PageHeader, Pill, RowMenu, Toolbar, VacioTabla } from '../../components/gestion/gestionUi'
import ConfirmarAltaDemandaModal from '../../components/gestion/ConfirmarAltaDemandaModal'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { formatFecha, formatMoney } from '../../lib/format'
import './GestionPage.css'

const GestionEventosPage = () => {
  const { user, token, isAdmin, canManageAllEvents } = useAuth()
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [confirmacion, setConfirmacion] = useState(null)

  const load = useCallback(
    async (signal) => {
      const data = await listEventosGestion(user, {
        isAdmin: canManageAllEvents,
        token,
        signal,
      })
      return Array.isArray(data) ? data : data?.content || data?.items || []
    },
    [user, token, canManageAllEvents]
  )

  const {
    data: items = [],
    status,
    error: loadError,
    reload,
  } = useAsync(load, [user?.id, token, canManageAllEvents], {
    enabled: FEATURE.eventosGestion,
  })

  const aplicarEstado = async (id, estado) => {
    setBusyId(id)
    setError('')
    try {
      await patchEventoEstado(id, estado, user, {
        isAdmin: canManageAllEvents,
        token,
      })
      await reload()
      toast.success(
        String(estado).toUpperCase() === EVENTO_ESTADOS.PUBLICADO
          ? 'Evento publicado'
          : `Estado actualizado: ${EVENTO_ESTADO_LABELS[String(estado).toUpperCase()] || estado}`
      )
    } catch (err) {
      setError(err.message || 'No se pudo actualizar')
      toast.error(err.message || 'No se pudo actualizar')
    } finally {
      setBusyId(null)
    }
  }

  const prepararPublicacion = async (id) => {
    setBusyId(id)
    setError('')
    try {
      const detalle = await getEvento(id, { token })
      setConfirmacion({
        id,
        cupoTotal: cupoTotalSectores(
          detalle?.sectores || detalle?.lugar?.sectores
        ),
        flagActual: Boolean(detalle?.esAltaDemanda),
      })
    } catch (err) {
      setError(err.message || 'No se pudo consultar el evento')
    } finally {
      setBusyId(null)
    }
  }

  const onEstado = async (id, estado) => {
    const next = String(estado || '').toUpperCase()
    if (next === EVENTO_ESTADOS.CANCELADO || next === 'CANCELADO') {
      if (!FEATURE.eventosCancelar || !isAdmin) return
    } else if (next === EVENTO_ESTADOS.SUSPENDIDO || next === 'SUSPENDIDO') {
      if (!FEATURE.eventosSuspender) return
    } else if (next === EVENTO_ESTADOS.PUBLICADO || next === 'PUBLICADO') {
      if (!FEATURE.eventosGestion) return
      await prepararPublicacion(id)
      return
    } else if (!FEATURE.eventosGestion) {
      return
    }
    await aplicarEstado(id, estado)
  }

  const confirmarPublicacion = async (esAltaDemanda) => {
    if (!confirmacion) return
    const { id, flagActual } = confirmacion
    setConfirmacion(null)
    if (esAltaDemanda !== flagActual) {
      setBusyId(id)
      setError('')
      try {
        await patchEventoAltaDemanda(id, esAltaDemanda, { token })
      } catch (err) {
        setError(err.message || 'No se pudo actualizar el alta demanda')
        setBusyId(null)
        return
      }
    }
    await aplicarEstado(id, EVENTO_ESTADOS.PUBLICADO)
  }

  const cancelarPublicacion = () => setConfirmacion(null)

  const [q, setQ] = useState('')
  const [fEstado, setFEstado] = useState('')

  const filtrados = items.filter((e) => {
    if (fEstado && e.estado !== fEstado) return false
    if (q && !String(e.nombre || '').toLowerCase().includes(q.trim().toLowerCase())) return false
    return true
  })

  const tono = {
    [EVENTO_ESTADOS.PUBLICADO]: 'success',
    [EVENTO_ESTADOS.BORRADOR]: 'neutral',
    [EVENTO_ESTADOS.SUSPENDIDO]: 'warning',
    [EVENTO_ESTADOS.CANCELADO]: 'danger',
    [EVENTO_ESTADOS.AGOTADO]: 'danger',
  }

  return (
    <>
      <PageHeader
        title={isAdmin ? 'Eventos' : 'Mis eventos'}
        subtitle={
          isAdmin
            ? 'Publicá, suspendé, reanudá o cancelá cualquier evento.'
            : 'Tus eventos. Podés publicar, suspender y volver a publicar; cancelar lo hace un administrador.'
        }
      >
        <GateLink enabled={FEATURE.eventosGestion} className="btn btn-primary" to="/gestion/eventos/nuevo">
          Nuevo evento
        </GateLink>
      </PageHeader>

      {error && <p className="g-error">{error}</p>}
      {status === 'loading' && <LoadingState label="Cargando eventos…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {!FEATURE.eventosGestion && (
        <VacioTabla>
          Gestión de eventos no cableada al backend en esta build. {GATE_HINT}
        </VacioTabla>
      )}

      {FEATURE.eventosGestion && status === 'success' && (
        <>
          <Toolbar
            buscar={q}
            onBuscar={setQ}
            placeholder="Buscar evento…"
            derecha={
              <button type="button" className="btn btn-secondary btn-sm" onClick={reload}>
                Actualizar
              </button>
            }
          >
            <select className="form-input" value={fEstado} onChange={(e) => setFEstado(e.target.value)} aria-label="Filtrar por estado">
              <option value="">Todos los estados</option>
              {Object.values(EVENTO_ESTADOS).map((s) => (
                <option key={s} value={s}>
                  {EVENTO_ESTADO_LABELS[s] || s}
                </option>
              ))}
            </select>
          </Toolbar>

          {filtrados.length === 0 ? (
            <VacioTabla>
              {items.length === 0 ? (
                <>
                  No hay eventos en este alcance.{' '}
                  <Link to="/gestion/eventos/nuevo">Crear el primero</Link>
                </>
              ) : (
                'Ningún evento coincide con el filtro.'
              )}
            </VacioTabla>
          ) : (
            <div className="g-table-wrap">
              <table className="g-table">
                <thead>
                  <tr>
                    <th>Evento</th>
                    <th>Fecha</th>
                    <th>Lugar</th>
                    <th>Estado</th>
                    <th className="is-num">Desde</th>
                    <th className="is-actions">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((e) => {
                    const ocupado = busyId === e.id
                    const esBorrador = e.estado === EVENTO_ESTADOS.BORRADOR
                    const publicado = e.estado === EVENTO_ESTADOS.PUBLICADO
                    const suspendido = e.estado === EVENTO_ESTADOS.SUSPENDIDO
                    const cerrado =
                      e.estado === EVENTO_ESTADOS.CANCELADO || e.estado === EVENTO_ESTADOS.FINALIZADO
                    return (
                      <tr key={e.id}>
                        <td>
                          <div className="g-cell-main">
                            <strong>{e.nombre}</strong>
                            <span>
                              {EVENTO_TIPO_LABELS[e.tipo] || e.tipo}
                              {e.esAltaDemanda ? ' · Alta demanda' : ''}
                            </span>
                          </div>
                        </td>
                        <td>{formatFecha(e.fechaInicio || e.fechaHora, { dateStyle: 'medium' })}</td>
                        <td>{e.lugar?.nombre || e.lugarNombre || '—'}</td>
                        <td>
                          <Pill tone={tono[e.estado] || 'neutral'}>{EVENTO_ESTADO_LABELS[e.estado] || e.estado}</Pill>
                        </td>
                        <td className="is-num">{e.precioDesde != null ? formatMoney(e.precioDesde) : '—'}</td>
                        <td className="is-actions">
                          <div className="g-row-actions">
                            {esBorrador && FEATURE.eventosGestion && (
                              <button
                                type="button"
                                className="btn btn-primary btn-sm"
                                disabled={ocupado}
                                onClick={() => onEstado(e.id, EVENTO_ESTADOS.PUBLICADO)}
                              >
                                Publicar
                              </button>
                            )}
                            {suspendido && FEATURE.eventosGestion && (
                              <button
                                type="button"
                                className="btn btn-primary btn-sm"
                                disabled={ocupado}
                                title="Reanuda ventas: el evento vuelve a PUBLICADO"
                                onClick={() => onEstado(e.id, EVENTO_ESTADOS.PUBLICADO)}
                              >
                                Reanudar
                              </button>
                            )}
                            <RowMenu
                              label={'Acciones de ' + e.nombre}
                              items={[
                                esBorrador &&
                                  FEATURE.eventosGestion && {
                                    label: 'Editar',
                                    to: '/gestion/eventos/' + e.id + '/editar',
                                  },
                                !esBorrador &&
                                  FEATURE.reportes && {
                                    label: 'Ver reportes',
                                    to: '/gestion/eventos/' + e.id + '/reportes',
                                  },
                                FEATURE.eventosGestion && {
                                  label: 'Duplicar',
                                  to: '/gestion/eventos/nuevo',
                                  state: { duplicarDe: e.id },
                                },
                                isAdmin &&
                                  e.esAltaDemanda && { label: 'Fila virtual', to: '/eventos/' + e.id + '/fila' },
                                publicado &&
                                  FEATURE.eventosSuspender && {
                                    label: 'Suspender ventas',
                                    onClick: () => onEstado(e.id, EVENTO_ESTADOS.SUSPENDIDO),
                                    disabled: ocupado,
                                  },
                                !cerrado && {
                                  label: 'Cancelar evento',
                                  danger: true,
                                  disabled: ocupado || !(FEATURE.eventosCancelar && isAdmin),
                                  title: !isAdmin ? 'Solo un administrador puede cancelar un evento' : undefined,
                                  onClick: () => onEstado(e.id, EVENTO_ESTADOS.CANCELADO),
                                },
                              ]}
                            />
                          </div>
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

      {confirmacion && (
        <ConfirmarAltaDemandaModal
          cupoTotal={confirmacion.cupoTotal}
          onConfirmar={confirmarPublicacion}
          onCancelar={cancelarPublicacion}
        />
      )}
    </>
  )
}

export default GestionEventosPage
