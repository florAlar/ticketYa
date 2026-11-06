import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { listMisEntradas } from '../../api/entradas'
import { crearSolicitudEntrada } from '../../api/soporteCliente'
import { ENTRADA_ESTADOS } from '../../domain/entrada'
import { DEVOLUCION_TIPOS } from '../../domain/devolucion'
import './SoportePage.css'

function formatFecha(iso) {
  if (!iso) return ''
  try {
    return new Date(iso).toLocaleString('es-AR', {
      dateStyle: 'short',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

/** Formulario real: devolución / reclamo desde soporte o preselección ?entrada= */
const DevolucionPage = () => {
  const { user, token } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const pre = params.get('entrada') || ''

  const [entradas, setEntradas] = useState([])
  const [entradaId, setEntradaId] = useState(pre)
  const [tipo, setTipo] = useState(DEVOLUCION_TIPOS.DEVOLUCION)
  const [motivo, setMotivo] = useState('')
  const [status, setStatus] = useState('loading')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const list = await listMisEntradas(user, { token })
      const vendidas = (Array.isArray(list) ? list : []).filter(
        (e) => e.estado === ENTRADA_ESTADOS.VENDIDA
      )
      setEntradas(vendidas)
      if (pre && vendidas.some((e) => e.id === pre)) setEntradaId(pre)
      else if (!pre && vendidas[0]) setEntradaId(vendidas[0].id)
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudieron cargar tus entradas')
      setStatus('error')
    }
  }, [user, token, pre])

  useEffect(() => {
    load()
  }, [load])

  const selected = useMemo(
    () => entradas.find((e) => e.id === entradaId),
    [entradas, entradaId]
  )

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!entradaId) return
    setSending(true)
    setError('')
    setOk('')
    try {
      const sol = await crearSolicitudEntrada(
        { idEntrada: entradaId, tipo, motivo },
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

      {status === 'loading' && <p>Cargando tus entradas…</p>}
      {error && <p className="soporte-error">{error}</p>}
      {ok && <p className="soporte-ok">{ok}</p>}

      {status === 'ok' && entradas.length === 0 && (
        <p>
          No tenés entradas válidas para gestionar.{' '}
          <Link to="/mis-entradas">Ver mis entradas</Link>
        </p>
      )}

      {status === 'ok' && entradas.length > 0 && (
        <form className="soporte-form" onSubmit={onSubmit}>
          <div className="form-group">
            <label htmlFor="entradaId">Entrada</label>
            <select
              id="entradaId"
              className="form-input"
              value={entradaId}
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
            disabled={sending || !entradaId}
          >
            {sending ? 'Enviando…' : 'Enviar solicitud'}
          </button>
        </form>
      )}
    </section>
  )
}

export default DevolucionPage
