import { Link } from 'react-router-dom'
import { FEATURE } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import './SoportePage.css'

/**
 * Hub soporte cliente.
 * Devolución cableada si ENTRADAS demock; transferencia shell → gated.
 */
const SoportePage = () => {
  return (
    <section className="soporte-page container section-v-padding">
      <header className="soporte-page__head">
        <h1>Soporte</h1>
        <p>
          Gestiones sobre tus entradas: devoluciones y transferencias.
          Las opera el servicio de ventas a través del gateway.
        </p>
      </header>

      <div className="soporte-page__grid">
        <article className="soporte-card">
          <h2>Devolución / reclamo</h2>
          <p>
            Pedí la devolución de una entrada hasta 72 h antes del evento, o
            abrí un reclamo si hay un problema con la entrada. Un administrador
            aprueba o rechaza desde el panel de ventas.
          </p>
          <ul className="soporte-card__list">
            <li>Entrada válida y no utilizada</li>
            <li>Devolución: dentro del plazo (72 h antes)</li>
            <li>Seguimiento en Mis entradas</li>
          </ul>
          <GateLink
            enabled={FEATURE.devoluciones}
            className="soporte-card__cta"
            to="/soporte/devolucion"
          >
            Solicitar ahora
          </GateLink>
        </article>

        <article className="soporte-card">
          <h2>Transferencia</h2>
          <p>
            Cedé una entrada a otro usuario de TicketYa. El destinatario
            recibe la entrada; vos dejás de figurar como titular.
          </p>
          <ul className="soporte-card__list">
            <li>Entrada propia, no usada ni en devolución</li>
            <li>Email o usuario destino</li>
            <li>Confirmación antes de completar</li>
          </ul>
          <GateLink
            enabled={FEATURE.transferencia}
            className="soporte-card__cta"
            to="/soporte/transferencia"
          >
            Transferir entrada
          </GateLink>
        </article>
      </div>

      <p className="soporte-page__foot">
        ¿Ya tenés entradas?{' '}
        <Link to="/mis-entradas">Ir a mis entradas</Link>
      </p>
    </section>
  )
}

export default SoportePage
