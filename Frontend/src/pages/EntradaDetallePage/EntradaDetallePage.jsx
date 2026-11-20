import { useCallback, useMemo, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { HiCheck } from 'react-icons/hi'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { getEntrada } from '../../api/entradas'
import { getEvento } from '../../api/eventos'
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
import Button from '../../components/ui/Button'
import EntradaTicket from './EntradaTicket'
import '../MisEntradasPage/MisEntradasPage.css'
import './EntradaDetallePage.css'

/** Detalle + QR + gestión reclamo/devolución. */
const EntradaDetallePage = () => {
  const { entradaId } = useParams()
  const { user, token } = useAuth()
  const recienComprada = Boolean(useLocation().state?.bought)
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
      // La entrada no trae la portada ni (en datos viejos) la fecha: se completan con el evento.
      let evento = null
      if (ent?.idEvento) {
        try {
          evento = await getEvento(ent.idEvento, { token, signal })
        } catch (err) {
          if (err?.name === 'AbortError') throw err
        }
      }
      return {
        entrada: ent,
        evento,
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
  const evento = data?.evento || null
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
    <section className="mis-entradas-page container section-v-padding">
      <header className="entrada-page__head">
        <p className="entrada-page__back">
          <Link to="/mis-entradas">← Mis entradas</Link>
        </p>
        <h1 className="mis-entradas-page__title">Tu entrada</h1>
      </header>

      {recienComprada && status === 'success' && (
        <div className="entrada-bought" role="status">
          <span className="entrada-bought__icon" aria-hidden>
            <HiCheck />
          </span>
          <div>
            <h2>¡Compra confirmada!</h2>
            <p>Tu entrada ya está lista. Guardá el QR: lo vas a necesitar en el ingreso.</p>
          </div>
          <div className="entrada-bought__actions">
            <Button to="/mis-entradas" variant="secondary" size="sm">
              Ver mis entradas
            </Button>
            <Button to="/eventos" variant="ghost" size="sm">
              Seguir mirando eventos
            </Button>
          </div>
        </div>
      )}

      {status === 'loading' && (
        <LoadingState label="Cargando entrada…" />
      )}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {error && <p className="mis-entradas-page__error">{error}</p>}
      {msg && <p className="mis-entradas-page__ok">{msg}</p>}

      {status === 'success' && entrada && (
        <>
          <div className="entrada-layout">
            <EntradaTicket
              entrada={entrada}
              evento={evento}
              mostrarQr={Boolean(mostrarQr)}
              cancelada={Boolean(cancelada)}
            />

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
          </div>
        </>
      )}
    </section>
  )
}

export default EntradaDetallePage
