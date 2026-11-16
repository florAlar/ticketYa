import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { validarIngreso } from '../../api/entradas'
import { listEventos } from '../../api/eventos'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import '../GestionPage/GestionPage.css'

/**
 * Validación de ingreso en la puerta (E2-ACC-01).
 * POST /api/eventos/{idEvento}/validaciones { codigoQR } → 200 VALIDA | 409 con `resultado`
 * (YA_USADA, OTRO_EVENTO, NO_VALIDA, EVENTO_CANCELADO). Cualquier PERSONAL_ACCESO puede validar
 * cualquier evento (no hay asignación de eventos por persona).
 */
const AccesoScanPage = () => {
  const { user, token } = useAuth()
  const wired = FEATURE.accesoScan
  const [eventos, setEventos] = useState([])
  const [idEvento, setIdEvento] = useState('')
  const [codigo, setCodigo] = useState('')
  const [msg, setMsg] = useState('')
  const [ok, setOk] = useState(false)
  const [entrada, setEntrada] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let vivo = true
    listEventos()
      .then((list) => {
        if (vivo) setEventos(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (vivo) setEventos([])
      })
    return () => {
      vivo = false
    }
  }, [])

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!wired) return
    const value = codigo.trim()
    if (!idEvento) {
      setOk(false)
      setEntrada(null)
      setMsg('Elegí el evento de esta puerta.')
      return
    }
    if (!value) {
      setOk(false)
      setEntrada(null)
      setMsg('Ingresá el código QR.')
      return
    }

    setBusy(true)
    setMsg('')
    setEntrada(null)
    try {
      const res = await validarIngreso(idEvento, { codigoQR: value }, user, { token })
      setOk(true)
      setMsg(res?.mensaje || 'Ingreso validado.')
      setEntrada(res?.entrada || null)
      setCodigo('')
    } catch (err) {
      setOk(false)
      // 409: el motivo del rechazo viene en el body (mensaje + resultado + entrada)
      setMsg(err.data?.mensaje || err.message || 'No se pudo validar')
      setEntrada(err.data?.entrada || null)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/acceso">Acceso</Link> / scan
        </p>
        <h1 className="gestion-page__title">Validar ingreso</h1>
        <p className="gestion-page__sub">
          Elegí el evento de la puerta y cargá el código QR de la entrada. Cada
          entrada ingresa una sola vez.
          {!wired && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      <form className="gestion-card acceso-scan" onSubmit={onSubmit}>
        <label htmlFor="id-evento">Evento</label>
        <select
          id="id-evento"
          className="form-input"
          value={idEvento}
          onChange={(e) => setIdEvento(e.target.value)}
          disabled={busy || !wired}
        >
          <option value="">Elegí un evento…</option>
          {eventos.map((ev) => (
            <option key={ev.id} value={ev.id}>
              {ev.nombre}
            </option>
          ))}
        </select>

        <label htmlFor="codigo-entrada">Código QR</label>
        <input
          id="codigo-entrada"
          className="form-input"
          value={codigo}
          onChange={(e) => setCodigo(e.target.value)}
          placeholder="ej. QR-8F3A1C5D2B7E9A40"
          autoComplete="off"
          disabled={busy || !wired}
        />
        <GateButton
          enabled={wired}
          type="submit"
          className="btn btn-primary"
          disabled={busy}
        >
          {busy ? 'Validando…' : 'Validar ingreso'}
        </GateButton>
        {msg && (
          <p className={ok ? 'gestion-card__muted' : 'gestion-error'} role="status">
            {ok ? '✔ ' : '✖ '}
            {msg}
          </p>
        )}
        {entrada && (
          <p className="gestion-card__muted">
            {entrada.eventoNombre ? <strong>{entrada.eventoNombre}</strong> : null}
            {entrada.sectorNombre ? ` · ${entrada.sectorNombre}` : ''}
            {entrada.fila ? ` · Fila ${entrada.fila}, asiento ${entrada.asiento}` : ''}
          </p>
        )}
        <Link to="/acceso">← Volver al hub de acceso</Link>
      </form>
    </section>
  )
}

export default AccesoScanPage
