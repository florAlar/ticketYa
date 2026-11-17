import { Link, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import EventoLugarSectores from '../../components/gestion/EventoLugarSectores'
import EventoParticipantesFields from '../../components/gestion/EventoParticipantesFields'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import useEventoEditarForm from './evento/useEventoEditarForm'
import EventoDatosForm from './evento/EventoDatosForm'
import './GestionPage.css'

/**
 * Edición de evento en BORRADOR (PUT /api/eventos/{id}).
 * Borrador laxo; la publicación completa se hace desde el listado.
 */
const GestionEventoEditarPage = () => {
  const { eventoId } = useParams()
  const { user, token, isAdmin } = useAuth()

  const {
    form,
    sectores,
    teloneros,
    loaded,
    loadStatus,
    loadError,
    reload,
    status,
    error,
    gated,
    onChange,
    onRowChange,
    onElegirLugar,
    onPrincipalChange,
    onTeloneroChange,
    onAddTelonero,
    onRemoveTelonero,
    onSubmit,
  } = useEventoEditarForm({ eventoId, user, token, isAdmin })

  const loading = status === 'loading'

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> /{' '}
          <Link to="/gestion/eventos">eventos</Link> / editar
        </p>
        <h1 className="gestion-page__title">Editar borrador</h1>
        <p className="gestion-page__sub">
          Solo borrador. Podés dejar el lugar y los sectores sin elegir; para
          publicar desde el listado hace falta fecha, un lugar y al menos un
          sector con precio.
          {gated && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {!gated && loadStatus === 'loading' && (
        <LoadingState label="Cargando evento…" />
      )}

      {!gated && loadStatus === 'error' && (
        <ErrorState
          error={loadError}
          message={loadError?.message || 'No se pudo cargar el evento'}
          onRetry={reload}
        />
      )}

      {error && <p className="gestion-error">{error}</p>}

      {loaded?.status === 'blocked' && (
        <div className="gestion-card">
          <p className="gestion-error">{loaded.message}</p>
          <Link className="btn" to="/gestion/eventos">
            ← Volver al listado
          </Link>
        </div>
      )}

      {gated && (
        <div className="gestion-card">
          <p className="gestion-card__muted">{GATE_HINT}</p>
          <Link className="btn" to="/gestion/eventos">
            ← Volver al listado
          </Link>
        </div>
      )}

      {loaded?.status === 'ok' && form && (
        <form className="gestion-form gestion-card" onSubmit={onSubmit} noValidate>
          <EventoDatosForm form={form} loading={loading} onChange={onChange} />

          <EventoParticipantesFields
            principal={form.principal}
            onPrincipalChange={onPrincipalChange}
            teloneros={teloneros}
            onTeloneroChange={onTeloneroChange}
            onAddTelonero={onAddTelonero}
            onRemoveTelonero={onRemoveTelonero}
            token={token}
            disabled={loading}
          />

          <EventoLugarSectores
            token={token}
            idLugar={form.idLugar}
            onElegirLugar={onElegirLugar}
            rows={sectores}
            onRowChange={onRowChange}
            publishing={false}
            disabled={loading}
            isAdmin={isAdmin}
          />

          <div className="gestion-card__actions">
            <GateButton
              enabled={FEATURE.eventosGestion}
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Guardando…' : 'Guardar borrador'}
            </GateButton>
            <Link className="btn" to="/gestion/eventos">
              Cancelar
            </Link>
          </div>
        </form>
      )}
    </section>
  )
}

export default GestionEventoEditarPage
