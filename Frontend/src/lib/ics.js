/**
 * Genera y descarga un archivo .ics (calendario) para un evento: se abre con Google Calendar,
 * Outlook, Apple Calendar, etc. Las horas van "flotantes" (hora local del lugar, sin zona).
 */
function pad(n) {
  return String(n).padStart(2, '0')
}

function fechaIcs(date) {
  return (
    `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}` +
    `T${pad(date.getHours())}${pad(date.getMinutes())}00`
  )
}

function escapar(texto) {
  return String(texto || '')
    .replace(/\\/g, '\\\\')
    .replace(/\n/g, '\\n')
    .replace(/,/g, '\\,')
    .replace(/;/g, '\\;')
}

export function descargarEventoIcs(evento, { duracionHoras = 3 } = {}) {
  const inicio = new Date(evento?.fechaInicio)
  if (Number.isNaN(inicio.getTime())) return
  const fin = evento.fechaFin ? new Date(evento.fechaFin) : new Date(inicio.getTime() + duracionHoras * 3600 * 1000)
  const lugar = [evento.lugar?.nombre, evento.lugar?.direccion, evento.lugar?.ciudad]
    .filter(Boolean)
    .join(', ')
  const lineas = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//TicketYa//Evento//ES',
    'BEGIN:VEVENT',
    `UID:${evento.id}@ticketya`,
    `DTSTAMP:${fechaIcs(new Date())}`,
    `DTSTART:${fechaIcs(inicio)}`,
    `DTEND:${fechaIcs(fin)}`,
    `SUMMARY:${escapar(evento.nombre)}`,
    `LOCATION:${escapar(lugar)}`,
    `DESCRIPTION:${escapar(evento.descripcion || 'Entradas en TicketYa')}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ]
  const blob = new Blob([lineas.join('\r\n')], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${String(evento.nombre || 'evento').replace(/[^\w-]+/g, '-').slice(0, 60)}.ics`
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
