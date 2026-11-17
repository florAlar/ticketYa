/** Helpers de participantes de evento (sin componentes para Fast Refresh). */

let teloneroKeySeq = 1

export function newTeloneroRow(partial = {}) {
  return {
    key: `t_${Date.now()}_${teloneroKeySeq++}`,
    id: partial.id || null,
    nombre: partial.nombre || '',
  }
}

/** Mapea respuesta BE/mock → principal + teloneros (con id). */
export function splitParticipantesForm(list) {
  const arr = Array.isArray(list) ? list : []
  const objs = arr
    .map((p) => {
      if (p && typeof p === 'object') {
        return {
          id: p.id || null,
          nombre: String(p.nombre || '').trim(),
          rol: String(p.rol || '').toUpperCase(),
        }
      }
      return { id: null, nombre: String(p || '').trim(), rol: '' }
    })
    .filter((p) => p.nombre || p.id)

  let principal = objs.find((p) => p.rol === 'PRINCIPAL')
  if (!principal && objs.length) principal = objs[0]
  const teloneros = objs
    .filter((p) => !principal || p.id !== principal.id || p.nombre !== principal.nombre)
    .filter((p) => {
      if (!principal) return true
      if (principal.id && p.id) return p.id !== principal.id
      return p !== principal
    })
    .filter((p) => p.rol === 'TELONERO' || p.rol !== 'PRINCIPAL')
    .map((p) => newTeloneroRow({ id: p.id, nombre: p.nombre }))

  return {
    principal: {
      id: principal?.id || null,
      nombre: principal?.nombre || '',
    },
    // legacy aliases
    principalNombre: principal?.nombre || '',
    principalId: principal?.id || null,
    teloneros,
  }
}
