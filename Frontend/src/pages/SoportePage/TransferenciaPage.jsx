import { Link } from 'react-router-dom'
import './SoportePage.css'

/** Shell: POST transferencia → gateway / ms-ventas */
const TransferenciaPage = () => {
  return (
    <section className="soporte-page container-slim section-v-padding">
      <p className="soporte-back">
        <Link to="/soporte">← Soporte</Link>
      </p>
      <h1>Transferir entrada</h1>
      <p className="soporte-lead">
        Indicá la entrada y el email del nuevo titular. La operación la
        confirma ventas vía gateway. Formulario real en el próximo paso.
      </p>

      <form
        className="soporte-form"
        onSubmit={(e) => {
          e.preventDefault()
        }}
      >
        <div className="form-group">
          <label htmlFor="entradaId">ID de entrada</label>
          <input
            id="entradaId"
            name="entradaId"
            className="form-input"
            placeholder="ej. ent-001"
            disabled
          />
        </div>
        <div className="form-group">
          <label htmlFor="emailDestino">Email del destinatario</label>
          <input
            id="emailDestino"
            name="emailDestino"
            type="email"
            className="form-input"
            placeholder="usuario@mail.com"
            disabled
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled>
          Transferir (próximamente)
        </button>
      </form>

      <p className="soporte-page__foot">
        <Link to="/mis-entradas">Elegir desde mis entradas</Link>
      </p>
    </section>
  )
}

export default TransferenciaPage
