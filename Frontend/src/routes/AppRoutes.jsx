import { Routes, Route } from 'react-router-dom'

import Home from '../pages/HomePage/Home'
import EventosPage from '../pages/EventosPage/EventosPage'
import EventoDetallePage from '../pages/EventoDetallePage/EventoDetallePage'
import CheckoutPage from '../pages/CheckoutPage/CheckoutPage'
import MisEntradasPage from '../pages/MisEntradasPage/MisEntradasPage'
import EntradaDetallePage from '../pages/EntradaDetallePage/EntradaDetallePage'
import LoginPage from '../pages/LoginPage/LogInPage'
import SoportePage from '../pages/SoportePage/SoportePage'
import DevolucionPage from '../pages/SoportePage/DevolucionPage'
import TransferenciaPage from '../pages/SoportePage/TransferenciaPage'
import GestionPage from '../pages/GestionPage/GestionPage'
import GestionEventosPage from '../pages/GestionPage/GestionEventosPage'
import GestionEventoNuevoPage from '../pages/GestionPage/GestionEventoNuevoPage'
import GestionEventoEditarPage from '../pages/GestionPage/GestionEventoEditarPage'
import GestionUsuariosPage from '../pages/GestionPage/GestionUsuariosPage'
import OrganizerAccountPage from '../pages/CuentaRolesPage/OrganizerAccountPage'
import GestionVentasPage from '../pages/GestionPage/GestionVentasPage'
import GestionEntradaDetallePage from '../pages/GestionPage/GestionEntradaDetallePage'
import AccesoPage from '../pages/AccesoPage/AccesoPage'
import AccesoScanPage from '../pages/AccesoPage/AccesoScanPage'
import ForbiddenPage from '../pages/ForbiddenPage/ForbiddenPage'
import NotFoundPage from '../pages/NotFoundPage/NotFoundPage'
import ProtectedRoute from './ProtectedRoute'
import { ROLES } from '../auth/roles'

/**
 * Mapa front cliente TicketYa → API Gateway :8080
 * Ver docs/endpoints-front.md
 */
const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/eventos" element={<EventosPage />} />
      <Route path="/eventos/:eventoId" element={<EventoDetallePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cuenta/organizador" element={<OrganizerAccountPage />} />
      <Route path="/soporte" element={<SoportePage />} />
      <Route path="/sin-permiso" element={<ForbiddenPage />} />

      <Route
        path="/eventos/:eventoId/comprar"
        element={
          <ProtectedRoute>
            <CheckoutPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/mis-entradas"
        element={
          <ProtectedRoute>
            <MisEntradasPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/mis-entradas/:entradaId"
        element={
          <ProtectedRoute>
            <EntradaDetallePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/soporte/devolucion"
        element={
          <ProtectedRoute>
            <DevolucionPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/soporte/transferencia"
        element={
          <ProtectedRoute>
            <TransferenciaPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/gestion"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN, ROLES.ORGANIZADOR]}>
            <GestionPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/gestion/eventos"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN, ROLES.ORGANIZADOR]}>
            <GestionEventosPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/gestion/eventos/nuevo"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN, ROLES.ORGANIZADOR]}>
            <GestionEventoNuevoPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/gestion/eventos/:eventoId/editar"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN, ROLES.ORGANIZADOR]}>
            <GestionEventoEditarPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/gestion/usuarios"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN]}>
            <GestionUsuariosPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/gestion/ventas"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN]}>
            <GestionVentasPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/gestion/entradas/:entradaId"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN]}>
            <GestionEntradaDetallePage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/acceso"
        element={
          <ProtectedRoute roles={[ROLES.ACCESO, ROLES.ADMIN]}>
            <AccesoPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/acceso/scan"
        element={
          <ProtectedRoute roles={[ROLES.ACCESO, ROLES.ADMIN]}>
            <AccesoScanPage />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default AppRoutes
