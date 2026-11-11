import useAuth from '../../hooks/useAuth'
import { ROLE_LABELS } from '../../auth/roles'
import { FEATURE } from '../../config/featureGates'
import GateLink from '../../components/ui/GateLink'
import { Link } from 'react-router-dom'
import './GestionPage.css'

/**
 * Hub: org → eventos; admin → eventos + usuarios + ventas.
 * Acceso puerta/QR es solo PERSONAL_ACCESO (ruta /acceso), no el admin.
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
            <h2>Usuarios</h2>
            <p>
              Roles y estado de cuentas de la plataforma. Podés dar de alta
              personal de acceso (eso no es el panel de puerta/QR: ese es solo
              para quien tiene rol PERSONAL_ACCESO).
            </p>
            <GateLink
              enabled={FEATURE.adminUsuariosListado}
              className="btn btn-primary"
              to="/gestion/usuarios"
            >
              Administrar usuarios
            </GateLink>
          </article>
        )}

        {canManageStaff && (
          <article className="gestion-card gestion-card--ops">
            <h2>Lugares</h2>
            <p>
              Estadios, teatros y salas con sus sectores y su ubicación (dirección, latitud y
              longitud). Los organizadores eligen uno al crear un evento.
            </p>
            <GateLink
              enabled={FEATURE.lugares}
              className="btn btn-primary"
              to="/gestion/lugares"
            >
              Administrar lugares
            </GateLink>
          </article>
        )}

        {canManageStaff && (
          <article className="gestion-card gestion-card--ops">
            <h2>Participantes</h2>
            <p>
              Catálogo de bandas, artistas, elencos, equipos y pilotos. Editá nombres y tipos o
              eliminá los que no se usan; el principal y los teloneros se eligen por evento.
            </p>
            <GateLink
              enabled={FEATURE.participantes}
              className="btn btn-primary"
              to="/gestion/participantes"
            >
              Administrar participantes
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
