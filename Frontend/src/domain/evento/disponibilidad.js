import { EVENTO_ESTADOS } from './constants'

/** Niveles de cupo libre (msvc-ventas NivelCupo). */
export const NIVEL_CUPO = {
  AGOTADO: 'AGOTADO',
  ULTIMOS_LUGARES: 'ULTIMOS_LUGARES',
  MENOS_DE_50: 'MENOS_DE_50',
  MENOS_DE_100: 'MENOS_DE_100',
  OK: 'OK',
}

export const NIVEL_CUPO_LABELS = {
  [NIVEL_CUPO.AGOTADO]: 'Agotado',
  [NIVEL_CUPO.ULTIMOS_LUGARES]: 'Últimos lugares',
  [NIVEL_CUPO.MENOS_DE_50]: 'Menos de 50',
  [NIVEL_CUPO.MENOS_DE_100]: 'Menos de 100',
  [NIVEL_CUPO.OK]: null,
}

/** Umbrales alineados al BE (NivelCupo.java). */
export const CUPO_UMBRAL_ULTIMOS = 20
export const CUPO_UMBRAL_50 = 50
export const CUPO_UMBRAL_100 = 100

/** Clasifica número real de cupos libres → nivel + label. */
export function clasificarNivelCupo(disponibleTotal) {
  const n = Number(disponibleTotal)
  const total = Number.isFinite(n) ? Math.max(0, Math.floor(n)) : 0
  let nivel = NIVEL_CUPO.OK
  if (total < 1) nivel = NIVEL_CUPO.AGOTADO
  else if (total < CUPO_UMBRAL_ULTIMOS) nivel = NIVEL_CUPO.ULTIMOS_LUGARES
  else if (total < CUPO_UMBRAL_50) nivel = NIVEL_CUPO.MENOS_DE_50
  else if (total < CUPO_UMBRAL_100) nivel = NIVEL_CUPO.MENOS_DE_100
  return {
    disponibleTotal: total,
    agotado: nivel === NIVEL_CUPO.AGOTADO,
    nivelCupo: nivel,
    nivelCupoLabel: NIVEL_CUPO_LABELS[nivel],
  }
}

/**
 * Normaliza DTO disponibilidad (1 evento o ítem batch).
 * Respeta números del BE; completa clasificación si faltara.
 */
export function normalizeDisponibilidadDto(raw) {
  if (!raw || typeof raw !== 'object') return null
  const sectores = Array.isArray(raw.sectores)
    ? raw.sectores.map((s) => {
        const disp = Number(s.cantidadDisponible)
        const d = Number.isFinite(disp) ? disp : 0
        const cls = clasificarNivelCupo(d)
        return {
          idSector: s.idSector,
          capacidadTotal: s.capacidadTotal,
          cantidadDisponible: d,
          cantidadReservada: s.cantidadReservada,
          cantidadVendida: s.cantidadVendida,
          asientosOcupados: Array.isArray(s.asientosOcupados)
            ? s.asientosOcupados.map((a) => ({
                fila: String(a.fila),
                numero: Number(a.numero),
                estado: a.estado,
              }))
            : [],
          agotado: s.agotado != null ? Boolean(s.agotado) : cls.agotado,
          nivelCupo: s.nivelCupo || cls.nivelCupo,
          nivelCupoLabel:
            s.nivelCupoLabel != null ? s.nivelCupoLabel : cls.nivelCupoLabel,
        }
      })
    : []
  let total = Number(raw.disponibleTotal)
  if (!Number.isFinite(total)) {
    total = sectores.reduce((acc, s) => acc + (s.cantidadDisponible || 0), 0)
  }
  const cls = clasificarNivelCupo(total)
  return {
    idEvento: raw.idEvento,
    disponibleTotal: cls.disponibleTotal,
    agotado: raw.agotado != null ? Boolean(raw.agotado) : cls.agotado,
    nivelCupo: raw.nivelCupo || cls.nivelCupo,
    nivelCupoLabel:
      raw.nivelCupoLabel != null ? raw.nivelCupoLabel : cls.nivelCupoLabel,
    sectores,
  }
}

/** Adjunta resumen de cupo al evento de catálogo/detalle. */
export function withCupoResumen(evento, disponibilidad) {
  if (!evento) return evento
  const cupo = normalizeDisponibilidadDto(disponibilidad)
  return cupo ? { ...evento, cupo } : { ...evento, cupo: null }
}

export function cupoLabelDeEvento(evento) {
  return evento?.cupo?.nivelCupoLabel || null
}

export function isEventoAgotado(evento) {
  if (evento?.estado === EVENTO_ESTADOS.AGOTADO) return true
  if (evento?.cupo) return Boolean(evento.cupo.agotado)
  return false
}
