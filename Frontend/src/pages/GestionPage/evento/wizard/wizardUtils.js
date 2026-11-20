/** Fecha legible a partir del valor de un input datetime-local. */
export function fechaLegible(valor) {
  const d = new Date(valor)
  if (!valor || Number.isNaN(d.getTime())) return ''
  const dia = d.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
  const hora = d.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })
  return `${dia} · ${hora} h`
}

/** Lo que falta (o no) para poder publicar. */
export function checklistPublicacion(form, sectores) {
  const elegidos = (sectores || []).filter((s) => s.incluido)
  const fecha = form.fechaInicio ? new Date(form.fechaInicio) : null
  return [
    { ok: form.nombre.trim().length >= 2, texto: 'Tiene un nombre', paso: 0 },
    { ok: Boolean(form.principal?.id), texto: 'Tiene un artista principal', paso: 2 },
    { ok: Boolean(fecha && !Number.isNaN(fecha.getTime()) && fecha > new Date()), texto: 'Tiene fecha y hora futuras', paso: 1 },
    { ok: Boolean(form.idLugar), texto: 'Tiene un lugar', paso: 1 },
    {
      ok: elegidos.length > 0 && elegidos.every((s) => String(s.precio).trim() !== '' && Number(String(s.precio).replace(',', '.')) >= 0),
      texto: 'Vende al menos un sector con su precio',
      paso: 3,
    },
  ]
}

