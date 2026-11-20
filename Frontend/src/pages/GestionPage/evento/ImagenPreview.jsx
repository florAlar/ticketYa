import EventCover from '../../../components/ui/EventCover'

/**
 * Vista previa de la portada mientras se completa el formulario. Sin URL muestra
 * la portada generada que verá el público; con URL, la imagen (y si no carga,
 * cae a la generada).
 */
export default function ImagenPreview({ form }) {
  const url = String(form?.imagenUrl || '').trim()
  const valida = !url || /^https?:\/\//i.test(url) || (url.startsWith('/') && !url.startsWith('//'))
  return (
    <div className="gestion-imagen-preview">
      <div className="gestion-imagen-preview__box">
        <EventCover
          evento={{ nombre: form?.nombre, tipo: form?.tipo, imagenUrl: valida ? url : '' }}
        />
      </div>
      <p className={`gestion-card__muted${valida ? '' : ' gestion-error'}`}>
        {valida
          ? url
            ? 'Así se verá la portada. Si el enlace no carga, se usa una portada generada.'
            : 'Sin URL se usa una portada generada según el tipo de evento.'
          : 'La URL debe empezar con http:// o https://'}
      </p>
    </div>
  )
}
