import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import EventoLugarSectores from '../../components/gestion/EventoLugarSectores'
import EventoParticipantesFields from '../../components/gestion/EventoParticipantesFields'
import useEventoNuevoForm from './evento/useEventoNuevoForm'
import EventoNuevoDatosForm from './evento/EventoNuevoDatosForm'
import './GestionPage.css'

const GestionEventoNuevoPage = () => {
  const { user, token, isAdmin } = useAuth()
  const {
    form,
    sectores,
    teloneros,
    error,
    loading,
    publishing,
    gated,
    onChange,
    onRowChange,
    onElegirLugar,
    onPrincipalChange,
    onTeloneroChange,
    onAddTelonero,
    onRemoveTelonero,
    onSubmit,
  } = useEventoNuevoForm({ user, token })

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> /{' '}
          <Link to="/gestion/eventos">eventos</Link> / nuevo
        </p>
        <h1 className="gestion-page__title">Crear evento</h1>
        <p className="gestion-page__sub">
          Borrador: alcanza con el nombre (el resto se puede completar después).
          Publicar: fecha, un lugar y al menos un sector del lugar con su precio.
          {isAdmin ? ' · Alta como admin.' : ` · org: ${user?.id}`}
          {gated && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {gated ? (
        <div className="gestion-card">
          <p>
            Alta de eventos no disponible: backend de eventos en mock o no
            cableado.
          </p>
          <Link className="btn" to="/gestion/eventos">
            ← Volver al listado
          </Link>
        </div>
      ) : (
        <form className="gestion-form gestion-card" onSubmit={onSubmit} noValidate>
          <EventoNuevoDatosForm
            form={form}
            loading={loading}
            publishing={publishing}
            onChange={onChange}
          />

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
            publishing={publishing}
            disabled={loading}
            isAdmin={isAdmin}
          />

          {error && <p className="gestion-error">{error}</p>}

          <div className="gestion-card__actions">
            <GateButton
              enabled={FEATURE.eventosGestion}
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading
                ? 'Guardando…'
                : publishing
                  ? 'Crear y publicar'
                  : 'Guardar borrador'}
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

export default GestionEventoNuevoPage
