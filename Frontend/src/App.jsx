import Layout from './components/layout/Layout'
import AppRoutes from './routes/AppRoutes'

import ThemeProvider from './providers/ThemeProvider'
import SiteProvider from './providers/SiteProvider'
import AuthProvider from './providers/AuthProvider'
import './styles/global.css'

/**
 * Árbol de app TicketYa (cliente → gateway :8080).
 * Site = brand/chrome; Theme = tokens CSS; Auth = sesión/roles.
 * Rutas solo en routes/AppRoutes.jsx (ver docs/endpoints-front.md).
 */
function App() {
  return (
    <SiteProvider>
      <ThemeProvider>
        <AuthProvider>
          <Layout>
            <AppRoutes />
          </Layout>
        </AuthProvider>
      </ThemeProvider>
    </SiteProvider>
  )
}

export default App
