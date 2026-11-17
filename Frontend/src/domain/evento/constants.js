/** Constantes y labels de eventos (msvc-eventos). */

export const EVENTO_TIPOS = {
  RECITAL: 'RECITAL',
  TEATRO: 'TEATRO',
  DEPORTE: 'DEPORTE',
  CONFERENCIA: 'CONFERENCIA',
  FESTIVAL: 'FESTIVAL',
}

export const EVENTO_TIPO_LABELS = {
  [EVENTO_TIPOS.RECITAL]: 'Recital',
  [EVENTO_TIPOS.TEATRO]: 'Teatro',
  [EVENTO_TIPOS.DEPORTE]: 'Deporte',
  [EVENTO_TIPOS.CONFERENCIA]: 'Conferencia',
  [EVENTO_TIPOS.FESTIVAL]: 'Festival',
}

/** Estados de ciclo de vida del evento. */
export const EVENTO_ESTADOS = {
  BORRADOR: 'BORRADOR',
  PUBLICADO: 'PUBLICADO',
  AGOTADO: 'AGOTADO',
  SUSPENDIDO: 'SUSPENDIDO',
  CANCELADO: 'CANCELADO',
  FINALIZADO: 'FINALIZADO',
}

export const EVENTO_ESTADO_LABELS = {
  [EVENTO_ESTADOS.BORRADOR]: 'Borrador',
  [EVENTO_ESTADOS.PUBLICADO]: 'Publicado',
  [EVENTO_ESTADOS.AGOTADO]: 'Agotado',
  [EVENTO_ESTADOS.SUSPENDIDO]: 'Suspendido',
  [EVENTO_ESTADOS.CANCELADO]: 'Cancelado',
  [EVENTO_ESTADOS.FINALIZADO]: 'Finalizado',
}

/**
 * Lugar reservado del backend para los borradores que todavía no tienen lugar
 * elegido. No se muestra ni se elige.
 */
export const LUGAR_POR_DEFINIR_ID = 'c0000000-0000-4000-8000-0000000000ff'
