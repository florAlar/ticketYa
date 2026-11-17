export const NOTIFICACION_TIPO_LABELS = {
  CompraConfirmada: 'Compra confirmada',
  EventoCancelado: 'Evento cancelado',
  EventoSuspendido: 'Evento suspendido',
  EventoModificado: 'Evento modificado',
  NuevoEventoArtistaFavorito: 'Artista favorito',
  DevolucionAprobada: 'Devolución aprobada',
}

export const NOTIFICACION_CANAL_LABELS = {
  PUSH: 'Push',
  EMAIL: 'Email',
}

export const NOTIFICACION_ESTADO_LABELS = {
  PENDIENTE: 'Pendiente',
  ENVIADA: 'Enviada',
  FALLIDA: 'Fallida',
}

export function tipoNotificacionLabel(tipo) {
  return NOTIFICACION_TIPO_LABELS[tipo] || tipo || 'Notificación'
}

export function canalNotificacionLabel(canal) {
  return NOTIFICACION_CANAL_LABELS[canal] || canal || '—'
}

export function estadoNotificacionLabel(estado) {
  return NOTIFICACION_ESTADO_LABELS[estado] || estado || '—'
}

export function formatFechaNotificacion(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('es-AR', {
      dateStyle: 'short',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}
