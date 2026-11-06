import React from 'react'
import { Link } from 'react-router-dom'
import useSite from '../../../hooks/useSite'
import '../../../styles/global.css'

const FOOTER_ROUTES = [
  { to: '/', label: 'Inicio' },
  { to: '/eventos', label: 'Eventos' },
  { to: '/mis-entradas', label: 'Mis entradas' },
  { to: '/soporte', label: 'Soporte' },
  { to: '/login', label: 'Ingresar' },
]

const Footer = () => {
  const { siteInfo } = useSite()
  return (
    <footer className="site-footer">
      <div className="footer-content container">
        <div className="footer-left">
          <p className="footer-love">
            {siteInfo.footer.copyright}
          </p>
          {siteInfo.footer.madeWith && (
            <p className="footer-copy">{siteInfo.footer.madeWith}</p>
          )}
        </div>

        <nav className="footer-right" aria-label="Rutas">
          <ul className="footer-links">
            {FOOTER_ROUTES.map((route, i) => (
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
