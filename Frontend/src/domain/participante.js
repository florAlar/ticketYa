/** Participantes (PAR-01): tipos del catálogo y validación del formulario (espeja msvc-eventos). */
export const TIPOS_PARTICIPANTE = [
  { value: 'ARTISTA', label: 'Artista' },
  { value: 'BANDA', label: 'Banda' },
  { value: 'ELENCO', label: 'Elenco' },
  { value: 'EQUIPO', label: 'Equipo' },
  { value: 'PILOTO', label: 'Piloto' },
]

export function labelTipoParticipante(tipo) {
  return TIPOS_PARTICIPANTE.find((t) => t.value === tipo)?.label || tipo || '—'
}

export function participanteVacio() {
  return { nombre: '', tipo: 'BANDA', descripcion: '' }
}

/** @returns {string|null} mensaje de error o null si es válido */
export function validarParticipante(f) {
  if (!String(f?.nombre || '').trim()) return 'El nombre es obligatorio'
  if (String(f.nombre).trim().length > 200) return 'El nombre no puede superar los 200 caracteres'
  if (!TIPOS_PARTICIPANTE.some((t) => t.value === f.tipo)) return 'Elegí un tipo válido'
  return null
}

export function participantePayload(f) {
  return {
    nombre: String(f.nombre).trim(),
    tipo: f.tipo,
    descripcion: String(f.descripcion || '').trim() || null,
  }
}
