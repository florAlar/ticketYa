import { Link } from 'react-router-dom'

/**
 * Tercera solapa del box de acceso: cómo ser organizador y quién da de alta a
 * administración y acceso. Las cuentas demo viven en la pastilla flotante.
 */
export default function OrganizadorInfo({ onCrearCuenta, onVerDemo }) {

  return (
    <div className="org-info">
      <section className="org-info__bloque">
        <h2 className="org-info__kicker">Cómo ser organizador</h2>
        <ol className="org-info__pasos">
          <li>
            <span className="org-info__num">1</span>
            <div>
              <strong>Creá tu cuenta</strong>
              <p>
                El registro es siempre de cliente.{' '}
                <button type="button" className="org-info__link" onClick={onCrearCuenta}>
                  Crear cuenta
                </button>
              </p>
            </div>
          </li>
          <li>
            <span className="org-info__num">2</span>
            <div>
              <strong>Activá el perfil de organizador</strong>
              <p>
                Un administrador te asigna el rol de organizador sobre tu cuenta de cliente.{' '}
                <Link className="org-info__link" to="/cuenta/organizador">
                  Ver guía
                </Link>
              </p>
            </div>
          </li>
          <li>
            <span className="org-info__num">3</span>
            <div>
              <strong>Gestioná tus eventos</strong>
              <p>Publicá eventos, mirá las ventas y administrá a tu equipo desde Gestión.</p>
            </div>
          </li>
        </ol>
        <p className="org-info__nota">
          Administración y personal de acceso no se registran acá: un admin de la plataforma
          tiene que darte de alta.
        </p>
      </section>

      <button type="button" className="org-info__link org-info__demo-link" onClick={onVerDemo}>
        Ver cuentas demo
      </button>
    </div>
  )
}
