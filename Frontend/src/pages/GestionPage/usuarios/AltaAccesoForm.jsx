/** Alta de personal de acceso (solo mock hasta que el BE lo defina). */
export default function AltaAccesoForm({
  form,
  setForm,
  eventoOptions,
  busyId,
  onSubmit,
  onToggleEvento,
}) {
  return (
    <form className="gestion-card gestion-form" onSubmit={onSubmit}>
      <h2 className="gestion-section__title">Alta ACCESO</h2>
      <div className="gestion-form__row">
        <div className="form-group">
          <label htmlFor="acc-nombre">Nombre</label>
          <input
            id="acc-nombre"
            className="form-input"
            value={form.nombre}
            onChange={(e) => setForm({ ...form, nombre: e.target.value })}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="acc-email">Email</label>
          <input
            id="acc-email"
            type="email"
            className="form-input"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="acc-pass">Contraseña</label>
          <input
            id="acc-pass"
            type="text"
            className="form-input"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        </div>
      </div>
      <p className="gestion-card__muted">Eventos asignados</p>
      <div className="gestion-checks">
        {eventoOptions.length === 0 && (
          <span className="gestion-card__muted">No hay eventos en el store.</span>
        )}
        {eventoOptions.map((ev) => (
          <label key={ev.id} className="gestion-check">
            <input
              type="checkbox"
              checked={form.eventosAsignados.includes(ev.id)}
              onChange={() => onToggleEvento(ev.id)}
            />
            {ev.label}
          </label>
        ))}
      </div>
      <button
        type="submit"
        className="btn btn-primary"
        disabled={busyId === 'create'}
      >
        {busyId === 'create' ? 'Creando…' : 'Crear acceso'}
      </button>
    </form>
  )
}
