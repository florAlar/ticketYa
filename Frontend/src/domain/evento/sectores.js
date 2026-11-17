import { EVENTO_ESTADOS } from './constants'

/**
 * Filas de la tabla de sectores de un evento (LUG-04). Los sectores los define
 * el lugar: el organizador elige cuáles se venden y fija precio/cupo.
 */
export function rowsDeLugar(lugar, delEvento = []) {
  const sectores = Array.isArray(lugar?.sectores) ? lugar.sectores : []
  return sectores.map((s) => {
    const previo = (Array.isArray(delEvento) ? delEvento : []).find((x) => x.id === s.id)
    const capacidad = Number(s.capacidad) || 0
    const cupoPrevio = previo ? Number(previo.capacidad ?? previo.capacidadTotal) : 0
    return {
      id: s.id,
      nombre: s.nombre || '',
      tipo: s.tipo || '',
      capacidad,
      esNumerado: Boolean(s.esNumerado),
      filas: s.filas,
      asientosPorFila: s.asientosPorFila,
      incluido: Boolean(previo),
      precio: previo && previo.precio != null ? String(previo.precio) : '',
      cupo:
        previo && !s.esNumerado && cupoPrevio > 0 && cupoPrevio < capacidad
          ? String(cupoPrevio)
          : '',
    }
  })
}

/** '' / null → null; otro valor → Number (NaN si es basura). Acepta coma decimal. */
function numeroOVacio(valor) {
  const s = String(valor ?? '').trim().replace(',', '.')
  return s === '' ? null : Number(s)
}

/**
 * Sectores elegidos del form, listos para el pedido:
 * { id, precio, capacidadTotal? }.
 */
export function cleanSectoresFromForm(rows) {
  if (!Array.isArray(rows)) return []
  return rows
    .filter((r) => r?.incluido)
    .map((r) => {
      const precio = numeroOVacio(r.precio)
      const cupo = r.esNumerado ? null : numeroOVacio(r.cupo)
      const out = { id: r.id, precio: Number.isFinite(precio) ? precio : null }
      if (cupo !== null) out.capacidadTotal = cupo
      return out
    })
}

/** Mensaje de error de los sectores elegidos ('' si están bien). */
function errorEnSectoresElegidos(rows, estricto) {
  for (const r of (Array.isArray(rows) ? rows : []).filter((x) => x?.incluido)) {
    const precio = numeroOVacio(r.precio)
    if (precio === null) {
      if (estricto) return `Para publicar, «${r.nombre}» necesita precio ≥ 0`
    } else if (!Number.isFinite(precio) || precio < 0) {
      return `Precio inválido en «${r.nombre}»`
    }
    if (estricto && !(r.capacidad >= 1)) {
      return `El sector «${r.nombre}» no tiene capacidad: el administrador tiene que definirla en el lugar`
    }
    if (!r.esNumerado) {
      const cupo = numeroOVacio(r.cupo)
      if (cupo !== null && (!Number.isInteger(cupo) || cupo < 1 || cupo > r.capacidad)) {
        return `El cupo de «${r.nombre}» debe ser un entero entre 1 y ${r.capacidad} (capacidad del sector)`
      }
    }
  }
  return ''
}

/**
 * Validación del form de gestión (LUG-04).
 * mode 'borrador': solo nombre y artista principal; lugar/sectores opcionales.
 * mode 'publicacion': completo (fecha, lugar, ≥1 sector con precio).
 */
export function validateEventoGestionForm(form, sectoresRows, mode = 'borrador') {
  const isPub =
    String(mode).toUpperCase() === 'PUBLICACION' ||
    String(mode).toUpperCase() === EVENTO_ESTADOS.PUBLICADO

  const nombre = String(form?.nombre || '').trim()
  if (!nombre) {
    return { ok: false, error: 'El nombre del evento es obligatorio', sectores: [] }
  }

  const principal = String(form?.principalId ?? form?.principal?.id ?? '').trim()
  if (!principal) {
    return {
      ok: false,
      error:
        'Elegí un artista / banda principal del catálogo (o crealo desde el buscador)',
      sectores: [],
    }
  }

  const telos = Array.isArray(form?.teloneros) ? form.teloneros : []
  for (let i = 0; i < telos.length; i++) {
    const t = telos[i]
    const hasText = String(t?.nombre || '').trim()
    const hasId = String(t?.id || '').trim()
    if (hasText && !hasId) {
      return {
        ok: false,
        error: `Telonero ${i + 1}: elegí del catálogo o crealo (hace falta el id)`,
        sectores: [],
      }
    }
  }

  const fecha = form?.fechaInicio || form?.fechaHora || ''
  const idLugar = String(form?.idLugar || '').trim()
  const sectores = cleanSectoresFromForm(sectoresRows)

  if (sectores.length && !idLugar) {
    return { ok: false, error: 'Elegí un lugar antes de seleccionar sectores', sectores }
  }
  const errorSectores = errorEnSectoresElegidos(sectoresRows, isPub)
  if (errorSectores) {
    return { ok: false, error: errorSectores, sectores }
  }

  if (!isPub) {
    return { ok: true, error: '', sectores }
  }

  if (!fecha) {
    return { ok: false, error: 'Para publicar necesitás fecha y hora de inicio', sectores }
  }
  if (!idLugar) {
    return { ok: false, error: 'Para publicar elegí un lugar', sectores }
  }
  if (!sectores.length) {
    return {
      ok: false,
      error: 'Para publicar elegí al menos un sector del lugar con su precio',
      sectores,
    }
  }
  return { ok: true, error: '', sectores }
}

/** Normaliza los sectores crudos del BE/mock. */
export function normalizeSectores(raw) {
  if (!Array.isArray(raw) || raw.length === 0) return []
  return raw.map((s, i) => {
    const capRaw = s.capacidadTotal != null ? s.capacidadTotal : s.capacidad
    const cap = Number(capRaw)
    return {
      id: s.id || `sec_${i + 1}`,
      nombre: String(s.nombre || `Sector ${i + 1}`).trim(),
      esNumerado: Boolean(s.esNumerado),
      sinCupo: false,
      capacidadTotal: Number.isFinite(cap) ? cap : 0,
      filas: s.filas != null ? Number(s.filas) : undefined,
      asientosPorFila:
        s.asientosPorFila != null ? Number(s.asientosPorFila) : undefined,
      precio: Number(s.precio) || 0,
    }
  })
}

export function minPrecioSectores(sectores, fallback) {
  const prices = (sectores || [])
    .map((s) => Number(s.precio))
    .filter((n) => !Number.isNaN(n) && n >= 0)
  if (prices.length) return Math.min(...prices)
  const f = Number(fallback)
  return Number.isFinite(f) ? f : 0
}
