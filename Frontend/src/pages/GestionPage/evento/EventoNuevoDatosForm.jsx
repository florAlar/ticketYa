import {
  EVENTO_ESTADOS,
  EVENTO_ESTADO_LABELS,
  EVENTO_TIPOS,
  EVENTO_TIPO_LABELS,
} from '../../../domain/evento'

/** Campos del alta de evento (borrador o publicación). */
export default function EventoNuevoDatosForm({
  form,
  loading,
  publishing,
  onChange,
}) {
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
          <label htmlFor="estado">Al guardar como</label>
          <select
            id="estado"
            name="estado"
            className="form-input"
            value={form.estado}
            onChange={onChange}
            disabled={loading}
          >
            {[EVENTO_ESTADOS.BORRADOR, EVENTO_ESTADOS.PUBLICADO].map((s) => (
              <option key={s} value={s}>
                {EVENTO_ESTADO_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="form-group">
        <label htmlFor="fechaInicio">
          Fecha y hora {publishing ? '*' : '(obligatoria al publicar)'}
        </label>
        <input
          id="fechaInicio"
          name="fechaInicio"
          type="datetime-local"
          className="form-input"
          value={form.fechaInicio}
          onChange={onChange}
          disabled={loading}
        />
        <p className="gestion-card__muted gestion-hint--top">
          En borrador podés dejarla vacía y completarla después. Al publicar es
          obligatoria.
        </p>
      </div>

      <div className="gestion-checks">
        <label className="gestion-check">
          <input
            type="checkbox"
            name="esAltaDemanda"
            checked={form.esAltaDemanda}
            onChange={onChange}
            disabled={loading}
          />
          Alta demanda (fila virtual)
        </label>
        <label className="gestion-check">
          <input
            type="checkbox"
            name="destacado"
            checked={form.destacado}
            onChange={onChange}
            disabled={loading}
          />
          Destacado en home
        </label>
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
