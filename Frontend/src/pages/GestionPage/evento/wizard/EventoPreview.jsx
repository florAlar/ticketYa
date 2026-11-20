import { LIMITE_ALTA_DEMANDA, cupoTotalSectores } from '../../../../domain/evento'
import '../../../../components/home/events/events.css'
import EventCardStandard from '../../../../components/home/events/cards/EventCardStandard'

/** Vista previa en vivo: la card tal como la verá el público en el catálogo. */
export default function EventoPreview({ form, sectores, lugar }) {
  const elegidos = (sectores || []).filter((s) => s.incluido)
  const precios = elegidos.filter((s) => String(s.precio).trim() !== '').map((s) => Number(String(s.precio).replace(',', '.'))).filter((n) => Number.isFinite(n) && n >= 0)
  const evento = {
    id: 'vista-previa',
    nombre: form.nombre.trim() || 'Nombre del evento',
    tipo: form.tipo,
    fechaInicio: form.fechaInicio || null,
    lugar: lugar ? { nombre: lugar.nombre, ciudad: lugar.ciudad } : null,
    imagenUrl: form.imagenUrl?.trim() || null,
    esAltaDemanda: Boolean(form.esAltaDemanda) || cupoTotalSectores(sectores) > LIMITE_ALTA_DEMANDA,
    estado: 'PUBLICADO',
    precioDesde: precios.length ? Math.min(...precios) : null,
  }
  return (
    <aside className="wz-preview" aria-label="Vista previa del evento">
      <p className="wz-preview__label">Así se verá en el catálogo</p>
      <div className="wz-preview__card" aria-hidden>
        <EventCardStandard evento={evento} />
      </div>
    </aside>
  )
}
