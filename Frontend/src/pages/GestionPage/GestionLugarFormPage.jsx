import { Link, useParams } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import useLugarForm from './lugar/useLugarForm'
import LugarDatosForm from './lugar/LugarDatosForm'
import LugarSectoresPanel from './lugar/LugarSectoresPanel'
import './GestionPage.css'

/**
 * Alta y edición de un lugar (LUG-01) — solo ADMIN. En edición muestra además
 * los sectores del lugar (alta y edición; los sectores no se eliminan).
 * Ruta: /gestion/lugares/nuevo | /:lugarId.
 */
const GestionLugarFormPage = () => {
  const { lugarId } = useParams()
  const esNuevo = !lugarId
  const { token } = useAuth()
  const wired = FEATURE.lugares

  const {
    form,
    lugar,
    status,
    actionError,
    loadError,
    aviso,
    saving,
    geocoding,
    editando,
    sectorForm,
    sectorError,
    sectorSaving,
    onChange,
    buscarCoordenadas,
    onSubmit,
    abrirSector,
    onSectorChange,
    cerrarSector,
    guardarSector,
    reload,
  } = useLugarForm({ lugarId, esNuevo, wired, token })

  const titulo = esNuevo ? 'Nuevo lugar' : lugar?.nombre || 'Editar lugar'

  return (
    <section className="gestion-page container-slim section-v-padding">
      <header className="gestion-page__head">
        <p className="gestion-page__eyebrow">
          <Link to="/gestion">Gestión</Link> /{' '}
          <Link to="/gestion/lugares">lugares</Link> /{' '}
          {esNuevo ? 'nuevo' : 'editar'}
        </p>
        <h1 className="gestion-page__title">{titulo}</h1>
        <p className="gestion-page__sub">
          Nombre, ciudad y dirección; la ubicación (latitud y longitud) es
          opcional.
          {!wired && (
            <>
              {' '}
              <strong title={GATE_HINT}>({GATE_HINT})</strong>
            </>
          )}
        </p>
      </header>

      {status === 'loading' && <LoadingState label="Cargando lugar…" />}
      {status === 'error' && <ErrorState message={loadError} onRetry={reload} />}

      {status === 'success' && (
        <LugarDatosForm
          form={form}
          wired={wired}
          saving={saving}
          geocoding={geocoding}
          error={actionError}
          aviso={aviso}
          esNuevo={esNuevo}
          onChange={onChange}
          onSubmit={onSubmit}
          buscarCoordenadas={buscarCoordenadas}
        />
      )}

      {status === 'success' && !esNuevo && lugar && (
        <LugarSectoresPanel
          lugar={lugar}
          wired={wired}
          editando={editando}
          sectorForm={sectorForm}
          sectorError={sectorError}
          sectorSaving={sectorSaving}
          onAbrirSector={abrirSector}
          onSectorChange={onSectorChange}
          onGuardarSector={guardarSector}
          onCerrarSector={cerrarSector}
        />
      )}
    </section>
  )
}

export default GestionLugarFormPage
