import { Fragment, createElement } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  HiOutlineCalendar,
  HiOutlineClipboardList,
  HiOutlineCollection,
  HiOutlineExternalLink,
  HiOutlineHome,
  HiOutlineLocationMarker,
  HiOutlineUsers,
} from 'react-icons/hi'
import useAuth from '../../hooks/useAuth'
import { ROLE_LABELS } from '../../auth/roles'
import './GestionLayout.css'
import './gestionUi.css'

const SEGMENTOS = {
  gestion: 'Gestión',
  eventos: 'Eventos',
  nuevo: 'Nuevo',
  editar: 'Editar',
  reportes: 'Reportes',
  lugares: 'Lugares',
  participantes: 'Participantes',
  usuarios: 'Usuarios',
  ventas: 'Cola de ventas',
  entradas: 'Entrada',
}

/** Camino de migas a partir de la URL; los ids (uuid) no se muestran. */
function migas(pathname) {
  const partes = pathname.split('/').filter(Boolean)
  const out = []
  let acumulado = ''
  for (const p of partes) {
    acumulado += `/${p}`
    if (!SEGMENTOS[p]) continue
    out.push({ label: SEGMENTOS[p], to: acumulado })
  }
  return out
}

/**
 * Marco del panel de gestión: barra lateral + barra superior con camino de migas.
 * El organizador ve el mismo marco con menos secciones.
 */
export default function GestionLayout() {
  const { user, isAdmin, canManageEvents, canManageStaff, canManageSalesOps } = useAuth()
  const { pathname } = useLocation()
  const crumbs = migas(pathname)
  const rol = ROLE_LABELS[user?.rol] || user?.rol

  const items = [
    { to: '/gestion', label: 'Resumen', icon: HiOutlineHome, end: true, show: true },
    {
      to: '/gestion/eventos',
      label: isAdmin ? 'Eventos' : 'Mis eventos',
      icon: HiOutlineCalendar,
      show: canManageEvents,
    },
    { to: '/gestion/lugares', label: 'Lugares', icon: HiOutlineLocationMarker, show: isAdmin },
    { to: '/gestion/participantes', label: 'Participantes', icon: HiOutlineCollection, show: isAdmin },
    { to: '/gestion/usuarios', label: 'Usuarios', icon: HiOutlineUsers, show: canManageStaff },
    { to: '/gestion/ventas', label: 'Cola de ventas', icon: HiOutlineClipboardList, show: canManageSalesOps },
  ].filter((i) => i.show)

  return (
    <div className="gestion-shell">
      <aside className="gestion-shell__side" aria-label="Menú de gestión">
        <p className="gestion-shell__brand">{isAdmin ? 'Administración' : 'Organizador'}</p>
        <nav>
          <ul>
            {items.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.end} className="gestion-shell__link">
                  {createElement(item.icon, { "aria-hidden": true })} <span>{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="gestion-shell__foot">
          <Link to="/eventos" className="gestion-shell__link gestion-shell__link--ghost">
            <HiOutlineExternalLink aria-hidden /> <span>Ver catálogo público</span>
          </Link>
          <p className="gestion-shell__user">
            <strong>{user?.nombre || user?.email}</strong>
            <span>{rol}</span>
          </p>
        </div>
      </aside>

      <div className="gestion-shell__body">
        <nav className="gestion-shell__crumbs" aria-label="Ubicación">
          {crumbs.map((c, i) => (
            <Fragment key={c.to}>
              {i > 0 && <span aria-hidden>›</span>}
              {i === crumbs.length - 1 ? (
                <strong aria-current="page">{c.label}</strong>
              ) : (
                <Link to={c.to}>{c.label}</Link>
              )}
            </Fragment>
          ))}
        </nav>
        <div className="gestion-shell__content">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
