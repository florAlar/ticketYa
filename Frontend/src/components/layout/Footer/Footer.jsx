import React from 'react'
import { Link } from 'react-router-dom'
import useSite from '../../../hooks/useSite'
import useAuth from '../../../hooks/useAuth'
import '../../../styles/global.css'

const Footer = () => {
  const { siteInfo } = useSite()
  const { isAuthenticated, isAcceso } = useAuth()

  const routes = [
    { to: '/', label: 'Inicio' },
    ...(isAuthenticated && !isAcceso
      ? [{ to: '/eventos', label: 'Eventos' }]
      : []),
    ...(!isAcceso ? [{ to: '/mis-entradas', label: 'Mis entradas' }] : []),
    { to: '/soporte', label: 'Soporte' },
    ...(isAuthenticated
      ? [{ to: '/perfil', label: 'Mi perfil' }]
      : [{ to: '/login', label: 'Ingresar' }]),
  ]

  return (
    <footer className="site-footer">
      <div className="footer-content container">
        <div className="footer-left">
          <p className="footer-love">{siteInfo.footer.copyright}</p>
          {siteInfo.footer.madeWith && (
            <p className="footer-copy">{siteInfo.footer.madeWith}</p>
          )}
        </div>

        <nav className="footer-right" aria-label="Rutas">
          <ul className="footer-links">
            {routes.map((route, i) => (
              <React.Fragment key={route.to}>
                {i > 0 && <li className="separator" aria-hidden>•</li>}
                <li>
                  <Link to={route.to}>{route.label}</Link>
                </li>
              </React.Fragment>
            ))}
          </ul>
        </nav>
      </div>
    </footer>
  )
}

export default Footer
