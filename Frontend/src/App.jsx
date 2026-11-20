import Layout from './components/layout/Layout'
import AppRoutes from './routes/AppRoutes'
import ErrorBoundary from './components/ui/ErrorBoundary'

import ThemeProvider from './providers/ThemeProvider'
import SiteProvider from './providers/SiteProvider'
import AuthProvider from './providers/AuthProvider'
import ConfirmProvider from './components/ui/ConfirmProvider'
import AppToaster from './components/ui/AppToaster'

/**
 * Árbol de app TicketYa (cliente → gateway :8080).
 * Site = brand/chrome; Theme = tokens CSS; Auth = sesión/roles.
 * Rutas solo en routes/AppRoutes.jsx (ver docs/endpoints-front.md).
 */
function App() {
  return (
    <ErrorBoundary>
      <SiteProvider>
        <ThemeProvider>
          <AuthProvider>
            <ConfirmProvider>
              <Layout>
                <AppRoutes />
              </Layout>
            </ConfirmProvider>
            <AppToaster />
          </AuthProvider>
        </ThemeProvider>
      </SiteProvider>
    </ErrorBoundary>
  )
}

export default App
