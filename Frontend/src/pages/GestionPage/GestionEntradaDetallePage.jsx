import { useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useAsync from '../../hooks/useAsync'
import { getEntrada } from '../../api/entradas'
import { ENTRADA_ESTADO_LABELS } from '../../domain/entrada'
import { formatFecha } from '../../lib/format'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import '../MisEntradasPage/MisEntradasPage.css'
import './GestionPage.css'

/**
 * Detalle de entrada para ADMIN (cola ventas / ops).
 * Mismo GET /api/entradas/{id} (BE: dueño o ADMIN). No es "mis entradas".
 */
const GestionEntradaDetallePage = () => {
  const { entradaId } = useParams()
  const { user, token } = useAuth()

  const load = useCallback(
    (signal) => getEntrada(entradaId, user, { token, signal }),
    [entradaId, user, token]
  )

  const { data: entrada, status, error, reload } = useAsync(
    load,
    [entradaId, user?.id, token],
    { enabled: Boolean(user?.id && token) }
  )

  return (
    <section className="mis-entradas-page container-slim section-v-padding">
      <header className="mis-entradas-page__head">
        <p className="mis-entradas-page__muted">
          <Link to="/gestion/ventas">← Cola de ventas</Link>
          {' · '}
          <Link to="/gestion">Gestión</Link>
        </p>
        <h1 className="mis-entradas-page__title">
          Entrada (admin)
        </h1>
        <p className="mis-entradas-page__sub">
          GET /api/entradas/&#123;id&#125; con rol ADMIN — no es el listado del cliente.
        </p>
      </header>

      {status === 'loading' && <LoadingState label="Cargando entrada…" />}
      {status === 'error' && <ErrorState error={error} onRetry={reload} />}

      {status === 'success' && entrada && (
        <div className="mis-entradas-card mis-entradas-card--stack">
          <p>
            <strong>Estado:</strong>{' '}
            {ENTRADA_ESTADO_LABELS[entrada.estado] || entrada.estado}
          </p>
          <p>
            <strong>Evento:</strong> {entrada.eventoNombre || entrada.idEvento}
          </p>
          <p>
            <strong>Fecha evento:</strong> {formatFecha(entrada.eventoFecha)}
          </p>
          <p>
            <strong>Lugar:</strong>{' '}
            {[entrada.lugarNombre, entrada.lugarCiudad]
              .filter(Boolean)
              .join(' · ') || '—'}
          </p>
          <p>
            <strong>Sector:</strong> {entrada.sectorNombre || entrada.idSector}
          </p>
          <p>
            <strong>Cliente:</strong> {entrada.idCliente}
          </p>
          <p>
            <strong>Compra:</strong> {entrada.idCompra}
          </p>
          <p>
            <strong>ID entrada:</strong> {entrada.id}
          </p>
          <p>
            <strong>Emisión:</strong> {formatFecha(entrada.fechaEmision)}
          </p>
          <div className="entrada-qr-box">
            <p className="entrada-qr-box__label">Código QR</p>
            <p className="entrada-qr-box__code">{entrada.codigoQR}</p>
          </div>
        </div>
      )}
    </section>
  )
}

export default GestionEntradaDetallePage
