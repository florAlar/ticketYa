import { Component } from 'react'
import './AsyncState.css'

/**
 * Red de seguridad de render: evita que una excepción deje la app en blanco.
 * No captura errores de eventos ni de promesas (para eso está el manejo de API).
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    // Punto único para conectar Sentry/telemetría en el futuro.
    if (import.meta.env.DEV) {
      console.error('[ErrorBoundary]', error, info?.componentStack)
    }
  }

  handleReset = () => {
    this.setState({ error: null })
  }

  render() {
    const { error } = this.state
    if (!error) return this.props.children

    return (
      <section className="async-state async-state--error" role="alert">
        <h1 className="async-state__title">Algo salió mal</h1>
        <p className="async-state__text">
          La pantalla no pudo renderizarse. Podés reintentar o recargar la
          página.
        </p>
        {import.meta.env.DEV && error?.message ? (
          <pre className="async-state__detail">{error.message}</pre>
        ) : null}
        <div className="async-state__actions">
          <button type="button" className="btn btn-primary" onClick={this.handleReset}>
            Reintentar
          </button>
          <button
            type="button"
            className="btn"
            onClick={() => window.location.reload()}
          >
            Recargar
          </button>
        </div>
      </section>
    )
  }
}
