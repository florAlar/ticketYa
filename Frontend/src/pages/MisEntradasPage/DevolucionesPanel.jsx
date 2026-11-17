import { formatFecha, formatMoney } from '../../lib/format'
import {
  DEVOLUCION_ESTADO_LABELS,
  DEVOLUCION_TIPO_LABELS,
  DEVOLUCION_TIPOS,
  isDevolucionPorCancelacion,
} from '../../domain/devolucion'

/** Trámites de devolución/reintegro del cliente. */
export default function DevolucionesPanel({ solicitudes }) {
  return (
    <section className="mis-entradas-section" id="mis-devoluciones">
      <h2 className="mis-entradas-page__title mis-entradas-page__title--sub">
        Mis devoluciones y reintegros
      </h2>
      <p className="mis-entradas-page__muted">
        Todos los trámites de dinero en un solo lugar (voluntarios y por
        cancelación de evento). No son tickets de ingreso.
      </p>
      {solicitudes.length === 0 ? (
        <p className="mis-entradas-page__muted">
          Todavía no tenés devoluciones ni reintegros.
        </p>
      ) : (
        <ul className="mis-entradas-list">
          {solicitudes.map((s) => {
            const porCancel = isDevolucionPorCancelacion(s)
            return (
              <li
                key={s.id}
                className={
                  porCancel
                    ? 'mis-entradas-card mis-entradas-card--cancelada'
                    : 'mis-entradas-card'
                }
                id={`tramite-${s.id}`}
              >
                <div className="mis-entradas-card__body">
                  <h2 className="mis-entradas-page__title--sub">
                    {DEVOLUCION_TIPO_LABELS[s.tipo] || s.tipo || 'Devolución'}
                    {' · '}
                    {s.eventoNombre || 'Evento'}
                  </h2>
                  <p className="mis-entradas-page__muted">
                    N° trámite:{' '}
                    <strong className="mis-entradas-page__mono">{s.id}</strong>
                  </p>
                  <p className="mis-entradas-page__muted">
                    {formatFecha(s.fechaSolicitud)} ·{' '}
                    <strong>
                      {DEVOLUCION_ESTADO_LABELS[s.estado] || s.estado}
                    </strong>
                    {s.monto != null ? ` · ${formatMoney(s.monto)}` : ''}
                  </p>
                  {s.motivo ? <p>{s.motivo}</p> : null}
                  {porCancel && (
                    <p className="mis-entradas-page__aviso">
                      Te vamos a informar cuando el pago haya devuelto el dinero.
                    </p>
                  )}
                  {s.idEntrada ? (
                    <p className="mis-entradas-page__muted">
                      Ref. entrada: {s.idEntrada}
                      {s.tipo === DEVOLUCION_TIPOS.CANCELACION_EVENTO || porCancel
                        ? ' · origen: cancelación de evento'
                        : ' · origen: solicitud tuya'}
                    </p>
                  ) : null}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
