import { useCallback, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { listMisEntradas } from '../../api/entradas'
import { crearSolicitudEntrada } from '../../api/soporteCliente'
import { ENTRADA_ESTADOS } from '../../domain/entrada'
import { DEVOLUCION_TIPOS } from '../../domain/devolucion'
import { formatFecha } from '../../lib/format'
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/AsyncState'
import './SoportePage.css'

/** Formulario real: devolución / reclamo desde soporte o preselección ?entrada= */
const DevolucionPage = () => {
  const { user, token } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const pre = params.get('entrada') || ''

  const [entradaId, setEntradaId] = useState(pre)
  const [tipo, setTipo] = useState(DEVOLUCION_TIPOS.DEVOLUCION)
  const [motivo, setMotivo] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  const load = useCallback(
    async (signal) => {
      const list = await listMisEntradas(user, { token, signal })
      return (Array.isArray(list) ? list : []).filter(
        (e) => e.estado === ENTRADA_ESTADOS.VENDIDA
      )
    },
    [user, token]
  )

  const {
    data: entradas = [],
    status,
    error: loadError,
    reload,
  } = useAsync(load, [user?.id, token], {
    enabled: Boolean(user?.id && token),
  })

  const selectedId = useMemo(() => {
    if (entradas.some((e) => e.id === entradaId)) return entradaId
    if (pre && entradas.some((e) => e.id === pre)) return pre
    return entradas[0]?.id || ''
  }, [entradas, entradaId, pre])

  const selected = useMemo(
    () => entradas.find((e) => e.id === selectedId),
    [entradas, selectedId]
  )

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!selectedId) return
    setSending(true)
    setError('')
    setOk('')
    try {
      const sol = await crearSolicitudEntrada(
        { idEntrada: selectedId, tipo, motivo },
        user,
        { token }
      )
      setOk('Solicitud enviada.')
      navigate(`/mis-entradas/${sol.idEntrada}`, { replace: true })
    } catch (err) {
      setError(err.message || 'No se pudo enviar')
    } finally {
      setSending(false)
    }
  }

  return (
    <section className="soporte-page container-slim section-v-padding">
      <p className="soporte-back">
        <Link to="/soporte">← Soporte</Link>
        {' · '}
        <Link to="/mis-entradas">Mis entradas</Link>
      </p>
      <h1>Solicitar devolución o reclamo</h1>
      <p className="soporte-lead">
        Elegí una entrada válida. Devolución: hasta 72 h antes del evento.
        Reclamo: problema con datos o la entrada. El admin aprueba o rechaza
        desde el panel de ventas.
      </p>

      {status === 'loading' && <LoadingState label="Cargando tus entradas…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}
      {error && <p className="soporte-error">{error}</p>}
      {ok && <p className="soporte-ok">{ok}</p>}

      {status === 'success' && entradas.length === 0 && (
        <EmptyState
          title="No tenés entradas válidas para gestionar."
          action={
            <Link className="btn" to="/mis-entradas">
              Ver mis entradas
            </Link>
          }
        />
      )}

      {status === 'success' && entradas.length > 0 && (
        <form className="soporte-form" onSubmit={onSubmit}>
          <div className="form-group">
            <label htmlFor="entradaId">Entrada</label>
            <select
              id="entradaId"
              className="form-input"
              value={selectedId}
              onChange={(e) => setEntradaId(e.target.value)}
              required
              disabled={sending}
            >
              {entradas.map((ent) => (
                <option key={ent.id} value={ent.id}>
                  {ent.eventoNombre} · {ent.sectorNombre}
                  {ent.eventoFecha ? ` · ${formatFecha(ent.eventoFecha)}` : ''}
                  {` (${ent.id})`}
                </option>
              ))}
            </select>
          </div>

          {selected && (
            <p className="soporte-lead">
              Seleccionada: <strong>{selected.eventoNombre}</strong> ·{' '}
              {selected.sectorNombre} · ${selected.precio}
            </p>
          )}

          <div className="form-group">
            <label htmlFor="tipo">Tipo</label>
            <select
              id="tipo"
              className="form-input"
              value={tipo}
              onChange={(e) => setTipo(e.target.value)}
              disabled={sending}
            >
              <option value={DEVOLUCION_TIPOS.DEVOLUCION}>Devolución</option>
              <option value={DEVOLUCION_TIPOS.RECLAMO}>Reclamo</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="motivo">Motivo</label>
            <textarea
              id="motivo"
              className="form-textarea"
              rows={4}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              required
              minLength={5}
              disabled={sending}
              placeholder="Contanos el motivo"
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={sending || !selectedId}
          >
            {sending ? 'Enviando…' : 'Enviar solicitud'}
          </button>
        </form>
      )}
    </section>
  )
}

export default DevolucionPage
