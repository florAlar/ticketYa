import { tipoNotificacionLabel } from '../../domain/notificacion'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { useNotificacionesPrefs } from './useNotificaciones'

/** Canales push/email de la bandeja de notificaciones. */
export default function NotificacionesPrefs({ token, enabled }) {
  const { prefs, status, error, msg, saving, toggle, reload } =
    useNotificacionesPrefs({ token, enabled })

  return (
    <section className="notificaciones-prefs" aria-label="Preferencias de notificación">
      <div className="notificaciones-prefs__copy">
        <h2>Canales</h2>
        <p>
          Elegí cómo querés recibir los avisos. El push es simulado en esta
          demo; el email se envía de verdad a MailHog.
        </p>
        {prefs?.tipos?.length > 0 && (
          <p className="notificaciones-prefs__tipos">
            Tipos habilitados: {prefs.tipos.map(tipoNotificacionLabel).join(', ')}
          </p>
        )}
        {status === 'loading' && (
          <LoadingState label="Cargando preferencias…" />
        )}
        {status === 'error' && <ErrorState message={error} onRetry={reload} />}
        {msg && <p className="notificaciones-page__ok">{msg}</p>}
      </div>

      <div className="notificaciones-prefs__channels">
        <label className="notificaciones-prefs__toggle">
          <input
            type="checkbox"
            checked={Boolean(prefs?.push)}
            disabled={status !== 'success' || saving}
            onChange={() => toggle('push')}
          />
          <span>
            <strong>Push</strong>
            <small>Aviso dentro de la app</small>
          </span>
        </label>
        <label className="notificaciones-prefs__toggle">
          <input
            type="checkbox"
            checked={Boolean(prefs?.email)}
            disabled={status !== 'success' || saving}
            onChange={() => toggle('email')}
          />
          <span>
            <strong>Email</strong>
            <small>Correo a tu cuenta</small>
          </span>
        </label>
      </div>
    </section>
  )
}
