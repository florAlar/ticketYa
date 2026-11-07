import useAuth from '../../hooks/useAuth'
import { ROLE_LABELS } from '../../auth/roles'
import { FEATURE } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import { Link } from 'react-router-dom'
import './GestionPage.css'

/**
 * Hub: org → eventos; admin → eventos + usuarios/acceso + ventas.
 * CTAs no cableados al BE: agrisados (flujos mock/shell intactos en el repo).
 */
const GestionPage = () => {
  const {
    user,
    isAdmin,
    canManageEvents,
    canManageStaff,
    canManageSalesOps,
  } = useAuth()
  const rolLabel = ROLE_LABELS[user?.rol] || user?.rol

  return (
    <section className="gestion-page container section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">Panel de gestión</p>
        <h1 className="gestion-page__title">
          {isAdmin ? 'Administración' : 'Mis eventos'}
        </h1>
        <p className="gestion-page__sub">
          Sesión: <strong>{user?.nombre || user?.email}</strong>
          {rolLabel ? ` · ${rolLabel}` : ''}
        </p>
      </header>

      <div className="gestion-page__grid">
        {canManageEvents && (
          <>
            <article className="gestion-card">
              <h2>{isAdmin ? 'Todos los eventos' : 'Mis eventos'}</h2>
              <p>
                {isAdmin
                  ? 'Listado completo (gateway → msvc-eventos).'
                  : 'Solo eventos donde sos el organizador.'}
              </p>
              <GateLink
                enabled={FEATURE.eventosGestion}
                className="btn btn-primary"
                to="/gestion/eventos"
              >
                Ver listado
              </GateLink>
            </article>

            <article className="gestion-card">
              <h2>Crear evento</h2>
              <p>Alta con forma de entidad de referencia.</p>
              <GateLink
                enabled={FEATURE.eventosGestion}
                className="btn btn-primary"
                to="/gestion/eventos/nuevo"
              >
                Nuevo evento
              </GateLink>
            </article>
          </>
        )}

        {canManageStaff && (
          <article className="gestion-card gestion-card--ops">
            <h2>Usuarios y acceso</h2>
            <p>
              Rol y activo (no datos personales). Alta de personal de acceso
              asignado a eventos. ACCESO no se desactiva: se elimina.
            </p>
            <GateLink
              enabled={FEATURE.adminUsuarios}
              className="btn btn-primary"
              to="/gestion/usuarios"
            >
              Administrar usuarios
            </GateLink>
          </article>
        )}

        {canManageSalesOps && (
          <article className="gestion-card gestion-card--ops">
            <h2>Ventas / transacciones</h2>
            <p>
              Cola: devoluciones/reclamos pendientes y entradas de eventos
              cancelados (no todas las compras).
            </p>
            <GateLink
              enabled={FEATURE.adminColaVentas}
              className="btn btn-primary"
              to="/gestion/ventas"
            >
              Ver transacciones
            </GateLink>
          </article>
        )}
      </div>

      <p className="gestion-page__foot">
        <Link to="/">← Volver al inicio</Link>
      </p>
    </section>
  )
}

export default GestionPage
