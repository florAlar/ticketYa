import { Link } from 'react-router-dom'
import GateButton from '../../../components/ui/GateButton'

/** Datos básicos del lugar + geocodificación. */
export default function LugarDatosForm({
  form,
  wired,
  saving,
  geocoding,
  error,
  aviso,
  esNuevo,
  onChange,
  onSubmit,
  buscarCoordenadas,
}) {
  return (
    <form className="gestion-card gestion-form" onSubmit={onSubmit}>
      <div className="form-group">
        <label htmlFor="nombre">Nombre</label>
        <input
          id="nombre"
          name="nombre"
          className="form-input"
          value={form.nombre}
          onChange={onChange}
          disabled={saving || !wired}
          placeholder="Estadio Más Monumental"
        />
      </div>
      <div className="gestion-form__row">
        <div className="form-group">
          <label htmlFor="ciudad">Ciudad</label>
          <input
            id="ciudad"
            name="ciudad"
            className="form-input"
            value={form.ciudad}
            onChange={onChange}
            disabled={saving || !wired}
            placeholder="Buenos Aires"
          />
        </div>
        <div className="form-group">
          <label htmlFor="direccion">Dirección</label>
          <input
            id="direccion"
            name="direccion"
            className="form-input"
            value={form.direccion}
            onChange={onChange}
            disabled={saving || !wired}
            placeholder="Av. Figueroa Alcorta 7597"
          />
        </div>
      </div>
      <div className="gestion-form__row">
        <div className="form-group">
          <label htmlFor="latitud">Latitud (opcional)</label>
          <input
            id="latitud"
            name="latitud"
            inputMode="decimal"
            className="form-input"
            value={form.latitud}
            onChange={onChange}
            disabled={saving || !wired}
            placeholder="-34.5453"
          />
        </div>
        <div className="form-group">
          <label htmlFor="longitud">Longitud (opcional)</label>
          <input
            id="longitud"
            name="longitud"
            inputMode="decimal"
            className="form-input"
            value={form.longitud}
            onChange={onChange}
            disabled={saving || !wired}
            placeholder="-58.4497"
          />
        </div>
      </div>
      <p className="gestion-card__muted">
        Latitud entre -90 y 90, longitud entre -180 y 180. Se completan las dos o
        ninguna. Si el lugar tenía coordenadas y vaciás los dos campos, se
        borran.
      </p>
      <div className="gestion-card__actions gestion-card__actions--tight">
        <button
          type="button"
          className="btn"
          onClick={buscarCoordenadas}
          disabled={saving || geocoding || !wired || !form.ciudad.trim()}
        >
          {geocoding ? 'Buscando coordenadas…' : 'Buscar coordenadas por dirección'}
        </button>
      </div>

      {error && <p className="gestion-error" role="alert">{error}</p>}
      {aviso && <p className="gestion-card__muted" role="status">✔ {aviso}</p>}

      <div className="gestion-card__actions">
        <GateButton enabled={wired} type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Guardando…' : esNuevo ? 'Crear lugar' : 'Guardar cambios'}
        </GateButton>
        <Link className="btn" to="/gestion/lugares">
          {esNuevo ? 'Cancelar' : 'Volver'}
        </Link>
      </div>
    </form>
  )
}
