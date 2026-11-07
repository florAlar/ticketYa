import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { listMisEntradas } from '../../api/entradas'
import { listMisSolicitudes } from '../../api/soporteCliente'
import {
  ENTRADA_ESTADO_LABELS,
  ENTRADA_ESTADOS,
  isEntradaQrExponible,
  isEntradaReintegroCancelacion,
  isEntradaEventoSuspendido,
  isEntradaVisibleEnListado,
} from '../../domain/entrada'
import {
  DEVOLUCION_ESTADO_LABELS,
  DEVOLUCION_TIPO_LABELS,
  DEVOLUCION_TIPOS,
  isDevolucionPorCancelacion,
} from '../../domain/devolucion'
import { FEATURE } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import './MisEntradasPage.css'

function formatFecha(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

function formatMoney(n) {
  try {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(n ?? 0)
  } catch {
    return `$${n ?? 0}`
  }
}

const MisEntradasPage = () => {
  const { user, token } = useAuth()
  const [items, setItems] = useState([])
  const [solicitudes, setSolicitudes] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [showDevoluciones, setShowDevoluciones] = useState(false)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const ents = await listMisEntradas(user, { token })
      const listEnts = Array.isArray(ents) ? ents : []
      const sols = await listMisSolicitudes(user, {
        token,
        entradas: listEnts,
      })
      setItems(listEnts)
      setSolicitudes(Array.isArray(sols) ? sols : [])
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudieron cargar las entradas')
      setStatus('error')
    }
  }, [user, token])

  useEffect(() => {
    load()
  }, [load])

  const listado = useMemo(() => {
    const visible = items.filter((e) => isEntradaVisibleEnListado(e))
    // activas primero, canceladas después
    return visible.sort((a, b) => {
      const ac = isEntradaReintegroCancelacion(a) ? 1 : 0
      const bc = isEntradaReintegroCancelacion(b) ? 1 : 0
      return ac - bc
    })
  }, [items])

  const pendientePorEntrada = useMemo(() => {
    const map = new Map()
    for (const s of solicitudes) {
      if (s.estado !== 'PENDIENTE') continue
      map.set(s.idEntrada, s)
    }
    return map
  }, [solicitudes])

  const tramitePorEntrada = useMemo(() => {
    const map = new Map()
    for (const s of solicitudes) {
      if (!s.idEntrada) continue
      if (!map.has(s.idEntrada)) map.set(s.idEntrada, s)
    }
    return map
  }, [solicitudes])

  const revealDevoluciones = useCallback(() => {
    setShowDevoluciones(true)
    requestAnimationFrame(() => {
      document.getElementById('mis-devoluciones')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    })
  }, [])

  return (
    <section className="mis-entradas-page container-slim section-v-padding">
      <header className="mis-entradas-page__head">
        <h1 className="mis-entradas-page__title">Mis entradas</h1>
        <p className="mis-entradas-page__sub">
          Entradas de tu cuenta
          {user?.email ? ` (${user.email})` : ''}. El QR solo en tickets
          válidos. Si el evento se suspende, tu entrada sigue vigente (con
          aviso). Si se cancela, queda acá sin QR y el reintegro en devoluciones.
        </p>
      </header>

      {status === 'loading' && (
        <p className="mis-entradas-page__muted">Cargando entradas…</p>
      )}

      {error && <p className="mis-entradas-page__error">{error}</p>}

      {status === 'ok' && listado.length === 0 && (
        <div className="mis-entradas-empty">
          <p>No tenés entradas para mostrar.</p>
          <p className="mis-entradas-page__muted">
            Cuando compres, aparecen acá. Las usadas quedan fuera del listado
            principal.
          </p>
          <Link className="btn btn-primary" to="/eventos">
            Ver eventos
          </Link>
        </div>
      )}

      {listado.length > 0 && (
        <ul className="mis-entradas-list">
          {listado.map((e) => {
            const cancelada = isEntradaReintegroCancelacion(e)
            const suspendida = isEntradaEventoSuspendido(e)
            const pend = pendientePorEntrada.get(e.id)
            const tramite = tramitePorEntrada.get(e.id)
            const puedeReclamo =
              !cancelada &&
              e.estado === ENTRADA_ESTADOS.VENDIDA &&
              !pend &&
              isEntradaQrExponible(e)
            const showQr = isEntradaQrExponible(e)

            return (
              <li
                key={e.id}
                className={
                  cancelada
                    ? 'mis-entradas-card mis-entradas-card--cancelada'
                    : suspendida
                      ? 'mis-entradas-card mis-entradas-card--suspendida'
                      : 'mis-entradas-card'
                }
              >
                <div className="mis-entradas-card__body">
                  <h2>{e.eventoNombre}</h2>
                  <p>
                    {formatFecha(e.eventoFecha)}
                    {e.lugarCiudad ? ` · ${e.lugarCiudad}` : ''}
                  </p>
                  <p className="mis-entradas-page__muted">
                    {e.sectorNombre}
                    {e.fila != null && e.asiento != null
                      ? ` · Fila ${e.fila} Asiento ${e.asiento}`
                      : ''}
                    {' · '}
                    {formatMoney(e.precio)}
                    {!cancelada && (
                      <>
                        {' · '}
                        <strong>
                          {ENTRADA_ESTADO_LABELS[e.estado] || e.estado}
                        </strong>
                      </>
                    )}
                  </p>

                  {cancelada && (
                    <>
                      <p className="mis-entradas-page__badge mis-entradas-page__badge--cancel">
                        Evento cancelado
                      </p>
                      <p className="mis-entradas-page__aviso">
                        No hay QR de ingreso. El reintegro lo gestiona la
                        plataforma: te vamos a avisar cuando se acredite el
                        dinero.
                        {tramite?.id ? (
                          <>
                            {' '}
                            Trámite:{' '}
                            <strong className="mis-entradas-page__mono">
                              {tramite.id}
                            </strong>
                          </>
                        ) : null}
                      </p>
                    </>
                  )}

                  {suspendida && !cancelada && (
                    <>
                      <p className="mis-entradas-page__badge mis-entradas-page__badge--suspend">
                        Evento suspendido
                      </p>
                      <p className="mis-entradas-page__aviso">
                        El organizador pausó la venta de entradas nuevas. Tu
                        entrada sigue vigente y el QR de ingreso se mantiene.
                      </p>
                    </>
                  )}

                  {pend && !cancelada && (
                    <p className="mis-entradas-page__badge">
                      {DEVOLUCION_TIPO_LABELS[pend.tipo] || pend.tipo} pendiente
                    </p>
                  )}
                </div>
                <div className="mis-entradas-card__actions">
                  {cancelada ? (
                    <button
                      type="button"
                      className="btn"
                      onClick={revealDevoluciones}
                    >
                      Ver trámite
                    </button>
                  ) : showQr ? (
                    <Link
                      className="btn btn-primary"
                      to={`/mis-entradas/${e.id}`}
                    >
                      Ver QR
                    </Link>
                  ) : (
                    <Link className="btn" to={`/mis-entradas/${e.id}`}>
                      Ver detalle
                    </Link>
                  )}
                  {puedeReclamo && (
                    <GateLink
                      enabled={FEATURE.devoluciones}
                      className="btn"
                      to={`/mis-entradas/${e.id}#reclamo`}
                    >
                      Reclamo / devolución
                    </GateLink>
                  )}
                  {pend && !cancelada && (
                    <button
                      type="button"
                      className="btn"
                      onClick={revealDevoluciones}
                    >
                      Ver solicitud
                    </button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <div className="mis-entradas-devo-toggle" style={{ marginTop: '1.75rem' }}>
        <button
          type="button"
          className="btn"
          onClick={() => setShowDevoluciones((v) => !v)}
          aria-expanded={showDevoluciones}
        >
          {showDevoluciones ? 'Ocultar devoluciones' : 'Ver devoluciones'}
          {solicitudes.length > 0 ? ` (${solicitudes.length})` : ''}
        </button>
      </div>

      {showDevoluciones && (
        <section style={{ marginTop: '1rem' }} id="mis-devoluciones">
          <h2
            className="mis-entradas-page__title"
            style={{ fontSize: '1.15rem' }}
          >
            Mis devoluciones y reintegros
          </h2>
          <p className="mis-entradas-page__muted">
            Todos los trámites de dinero en un solo lugar (voluntarios y por
            cancelación de evento). No son tickets de ingreso.
          </p>
          {solicitudes.length === 0 ? (
            <p className="mis-entradas-page__muted">
              Todavía no tenés devoluciones ni reintegros.
            </p>
          ) : (
            <ul className="mis-entradas-list">
              {solicitudes.map((s) => {
                const porCancel = isDevolucionPorCancelacion(s)
                return (
                  <li
                    key={s.id}
                    className={
                      porCancel
                        ? 'mis-entradas-card mis-entradas-card--cancelada'
                        : 'mis-entradas-card'
                    }
                    id={`tramite-${s.id}`}
                  >
                    <div className="mis-entradas-card__body">
                      <h2 style={{ fontSize: '1rem' }}>
                        {DEVOLUCION_TIPO_LABELS[s.tipo] ||
                          s.tipo ||
                          'Devolución'}
                        {' · '}
                        {s.eventoNombre || 'Evento'}
                      </h2>
                      <p className="mis-entradas-page__muted">
                        N° trámite:{' '}
                        <strong className="mis-entradas-page__mono">
                          {s.id}
                        </strong>
                      </p>
                      <p className="mis-entradas-page__muted">
                        {formatFecha(s.fechaSolicitud)} ·{' '}
                        <strong>
                          {DEVOLUCION_ESTADO_LABELS[s.estado] || s.estado}
                        </strong>
                        {s.monto != null ? ` · ${formatMoney(s.monto)}` : ''}
                      </p>
                      {s.motivo ? <p>{s.motivo}</p> : null}
                      {porCancel && (
                        <p className="mis-entradas-page__aviso">
                          Te vamos a informar cuando el pago haya devuelto el
                          dinero.
                        </p>
                      )}
                      {s.idEntrada ? (
                        <p className="mis-entradas-page__muted">
                          Ref. entrada: {s.idEntrada}
                          {s.tipo === DEVOLUCION_TIPOS.CANCELACION_EVENTO ||
                          porCancel
                            ? ' · origen: cancelación de evento'
                            : ' · origen: solicitud tuya'}
                        </p>
                      ) : null}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      )}

      <p className="mis-entradas-page__foot">
        <Link to="/soporte">Soporte</Link>
        {' · '}
        <button
          type="button"
          className="mis-entradas-page__linkish"
          onClick={load}
        >
          Actualizar
        </button>
      </p>
    </section>
  )
}

export default MisEntradasPage
