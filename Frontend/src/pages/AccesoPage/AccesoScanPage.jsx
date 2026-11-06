import { useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { validarIngreso } from '../../api/entradas'
import '../GestionPage/GestionPage.css'

/**
 * Validación de ingreso.
 * POST /api/eventos/{idEvento}/validaciones (contrato back).
 * Mock: sin idEvento obligatorio busca por código en store.
 */
const AccesoScanPage = () => {
  const { user, token } = useAuth()
  const [idEvento, setIdEvento] = useState('')
  const [codigo, setCodigo] = useState('')
  const [msg, setMsg] = useState('')
  const [ok, setOk] = useState(false)
  const [busy, setBusy] = useState(false)

  const onSubmit = async (e) => {
    e.preventDefault()
    const value = codigo.trim()
    const evento = idEvento.trim()
    if (!value) {
      setOk(false)
      setMsg('Ingresá un código QR o ID de entrada.')
      return
    }
    if (!evento) {
      setOk(false)
      setMsg('Indicá el id del evento (requerido por el contrato back).')
      return
    }

    setBusy(true)
    setMsg('')
    try {
      const res = await validarIngreso(
        evento,
        { codigoQR: value },
        user,
        { token }
      )
      setOk(true)
      setMsg(res?.mensaje || 'Ingreso validado.')
      setCodigo('')
    } catch (err) {
      setOk(false)
      setMsg(err.message || 'No se pudo validar')
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
          Contrato: POST /api/eventos/&#123;idEvento&#125;/validaciones. Cámara nativa:
          después.
        </p>
      </header>

      <form className="gestion-card acceso-scan" onSubmit={onSubmit}>
        <label htmlFor="id-evento">ID de evento</label>
        <input
          id="id-evento"
          className="form-input"
          value={idEvento}
          onChange={(e) => setIdEvento(e.target.value)}
          placeholder="ej. e1"
          autoComplete="off"
          disabled={busy}
        />

        <label htmlFor="codigo-entrada">Código QR / ID entrada</label>
        <input
          id="codigo-entrada"
          className="form-input"
          value={codigo}
          onChange={(e) => setCodigo(e.target.value)}
          placeholder="ej. QR-DEMO-001-NOCHE-INDIE"
          autoComplete="off"
          disabled={busy}
        />
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Validando…' : 'Validar ingreso'}
        </button>
        {msg && (
          <p className={ok ? 'gestion-card__muted' : 'gestion-error'}>{msg}</p>
        )}
        <Link to="/acceso">← Volver al hub de acceso</Link>
      </form>
    </section>
  )
}

export default AccesoScanPage
