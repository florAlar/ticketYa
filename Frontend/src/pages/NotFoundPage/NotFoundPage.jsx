import { Link } from 'react-router-dom'

const NotFoundPage = () => {
  return (
    <section className="container-slim section-v-padding">
      <h1>404</h1>
      <p>Página no encontrada.</p>
      <p>
        <Link to="/">Ir al inicio</Link>
      </p>
    </section>
  )
}

export default NotFoundPage
