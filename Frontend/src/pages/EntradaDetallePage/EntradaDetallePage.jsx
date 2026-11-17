import { useCallback, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { getEntrada } from '../../api/entradas'
import { formatFecha } from '../../lib/format'
import {
  crearSolicitudEntrada,
  listSolicitudesEntrada,
} from '../../api/soporteCliente'
import { ENTRADA_ESTADOS, ENTRADA_ESTADO_LABELS, isEntradaQrExponible, isEntradaReintegroCancelacion } from '../../domain/entrada'
import {
  DEVOLUCION_ESTADO_LABELS,
  DEVOLUCION_TIPOS,
  DEVOLUCION_TIPO_LABELS,
} from '../../domain/devolucion'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import '../MisEntradasPage/MisEntradasPage.css'

/** Detalle + QR + gestión reclamo/devolución. */
const EntradaDetallePage = () => {
  const { entradaId } = useParams()
  const { user, token } = useAuth()
  const [msg, setMsg] = useState('')
  const [tipo, setTipo] = useState(DEVOLUCION_TIPOS.DEVOLUCION)
  const [motivo, setMotivo] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(
    async (signal) => {
      const [ent, sols] = await Promise.all([
        getEntrada(entradaId, user, { token, signal }),
        listSolicitudesEntrada(entradaId, user, { token, signal }),
      ])
      return {
        entrada: ent,
        solicitudes: Array.isArray(sols) ? sols : [],
      }
    },
    [entradaId, user, token]
  )

  const { data, status, error: loadError, reload } = useAsync(
    load,
    [entradaId, user?.id, token],
    { enabled: Boolean(user?.id && entradaId) }
  )

  const entrada = data?.entrada || null
  const solicitudes = useMemo(() => data?.solicitudes || [], [data])

  const pendiente = useMemo(
    () => solicitudes.some((s) => s.estado === 'PENDIENTE'),
    [solicitudes]
  )

  const puedeGestionar =
    entrada &&
    entrada.estado === ENTRADA_ESTADOS.VENDIDA &&
    isEntradaQrExponible(entrada) &&
    !pendiente

  const mostrarQr = entrada && isEntradaQrExponible(entrada) && !!entrada.codigoQR
  const cancelada = entrada && isEntradaReintegroCancelacion(entrada)

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!FEATURE.devoluciones) return
    setSending(true)
    setMsg('')
    setError('')
    try {
      await crearSolicitudEntrada(
        { idEntrada: entradaId, tipo, motivo },
        user,
        { token }
      )
      setMotivo('')
      setMsg('Solicitud enviada. Un administrador la revisará.')
      await reload()
    } catch (err) {
      setError(err.message || 'No se pudo enviar')
    } finally {
      setSending(false)
    }
  }

  return (
    <section className="mis-entradas-page container-slim section-v-padding">
      <header className="mis-entradas-page__head">
        <p className="mis-entradas-page__muted">
          <Link to="/mis-entradas">← Mis entradas</Link>
        </p>
        <h1 className="mis-entradas-page__title">
          {entrada?.eventoNombre || 'Entrada'}
        </h1>
      </header>

      {status === 'loading' && (
        <LoadingState label="Cargando entrada…" />
      )}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {error && <p className="mis-entradas-page__error">{error}</p>}
      {msg && <p className="mis-entradas-page__ok">{msg}</p>}

      {status === 'success' && entrada && (
        <>
          <div className="mis-entradas-card mis-entradas-card--stack">
            <p>
              <strong>Estado:</strong>{' '}
              {ENTRADA_ESTADO_LABELS[entrada.estado] || entrada.estado}
            </p>
            <p>
              <strong>Fecha evento:</strong> {formatFecha(entrada.eventoFecha)}
            </p>
            <p>
              <strong>Lugar:</strong>{' '}
              {[entrada.lugarNombre, entrada.lugarCiudad]
                .filter(Boolean)
                .join(' · ') || '—'}
            </p>
            <p>
              <strong>Sector:</strong> {entrada.sectorNombre}
              {entrada.fila != null && entrada.asiento != null
                ? ` · Fila ${entrada.fila} Asiento ${entrada.asiento}`
                : ''}
            </p>
            <p>
              <strong>ID:</strong> {entrada.id}
            </p>
            <p>
              <strong>Compra:</strong> {entrada.idCompra}
            </p>
            {entrada.eventoEstado ? (
              <p>
                <strong>Evento:</strong> {entrada.eventoEstado}
              </p>
            ) : null}

            {mostrarQr ? (
              <div className="entrada-qr-box" aria-label="Código QR de la entrada">
                <p className="entrada-qr-box__label">Código QR</p>
                <p className="entrada-qr-box__code">{entrada.codigoQR}</p>
              </div>
            ) : cancelada ? (
              <div className="entrada-detalle__notice">
                <p className="mis-entradas-page__badge mis-entradas-page__badge--cancel">
                  Evento cancelado
                </p>
                <p className="mis-entradas-page__aviso" role="status">
                  No hay QR de ingreso. El reintegro lo gestiona la plataforma:
                  te vamos a avisar cuando se acredite el dinero. El número de
                  trámite está en Mis entradas → Ver devoluciones.
                </p>
              </div>
            ) : (
              <p
                className="mis-entradas-page__muted entrada-detalle__notice"
                role="status"
              >
                Esta entrada no tiene QR de ingreso (estado{' '}
                {ENTRADA_ESTADO_LABELS[entrada.estado] || entrada.estado}
                {entrada.eventoEstado === 'CANCELADO'
                  ? ', evento cancelado'
                  : ''}
                ).
              </p>
            )}
          </div>

          {!cancelada && (
          <section className="entrada-gestion">
            <h2 className="mis-entradas-page__title mis-entradas-page__title--sub">
              Reclamo / devolución
            </h2>

            {solicitudes.length > 0 && (
              <ul className="mis-entradas-list mis-entradas-list--spaced">
                {solicitudes.map((s) => (
                  <li key={s.id} className="mis-entradas-card">
                    <div className="mis-entradas-card__body">
                      <h2 className="mis-entradas-page__title--sm">
                        {DEVOLUCION_TIPO_LABELS[s.tipo] || s.tipo}
                      </h2>
                      <p className="mis-entradas-page__muted">
                        {formatFecha(s.fechaSolicitud)} ·{' '}
                        <strong>
                          {DEVOLUCION_ESTADO_LABELS[s.estado] || s.estado}
                        </strong>
                      </p>
                      <p>{s.motivo}</p>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {puedeGestionar ? (
              <form className="mis-entradas-card mis-entradas-card--form" onSubmit={onSubmit}>
                <p className="mis-entradas-page__muted">
                  Devolución: hasta 72 h antes del evento. Reclamo: error de
                  datos / problema con la entrada.
                  {!FEATURE.devoluciones && (
                    <>
                      {' '}
                      <strong title={GATE_HINT}>({GATE_HINT})</strong>
                    </>
                  )}
                </p>
                <div className="form-group">
                  <label htmlFor="sol-tipo">Tipo</label>
                  <select
                    id="sol-tipo"
                    className="form-input"
                    value={tipo}
                    onChange={(e) => setTipo(e.target.value)}
                    disabled={sending || !FEATURE.devoluciones}
                  >
                    <option value={DEVOLUCION_TIPOS.DEVOLUCION}>
                      Devolución
                    </option>
                    <option value={DEVOLUCION_TIPOS.RECLAMO}>Reclamo</option>
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="sol-motivo">Motivo</label>
                  <textarea
                    id="sol-motivo"
                    className="form-textarea"
                    rows={3}
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                    required
                    minLength={5}
                    disabled={sending || !FEATURE.devoluciones}
                    placeholder="Contanos el motivo…"
                  />
                </div>
                <GateButton
                  enabled={FEATURE.devoluciones}
                  type="submit"
                  className="btn btn-primary"
                  disabled={sending}
                >
                  {sending ? 'Enviando…' : 'Enviar solicitud'}
                </GateButton>
              </form>
            ) : (
              <p className="mis-entradas-page__muted">
                {pendiente
                  ? 'Ya tenés una solicitud pendiente sobre esta entrada. Esperá la respuesta del admin.'
                  : 'No se puede gestionar reclamo/devolución en el estado actual de la entrada.'}
              </p>
            )}
          </section>
          )}
        </>
      )}
    </section>
  )
}

export default EntradaDetallePage
