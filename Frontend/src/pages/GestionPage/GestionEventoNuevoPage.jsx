import useAuth from '../../hooks/useAuth'
import { GATE_HINT } from '../../config/featureGates'
import { PageHeader, VacioTabla } from '../../components/gestion/gestionUi'
import { LoadingState } from '../../components/ui/AsyncState'
import useEventoNuevoForm from './evento/useEventoNuevoForm'
import EventoWizard from './evento/wizard/EventoWizard'
import './GestionPage.css'

/** Alta de evento en 5 pasos, con vista previa, borrador local y opción de duplicar otro evento. */
const GestionEventoNuevoPage = () => {
  const { user, token, isAdmin } = useAuth()
  const w = useEventoNuevoForm({ user, token })

  return (
    <>
      <PageHeader
        title="Crear evento"
        subtitle={
          w.duplicando
            ? 'Copiando los datos del evento original…'
            : 'Completá los pasos. Podés guardar un borrador en cualquier momento y terminarlo después.'
        }
      />

      {w.gated ? (
        <VacioTabla>
          Alta de eventos no disponible: backend de eventos en mock o no cableado. {GATE_HINT}
        </VacioTabla>
      ) : w.duplicando ? (
        <LoadingState label="Copiando evento…" />
      ) : (
        <>
          {w.borradorPendiente && (
            <div className="wz-borrador" role="status">
              <span>
                Tenés un evento sin terminar
                {w.borradorPendiente.form?.nombre ? `: «${w.borradorPendiente.form.nombre}»` : ''}. ¿Querés seguir donde lo dejaste?
              </span>
              <button type="button" className="btn btn-primary btn-sm" onClick={w.restaurarBorrador}>
                Continuar
              </button>
              <button type="button" className="btn btn-secondary btn-sm" onClick={w.descartarBorrador}>
                Descartar
              </button>
            </div>
          )}
          <EventoWizard w={w} token={token} isAdmin={isAdmin} />
        </>
      )}
    </>
  )
}

export default GestionEventoNuevoPage
