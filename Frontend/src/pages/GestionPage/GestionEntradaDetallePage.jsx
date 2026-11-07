import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { getEntrada } from '../../api/entradas'
import { ENTRADA_ESTADO_LABELS } from '../../domain/entrada'
import '../MisEntradasPage/MisEntradasPage.css'
import './GestionPage.css'

function formatFecha(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

/**
 * Detalle de entrada para ADMIN (cola ventas / ops).
 * Mismo GET /api/entradas/{id} (BE: dueño o ADMIN). No es "mis entradas".
 */
const GestionEntradaDetallePage = () => {
  const { entradaId } = useParams()
  const { user, token } = useAuth()
  const [entrada, setEntrada] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const ent = await getEntrada(entradaId, user, { token })
      setEntrada(ent)
      setStatus('ok')
    } catch (err) {
      setError(err.message || 'No se pudo cargar la entrada')
      setStatus('error')
    }
  }, [entradaId, user, token])

  useEffect(() => {
    load()
  }, [load])

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

      {status === 'loading' && (
        <p className="mis-entradas-page__muted">Cargando…</p>
      )}
      {error && <p className="mis-entradas-page__error">{error}</p>}

      {status === 'ok' && entrada && (
        <div
          className="mis-entradas-card"
          style={{ flexDirection: 'column', alignItems: 'stretch' }}
        >
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
          <div
            className="entrada-qr-box"
            style={{
              marginTop: '1rem',
              padding: '1.25rem',
              borderRadius: 12,
              border: '1px dashed var(--colors-border, #ccc)',
              textAlign: 'center',
              fontFamily: 'ui-monospace, monospace',
              wordBreak: 'break-all',
            }}
          >
            <p style={{ margin: '0 0 0.5rem', fontSize: '0.8rem', opacity: 0.7 }}>
              Código QR
            </p>
            <p style={{ margin: 0, fontWeight: 600 }}>{entrada.codigoQR}</p>
          </div>
        </div>
      )}
    </section>
  )
}

export default GestionEntradaDetallePage
