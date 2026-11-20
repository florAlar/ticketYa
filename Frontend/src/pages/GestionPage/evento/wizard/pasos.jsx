import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { HiCheck, HiOutlineExclamation, HiOutlineLocationMarker } from 'react-icons/hi'
import { EVENTO_TIPOS, EVENTO_TIPO_LABELS, LIMITE_ALTA_DEMANDA, cupoTotalSectores, urlMapaLugar } from '../../../../domain/evento'
import { describirSector } from '../../../../domain/lugar'
import { formatEntero, formatMoney } from '../../../../lib/format'
import EventoParticipantesFields from '../../../../components/gestion/EventoParticipantesFields'
import { ErrorState, LoadingState } from '../../../../components/ui/AsyncState'
import ImagenPreview from '../ImagenPreview'
import { checklistPublicacion, fechaLegible } from './wizardUtils'

function ahoraLocal() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

/* ---------------------------------------------------------------- 1 · Lo básico */
export function PasoBasicos({ form, onChange, loading }) {
  return (
    <div className="wz-grid">
      <div className="form-group wz-span-2">
        <label htmlFor="nombre">Nombre del evento *</label>
        <input id="nombre" name="nombre" className="form-input" value={form.nombre} onChange={onChange} disabled={loading} placeholder="Ej. Metallica — M72 World Tour" autoFocus />
        <p className="wz-hint">Se muestra como «Artista — Nombre del show»: lo que va antes del guion largo aparece como artista en las cards.</p>
      </div>
      <div className="form-group">
        <label htmlFor="tipo">Tipo de evento</label>
        <select id="tipo" name="tipo" className="form-input" value={form.tipo} onChange={onChange} disabled={loading}>
          {Object.values(EVENTO_TIPOS).map((t) => (
            <option key={t} value={t}>
              {EVENTO_TIPO_LABELS[t] || t}
            </option>
          ))}
        </select>
      </div>
      <div className="form-group wz-check">
        <label className="gestion-check">
          <input type="checkbox" name="destacado" checked={form.destacado} onChange={onChange} disabled={loading} />
          Destacado en la home
        </label>
      </div>
      <div className="form-group wz-span-2">
        <label htmlFor="descripcion">Descripción</label>
        <textarea id="descripcion" name="descripcion" className="form-textarea" rows={3} value={form.descripcion} onChange={onChange} disabled={loading} />
      </div>
      <div className="form-group wz-span-2">
        <label htmlFor="imagenUrl">Portada (URL de la imagen)</label>
        <input id="imagenUrl" name="imagenUrl" className="form-input" value={form.imagenUrl} onChange={onChange} disabled={loading} placeholder="https://…" />
        <ImagenPreview form={form} />
      </div>
    </div>
  )
}

/* ------------------------------------------------------------ 2 · Fecha y lugar */
function srcMapa(lat, lng) {
  const dx = 0.008
  const dy = 0.0045
  const bbox = [lng - dx, lat - dy, lng + dx, lat + dy].join('%2C')
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat}%2C${lng}`
}

export function PasoFechaLugar({ form, onChange, loading, lugares, lugaresStatus, lugaresError, recargarLugares, onElegirLugar, isAdmin }) {
  const [q, setQ] = useState('')
  const elegido = lugares.find((l) => l.id === form.idLugar) || null
  const visibles = useMemo(() => {
    const t = q.trim().toLowerCase()
    return t ? lugares.filter((l) => `${l.nombre} ${l.ciudad} ${l.direccion || ''}`.toLowerCase().includes(t)) : lugares
  }, [lugares, q])
  const lat = Number(elegido?.latitud)
  const lng = Number(elegido?.longitud)
  const conMapa = elegido && Number.isFinite(lat) && Number.isFinite(lng) && elegido.latitud != null

  return (
    <div className="wz-stack">
      <div className="form-group wz-fecha">
        <label htmlFor="fechaInicio">Fecha y hora de inicio</label>
        <input id="fechaInicio" name="fechaInicio" type="datetime-local" className="form-input" min={ahoraLocal()} value={form.fechaInicio} onChange={onChange} disabled={loading} />
        {form.fechaInicio ? <p className="wz-hint wz-hint--ok">{fechaLegible(form.fechaInicio)}</p> : <p className="wz-hint">Para guardar un borrador podés dejarla vacía; para publicar es obligatoria.</p>}
      </div>

      <div className="wz-lugares">
        <div className="wz-lugares__head">
          <label htmlFor="wz-buscar-lugar">Lugar</label>
          <input id="wz-buscar-lugar" type="search" className="form-input" placeholder="Buscar por nombre o ciudad…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>

        {lugaresStatus === 'loading' && <LoadingState label="Cargando lugares…" />}
        {lugaresStatus === 'error' && <ErrorState error={lugaresError} onRetry={recargarLugares} />}

        {lugaresStatus === 'success' && (
          <>
            {visibles.length === 0 ? (
              <p className="wz-hint">Ningún lugar coincide con la búsqueda.</p>
            ) : (
              <ul className="wz-cards">
                {visibles.map((l) => {
                  const activo = l.id === form.idLugar
                  return (
                    <li key={l.id}>
                      <button type="button" className={`wz-card${activo ? ' is-active' : ''}`} onClick={() => onElegirLugar(activo ? null : l)} disabled={loading} aria-pressed={activo}>
                        <span className="wz-card__name">
                          <HiOutlineLocationMarker aria-hidden /> {l.nombre}
                          {activo && <HiCheck className="wz-card__check" aria-hidden />}
                        </span>
                        <span className="wz-card__meta">{[l.direccion, l.ciudad].filter(Boolean).join(' · ')}</span>
                        <span className="wz-card__meta">
                          {(l.sectores || []).length} sector(es) · capacidad {formatEntero(l.capacidadTotal ?? 0)}
                        </span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
            <p className="wz-hint">
              Los lugares los carga el administrador.{' '}
              {isAdmin ? <Link to="/gestion/lugares/nuevo">Crear un lugar</Link> : 'Si no encontrás el tuyo, pedile que lo agregue.'}
            </p>
          </>
        )}

        {elegido && (
          <div className="wz-mapa">
            {conMapa ? (
              <iframe title={`Mapa: ${elegido.nombre}`} src={srcMapa(lat, lng)} loading="lazy" referrerPolicy="no-referrer" />
            ) : (
              <p className="wz-aviso">
                <HiOutlineExclamation aria-hidden /> Este lugar no tiene coordenadas, así que en la página del evento no se mostrará el mapa.
              </p>
            )}
            {urlMapaLugar(elegido) && (
              <a href={urlMapaLugar(elegido)} target="_blank" rel="noopener noreferrer">
                Ver en el mapa
              </a>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

/* ----------------------------------------------------------- 3 · Participantes */
export function PasoParticipantes({ form, teloneros, onPrincipalChange, onTeloneroChange, onAddTelonero, onRemoveTelonero, token, loading }) {
  return (
    <div className="wz-participantes">
      <EventoParticipantesFields
        principal={form.principal}
        onPrincipalChange={onPrincipalChange}
        teloneros={teloneros}
        onTeloneroChange={onTeloneroChange}
        onAddTelonero={onAddTelonero}
        onRemoveTelonero={onRemoveTelonero}
        token={token}
        disabled={loading}
      />
    </div>
  )
}

/* ------------------------------------------------------------------ 4 · Entradas */
export function PasoEntradas({ form, sectores, onChange, onRowChange, onPrecioATodos, loading, irAPaso }) {
  const [precioTodos, setPrecioTodos] = useState('')
  const cupo = cupoTotalSectores(sectores)
  const forzada = cupo > LIMITE_ALTA_DEMANDA

  if (!form.idLugar) {
    return (
      <div className="wz-vacio">
        <p>Primero elegí el lugar: de ahí salen los sectores que podés vender.</p>
        <button type="button" className="btn btn-secondary" onClick={() => irAPaso(1)}>
          Elegir lugar
        </button>
      </div>
    )
  }
  if (sectores.length === 0) {
    return <div className="wz-vacio">Este lugar todavía no tiene sectores: el administrador tiene que cargarlos.</div>
  }

  return (
    <div className="wz-stack">
      <div className="wz-toolbar">
        <p className="wz-hint">Tildá los sectores que se venden y poné su precio. En los sectores generales el cupo es opcional (vacío vende todo); los numerados se venden completos.</p>
        <div className="wz-precio-todos">
          <input type="number" min="0" className="form-input" placeholder="Precio" value={precioTodos} onChange={(e) => setPrecioTodos(e.target.value)} aria-label="Precio para todos los sectores elegidos" />
          <button type="button" className="btn btn-secondary btn-sm" disabled={precioTodos === '' || loading} onClick={() => onPrecioATodos(precioTodos)}>
            Mismo precio a todos
          </button>
        </div>
      </div>

      <div className="g-table-wrap">
        <table className="g-table">
          <thead>
            <tr>
              <th>Se vende</th>
              <th>Sector</th>
              <th className="is-num">Capacidad</th>
              <th>Precio (ARS)</th>
              <th>Cupo</th>
            </tr>
          </thead>
          <tbody>
            {sectores.map((r) => (
              <tr key={r.id} className={r.incluido ? '' : 'wz-off'}>
                <td>
                  <input type="checkbox" checked={r.incluido} onChange={(e) => onRowChange(r.id, 'incluido', e.target.checked)} disabled={loading} aria-label={`Vender ${r.nombre}`} />
                </td>
                <td>
                  <div className="g-cell-main">
                    <strong>{r.nombre}</strong>
                    <span>{describirSector(r)}</span>
                  </div>
                </td>
                <td className="is-num">{formatEntero(r.capacidad)}</td>
                <td>
                  <input type="number" min="0" className="form-input" value={r.precio} onChange={(e) => onRowChange(r.id, 'precio', e.target.value)} disabled={loading || !r.incluido} placeholder="0" aria-label={`Precio de ${r.nombre}`} />
                </td>
                <td>
                  {r.esNumerado ? (
                    <span className="g-muted">Completo</span>
                  ) : (
                    <input type="number" min="1" max={r.capacidad} className="form-input" value={r.cupo} onChange={(e) => onRowChange(r.id, 'cupo', e.target.value)} disabled={loading || !r.incluido} placeholder={`Todo (${formatEntero(r.capacidad)})`} aria-label={`Cupo de ${r.nombre}`} />
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="wz-resumen-cupo">
        <span>
          Cupo total a la venta: <strong>{formatEntero(cupo)}</strong>
        </span>
        <label className="gestion-check">
          <input type="checkbox" name="esAltaDemanda" checked={forzada || form.esAltaDemanda} onChange={onChange} disabled={loading || forzada} />
          Venta con fila virtual (alta demanda)
        </label>
      </div>
      {forzada ? (
        <p className="wz-aviso">
          <HiOutlineExclamation aria-hidden /> Con más de {formatEntero(LIMITE_ALTA_DEMANDA)} entradas el evento se vende siempre con fila virtual: los compradores esperan su turno por orden de llegada.
        </p>
      ) : (
        <p className="wz-hint">Activala si esperás mucha demanda: los clientes entran a una fila virtual y compran por tandas.</p>
      )}
    </div>
  )
}

/* ------------------------------------------------------------------ 5 · Revisión */
export function PasoRevision({ form, sectores, teloneros, lugar, irAPaso }) {
  const items = checklistPublicacion(form, sectores)
  const elegidos = sectores.filter((s) => s.incluido)
  const precios = elegidos.filter((s) => String(s.precio).trim() !== '').map((s) => Number(String(s.precio).replace(',', '.'))).filter((n) => Number.isFinite(n))
  const desde = precios.length ? Math.min(...precios) : null

  return (
    <div className="wz-stack">
      <section>
        <h3 className="wz-sub">Para publicar</h3>
        <ul className="wz-checklist">
          {items.map((it) => (
            <li key={it.texto} className={it.ok ? 'is-ok' : 'is-falta'}>
              <span className="wz-checklist__icon" aria-hidden>
                {it.ok ? <HiCheck /> : <HiOutlineExclamation />}
              </span>
              {it.texto}
              {!it.ok && (
                <button type="button" className="wz-link" onClick={() => irAPaso(it.paso)}>
                  Completar
                </button>
              )}
            </li>
          ))}
        </ul>
        <p className="wz-hint">Si todavía falta algo, podés guardarlo como borrador y completarlo después.</p>
      </section>

      <dl className="wz-resumen">
        <div>
          <dt>Evento</dt>
          <dd>
            {form.nombre || '—'} · {EVENTO_TIPO_LABELS[form.tipo] || form.tipo}
          </dd>
        </div>
        <div>
          <dt>Cuándo</dt>
          <dd>{fechaLegible(form.fechaInicio) || 'Sin fecha'}</dd>
        </div>
        <div>
          <dt>Dónde</dt>
          <dd>{lugar ? `${lugar.nombre} · ${lugar.ciudad}` : 'Sin lugar'}</dd>
        </div>
        <div>
          <dt>Artistas</dt>
          <dd>{[form.principal?.nombre, ...teloneros.map((t) => t.nombre)].filter(Boolean).join(' · ') || '—'}</dd>
        </div>
        <div>
          <dt>Entradas</dt>
          <dd>
            {elegidos.length} sector(es){desde != null ? ` · desde ${formatMoney(desde)}` : ''}
            {form.esAltaDemanda || cupoTotalSectores(sectores) > LIMITE_ALTA_DEMANDA ? ' · con fila virtual' : ''}
          </dd>
        </div>
      </dl>
    </div>
  )
}
