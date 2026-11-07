import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import EventList from '../../components/home/events/EventList'
import {
  EVENTO_TIPOS,
  EVENTO_TIPO_LABELS,
} from '../../domain/evento'
import '../../components/home/events/events.css'
import './EventosPage.css'

const ORDEN_OPTIONS = [
  { value: 'fecha', label: 'Fecha' },
  { value: 'precio', label: 'Precio' },
]

function readFilters(params) {
  const next = {}
  const q = params.get('q')
  const tipo = params.get('tipo')
  const ciudad = params.get('ciudad')
  const organizador = params.get('organizador')
  const fechaDesde = params.get('fechaDesde')
  const fechaHasta = params.get('fechaHasta')
  const orden = params.get('orden') || params.get('sort')
  const esAltaDemanda = params.get('esAltaDemanda')
  const incluirSuspendidos = params.get('incluirSuspendidos')
  const estado = params.get('estado')
  const page = params.get('page')
  const size = params.get('size')

  if (q) next.q = q
  if (tipo) next.tipo = tipo
  if (ciudad) next.ciudad = ciudad
  if (organizador) next.organizador = organizador
  if (fechaDesde) next.fechaDesde = fechaDesde
  if (fechaHasta) next.fechaHasta = fechaHasta
  if (orden) next.orden = orden === 'precio' ? 'precio' : 'fecha'
  if (esAltaDemanda === '1' || esAltaDemanda === 'true') next.esAltaDemanda = true
  // Catálogo comprable por default (PUBLICADO). Opcional: ver también suspendidos.
  if (incluirSuspendidos === '1' || incluirSuspendidos === 'true') {
    next.estado = 'TODOS'
    next.incluirSuspendidos = true
  } else if (estado) {
    next.estado = estado
  }
  // sin estado → BE default PUBLICADO
  if (page != null && page !== '') next.page = page
  if (size) next.size = size
  return next
}

/**
 * Catálogo JWT con sidebar de filtros.
 * Link «Eventos» en nav solo con sesión + SearchBar.
 * Default: solo PUBLICADO (comprables). Checkbox para incluir suspendidos.
 */
const EventosPage = () => {
  const [params, setParams] = useSearchParams()

  const filter = useMemo(() => readFilters(params), [params])

  const formValues = useMemo(
    () => ({
      q: params.get('q') || '',
      tipo: params.get('tipo') || '',
      ciudad: params.get('ciudad') || '',
      organizador: params.get('organizador') || '',
      fechaDesde: params.get('fechaDesde') || '',
      fechaHasta: params.get('fechaHasta') || '',
      orden: params.get('orden') || params.get('sort') || 'fecha',
      esAltaDemanda:
        params.get('esAltaDemanda') === '1' ||
        params.get('esAltaDemanda') === 'true',
      incluirSuspendidos:
        params.get('incluirSuspendidos') === '1' ||
        params.get('incluirSuspendidos') === 'true',
    }),
    [params]
  )

  const subtitle = useMemo(() => {
    const parts = []
    if (filter.q) parts.push(`“${filter.q}”`)
    if (filter.tipo) parts.push(EVENTO_TIPO_LABELS[filter.tipo] || filter.tipo)
    if (filter.ciudad) parts.push(filter.ciudad)
    if (filter.organizador) parts.push(`org ${filter.organizador}`)
    if (filter.fechaDesde || filter.fechaHasta) {
      parts.push(
        [filter.fechaDesde || '…', filter.fechaHasta || '…'].join(' → ')
      )
    }
    if (filter.esAltaDemanda) parts.push('alta demanda')
    if (filter.incluirSuspendidos) parts.push('incluye suspendidos')
    if (filter.orden === 'precio') parts.push('por precio')
    return parts.length ? parts.join(' · ') : 'Eventos publicados a la venta'
  }, [filter])

  const applyForm = useCallback(
    (e) => {
      e.preventDefault()
      const fd = new FormData(e.currentTarget)
      const next = new URLSearchParams()
      const setIf = (key, raw) => {
        const v = String(raw ?? '').trim()
        if (v) next.set(key, v)
      }
      setIf('q', fd.get('q'))
      setIf('tipo', fd.get('tipo'))
      setIf('ciudad', fd.get('ciudad'))
      setIf('organizador', fd.get('organizador'))
      setIf('fechaDesde', fd.get('fechaDesde'))
      setIf('fechaHasta', fd.get('fechaHasta'))
      const orden = String(fd.get('orden') || 'fecha')
      if (orden && orden !== 'fecha') next.set('orden', orden)
      if (fd.get('esAltaDemanda')) next.set('esAltaDemanda', 'true')
      if (fd.get('incluirSuspendidos')) next.set('incluirSuspendidos', 'true')
      setParams(next, { replace: false })
    },
    [setParams]
  )

  const clearFilters = useCallback(() => {
    setParams({}, { replace: false })
  }, [setParams])

  const hasActive = Object.keys(filter).length > 0

  return (
    <section className="eventos-page section-v-padding">
      <div className="eventos-page__shell">
        <aside className="eventos-sidebar" aria-label="Filtros de eventos">
          <h2 className="eventos-sidebar__title">Filtros</h2>
          <form className="eventos-sidebar__form" onSubmit={applyForm}>
            <div className="form-group">
              <label htmlFor="ev-q">Palabra clave</label>
              <input
                id="ev-q"
                className="form-input"
                name="q"
                type="search"
                defaultValue={formValues.q}
                key={`q-${formValues.q}`}
                placeholder="Nombre, ciudad…"
              />
            </div>

            <div className="form-group">
              <label htmlFor="ev-tipo">Tipo</label>
              <select
                id="ev-tipo"
                className="form-input"
                name="tipo"
                defaultValue={formValues.tipo}
                key={`tipo-${formValues.tipo}`}
              >
                <option value="">Todos</option>
                {Object.values(EVENTO_TIPOS).map((t) => (
                  <option key={t} value={t}>
                    {EVENTO_TIPO_LABELS[t] || t}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="ev-ciudad">Ciudad</label>
              <input
                id="ev-ciudad"
                className="form-input"
                name="ciudad"
                defaultValue={formValues.ciudad}
                key={`ciudad-${formValues.ciudad}`}
                placeholder="Ej. Tandil"
              />
            </div>

            <div className="form-group">
              <label htmlFor="ev-org">Organizador (id)</label>
              <input
                id="ev-org"
                className="form-input"
                name="organizador"
                defaultValue={formValues.organizador}
                key={`org-${formValues.organizador}`}
                placeholder="id organizador"
              />
            </div>

            <div className="form-group">
              <label htmlFor="ev-desde">Fecha desde</label>
              <input
                id="ev-desde"
                className="form-input"
                name="fechaDesde"
                type="date"
                defaultValue={formValues.fechaDesde}
                key={`desde-${formValues.fechaDesde}`}
              />
            </div>

            <div className="form-group">
              <label htmlFor="ev-hasta">Fecha hasta</label>
              <input
                id="ev-hasta"
                className="form-input"
                name="fechaHasta"
                type="date"
                defaultValue={formValues.fechaHasta}
                key={`hasta-${formValues.fechaHasta}`}
              />
            </div>

            <div className="form-group">
              <label htmlFor="ev-orden">Orden</label>
              <select
                id="ev-orden"
                className="form-input"
                name="orden"
                defaultValue={
                  formValues.orden === 'precio' ? 'precio' : 'fecha'
                }
                key={`orden-${formValues.orden}`}
              >
                {ORDEN_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>

            <label className="eventos-sidebar__check">
              <input
                type="checkbox"
                name="esAltaDemanda"
                value="true"
                defaultChecked={formValues.esAltaDemanda}
                key={`alta-${formValues.esAltaDemanda}`}
              />
              <span>Solo alta demanda</span>
            </label>

            <label className="eventos-sidebar__check">
              <input
                type="checkbox"
                name="incluirSuspendidos"
                value="true"
                defaultChecked={formValues.incluirSuspendidos}
                key={`susp-${formValues.incluirSuspendidos}`}
              />
              <span>Incluir suspendidos (solo lectura)</span>
            </label>

            <div className="eventos-sidebar__actions">
              <button type="submit" className="btn btn-primary">
                Aplicar
              </button>
              {hasActive && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={clearFilters}
                >
                  Limpiar
                </button>
              )}
            </div>
          </form>
        </aside>

        <div className="eventos-page__main">
          <header className="eventos-page__head">
            <h1 className="eventos-page__title">Eventos</h1>
            <p className="eventos-page__sub">{subtitle}</p>
          </header>
          <EventList filter={filter} layout="grid-4" card="standard" />
        </div>
      </div>
    </section>
  )
}

export default EventosPage
