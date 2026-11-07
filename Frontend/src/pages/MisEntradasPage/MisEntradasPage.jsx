import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { listMisEntradas } from '../../api/entradas'
import { listMisSolicitudes } from '../../api/soporteCliente'
import { ENTRADA_ESTADO_LABELS, ENTRADA_ESTADOS } from '../../domain/entrada'
import {
  DEVOLUCION_ESTADO_LABELS,
  DEVOLUCION_TIPO_LABELS,
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

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const [ents, sols] = await Promise.all([
        listMisEntradas(user, { token }),
        listMisSolicitudes(user, { token }),
      ])
      setItems(Array.isArray(ents) ? ents : [])
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

  const pendientePorEntrada = useMemo(() => {
    const map = new Map()
    for (const s of solicitudes) {
      if (s.estado !== 'PENDIENTE') continue
      map.set(s.idEntrada, s)
    }
    return map
  }, [solicitudes])

  return (
    <section className="mis-entradas-page container-slim section-v-padding">
      <header className="mis-entradas-page__head">
        <h1 className="mis-entradas-page__title">Mis entradas</h1>
        <p className="mis-entradas-page__sub">
          Entradas asociadas a tu cuenta
          {user?.email ? ` (${user.email})` : ''}. Desde el detalle podés
          abrir un reclamo o una devolución.
        </p>
      </header>

      {status === 'loading' && (
        <p className="mis-entradas-page__muted">Cargando entradas…</p>
      )}

      {error && <p className="mis-entradas-page__error">{error}</p>}

      {status === 'ok' && items.length === 0 && (
        <div className="mis-entradas-empty">
          <p>Todavía no tenés entradas.</p>
          <p className="mis-entradas-page__muted">
            Cuando compres, van a aparecer acá.
          </p>
          <Link className="btn btn-primary" to="/eventos">
            Ver eventos
          </Link>
        </div>
      )}

      {items.length > 0 && (
        <ul className="mis-entradas-list">
          {items.map((e) => {
            const pend = pendientePorEntrada.get(e.id)
            const puedeReclamo = e.estado === ENTRADA_ESTADOS.VENDIDA && !pend
            return (
              <li key={e.id} className="mis-entradas-card">
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
                    {' · '}
                    <strong>
                      {ENTRADA_ESTADO_LABELS[e.estado] || e.estado}
                    </strong>
                  </p>
                  {pend && (
                    <p className="mis-entradas-page__badge">
                      {DEVOLUCION_TIPO_LABELS[pend.tipo] || pend.tipo} pendiente
                    </p>
                  )}
                </div>
                <div className="mis-entradas-card__actions">
                  <Link className="btn btn-primary" to={`/mis-entradas/${e.id}`}>
                    Ver QR
                  </Link>
                  {puedeReclamo && (
                    <GateLink
                      enabled={FEATURE.devoluciones}
                      className="btn"
                      to={`/mis-entradas/${e.id}#reclamo`}
                    >
                      Reclamo / devolución
                    </GateLink>
                  )}
                  {pend && (
                    <Link className="btn" to={`/mis-entradas/${e.id}`}>
                      Ver solicitud
                    </Link>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {solicitudes.length > 0 && (
        <section style={{ marginTop: '2rem' }}>
          <h2 className="mis-entradas-page__title" style={{ fontSize: '1.15rem' }}>
            Mis solicitudes
          </h2>
          <ul className="mis-entradas-list">
            {solicitudes.map((s) => (
              <li key={s.id} className="mis-entradas-card">
                <div className="mis-entradas-card__body">
                  <h2 style={{ fontSize: '1rem' }}>
                    {DEVOLUCION_TIPO_LABELS[s.tipo] || s.tipo}
                    {' · '}
                    {s.eventoNombre || s.idEntrada}
                  </h2>
                  <p className="mis-entradas-page__muted">
                    {formatFecha(s.fechaSolicitud)} ·{' '}
                    <strong>
                      {DEVOLUCION_ESTADO_LABELS[s.estado] || s.estado}
                    </strong>
                  </p>
                  <p>{s.motivo}</p>
                </div>
                <Link className="btn" to={`/mis-entradas/${s.idEntrada}`}>
                  Ir a la entrada
                </Link>
              </li>
            ))}
          </ul>
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
