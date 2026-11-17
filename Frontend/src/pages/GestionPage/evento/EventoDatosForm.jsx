import {
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPOS,
  EVENTO_TIPO_LABELS,
} from '../../../domain/evento'

/** Campos básicos del evento en borrador. */
export default function EventoDatosForm({ form, loading, onChange }) {
  return (
    <>
      <div className="form-group">
        <label htmlFor="nombre">Nombre *</label>
        <input
          id="nombre"
          name="nombre"
          className="form-input"
          value={form.nombre}
          onChange={onChange}
          required
          disabled={loading}
        />
      </div>

      <div className="form-group">
        <label htmlFor="descripcion">Descripción</label>
        <textarea
          id="descripcion"
          name="descripcion"
          className="form-textarea"
          rows={3}
          value={form.descripcion}
          onChange={onChange}
          disabled={loading}
        />
      </div>

      <div className="gestion-form__row">
        <div className="form-group">
          <label htmlFor="tipo">Tipo de evento</label>
          <select
            id="tipo"
            name="tipo"
            className="form-input"
            value={form.tipo}
            onChange={onChange}
            disabled={loading}
          >
            {Object.values(EVENTO_TIPOS).map((t) => (
              <option key={t} value={t}>
                {EVENTO_TIPO_LABELS[t] || t}
              </option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Estado</label>
          <input
            className="form-input"
            value={EVENTO_ESTADO_LABELS.BORRADOR}
            disabled
            readOnly
          />
        </div>
      </div>

      <div className="form-group">
        <label htmlFor="fechaInicio">Fecha y hora (futura al publicar)</label>
        <input
          id="fechaInicio"
          name="fechaInicio"
          type="datetime-local"
          className="form-input"
          value={form.fechaInicio}
          onChange={onChange}
          disabled={loading}
        />
      </div>

      <div className="form-group">
        <label htmlFor="imagenUrl">URL imagen (opcional)</label>
        <input
          id="imagenUrl"
          name="imagenUrl"
          className="form-input"
          value={form.imagenUrl}
          onChange={onChange}
          disabled={loading}
        />
      </div>
    </>
  )
}
