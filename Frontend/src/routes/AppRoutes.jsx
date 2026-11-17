import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'

import ProtectedRoute from './ProtectedRoute'
import RouteFallback from '../components/layout/RouteFallback'
import { ROLES } from '../auth/roles'

// Rutas con code splitting: cada pantalla pesada (gestión, checkout, acceso)
// se descarga recién cuando se navega a ella.
const Home = lazy(() => import('../pages/HomePage/Home'))
const EventosPage = lazy(() => import('../pages/EventosPage/EventosPage'))
const EventoDetallePage = lazy(() => import('../pages/EventoDetallePage/EventoDetallePage'))
const CheckoutPage = lazy(() => import('../pages/CheckoutPage/CheckoutPage'))
const MisEntradasPage = lazy(() => import('../pages/MisEntradasPage/MisEntradasPage'))
const EntradaDetallePage = lazy(() => import('../pages/EntradaDetallePage/EntradaDetallePage'))
const LoginPage = lazy(() => import('../pages/LoginPage/LogInPage'))
const SoportePage = lazy(() => import('../pages/SoportePage/SoportePage'))
const DevolucionPage = lazy(() => import('../pages/SoportePage/DevolucionPage'))
const TransferenciaPage = lazy(() => import('../pages/SoportePage/TransferenciaPage'))
const GestionPage = lazy(() => import('../pages/GestionPage/GestionPage'))
const GestionEventosPage = lazy(() => import('../pages/GestionPage/GestionEventosPage'))
const GestionEventoNuevoPage = lazy(() => import('../pages/GestionPage/GestionEventoNuevoPage'))
const GestionEventoEditarPage = lazy(() => import('../pages/GestionPage/GestionEventoEditarPage'))
const GestionEventoReportesPage = lazy(() => import('../pages/GestionPage/GestionEventoReportesPage'))
const GestionUsuariosPage = lazy(() => import('../pages/GestionPage/GestionUsuariosPage'))
const GestionLugaresPage = lazy(() => import('../pages/GestionPage/GestionLugaresPage'))
const GestionLugarFormPage = lazy(() => import('../pages/GestionPage/GestionLugarFormPage'))
const GestionParticipantesPage = lazy(() => import('../pages/GestionPage/GestionParticipantesPage'))
const OrganizerAccountPage = lazy(() => import('../pages/CuentaRolesPage/OrganizerAccountPage'))
const PerfilPage = lazy(() => import('../pages/PerfilPage/PerfilPage'))
const GestionVentasPage = lazy(() => import('../pages/GestionPage/GestionVentasPage'))
const GestionEntradaDetallePage = lazy(() => import('../pages/GestionPage/GestionEntradaDetallePage'))
const AccesoPage = lazy(() => import('../pages/AccesoPage/AccesoPage'))
const AccesoScanPage = lazy(() => import('../pages/AccesoPage/AccesoScanPage'))
const NotificacionesPage = lazy(() => import('../pages/NotificacionesPage/NotificacionesPage'))
const ForbiddenPage = lazy(() => import('../pages/ForbiddenPage/ForbiddenPage'))
const NotFoundPage = lazy(() => import('../pages/NotFoundPage/NotFoundPage'))

const GESTION_ROLES = [ROLES.ADMIN, ROLES.ORGANIZADOR]

/** Configuración única de rutas: path + elemento + auth/roles. */
const ROUTES = [
  { path: '/', element: <Home /> },
  { path: '/eventos', element: <EventosPage />, auth: true },
  { path: '/eventos/:eventoId', element: <EventoDetallePage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/cuenta/organizador', element: <OrganizerAccountPage /> },
  { path: '/soporte', element: <SoportePage /> },
  { path: '/sin-permiso', element: <ForbiddenPage /> },

  { path: '/eventos/:eventoId/comprar', element: <CheckoutPage />, auth: true },
  { path: '/perfil', element: <PerfilPage />, auth: true },
  { path: '/mis-entradas', element: <MisEntradasPage />, auth: true },
  { path: '/notificaciones', element: <NotificacionesPage />, auth: true },
  { path: '/mis-entradas/:entradaId', element: <EntradaDetallePage />, auth: true },
  { path: '/soporte/devolucion', element: <DevolucionPage />, auth: true },
  { path: '/soporte/transferencia', element: <TransferenciaPage />, auth: true },

  { path: '/gestion', element: <GestionPage />, auth: true, roles: GESTION_ROLES },
  { path: '/gestion/eventos', element: <GestionEventosPage />, auth: true, roles: GESTION_ROLES },
  { path: '/gestion/eventos/nuevo', element: <GestionEventoNuevoPage />, auth: true, roles: GESTION_ROLES },
  { path: '/gestion/eventos/:eventoId/editar', element: <GestionEventoEditarPage />, auth: true, roles: GESTION_ROLES },
  { path: '/gestion/eventos/:eventoId/reportes', element: <GestionEventoReportesPage />, auth: true, roles: GESTION_ROLES },
  { path: '/gestion/lugares', element: <GestionLugaresPage />, auth: true, roles: [ROLES.ADMIN] },
  { path: '/gestion/lugares/nuevo', element: <GestionLugarFormPage />, auth: true, roles: [ROLES.ADMIN] },
  { path: '/gestion/lugares/:lugarId', element: <GestionLugarFormPage />, auth: true, roles: [ROLES.ADMIN] },
  { path: '/gestion/participantes', element: <GestionParticipantesPage />, auth: true, roles: [ROLES.ADMIN] },
  { path: '/gestion/usuarios', element: <GestionUsuariosPage />, auth: true, roles: [ROLES.ADMIN] },
  { path: '/gestion/ventas', element: <GestionVentasPage />, auth: true, roles: [ROLES.ADMIN] },
  { path: '/gestion/entradas/:entradaId', element: <GestionEntradaDetallePage />, auth: true, roles: [ROLES.ADMIN] },

  { path: '/acceso', element: <AccesoPage />, auth: true, roles: [ROLES.ACCESO] },
  { path: '/acceso/scan', element: <AccesoScanPage />, auth: true, roles: [ROLES.ACCESO] },
]

/**
 * Mapa front cliente TicketYa → API Gateway :8080
 * Ver docs/endpoints-front.md
 */
const AppRoutes = () => {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        {ROUTES.map(({ path, element, auth, roles }) => (
          <Route
            key={path}
            path={path}
            element={
              auth ? <ProtectedRoute roles={roles}>{element}</ProtectedRoute> : element
            }
          />
        ))}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}

export default AppRoutes
