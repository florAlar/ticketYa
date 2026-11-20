import { useState } from 'react'
import { HiCheck } from 'react-icons/hi'
import useAsync from '../../../../hooks/useAsync'
import { listLugares } from '../../../../api/lugares'
import { validateEventoGestionForm } from '../../../../domain/evento'
import EventoPreview from './EventoPreview'
import { PasoBasicos, PasoEntradas, PasoFechaLugar, PasoParticipantes, PasoRevision } from './pasos'
import { checklistPublicacion } from './wizardUtils'
import './eventoWizard.css'

const PASOS = ['Lo básico', 'Fecha y lugar', 'Artistas', 'Entradas', 'Revisión']
const ULTIMO = PASOS.length - 1

/** Valida solo lo que pide el paso actual; devuelve el mensaje de error o ''. */
function validarPaso(paso, w) {
  const { form, teloneros, sectores } = w
  if (paso === 0) {
    if (form.nombre.trim().length < 2) return 'Escribí el nombre del evento.'
    const url = form.imagenUrl.trim()
    if (url && (!/^(https?:\/\/|\/(?!\/))/i.test(url) || url.length > 500)) {
      return 'La portada tiene que ser una URL que empiece con http:// o https:// (hasta 500 caracteres).'
    }
  }
  if (paso === 2) {
    if (!form.principal?.id) return 'Elegí el artista o banda principal del catálogo.'
    const i = teloneros.findIndex((t) => t.nombre?.trim() && !t.id)
    if (i >= 0) return `Telonero ${i + 1}: elegilo del catálogo (hace falta que exista).`
  }
  if (paso === 3) {
    const r = validateEventoGestionForm({ ...form, principalId: form.principal?.id, teloneros }, sectores, 'borrador')
    if (!r.ok) return r.error
  }
  return ''
}

/**
 * Asistente de alta de evento: 5 pasos con vista previa en vivo.
 * `w` es el estado de `useEventoNuevoForm`.
 */
export default function EventoWizard({ w, token, isAdmin }) {
  const [maxPaso, setMaxPaso] = useState(w.paso)
  const [errorPaso, setErrorPaso] = useState('')
  const { paso, setPaso, form, sectores, teloneros, loading } = w
  const alcanzado = Math.max(maxPaso, paso)

  const { data: lugares = [], status: lugaresStatus, error: lugaresError, reload: recargarLugares } = useAsync(
    (signal) => listLugares({}, { token, signal }),
    [token],
    { enabled: Boolean(token) }
  )
  const lugar = lugares.find((l) => l.id === form.idLugar) || null

  const ir = (n) => {
    setErrorPaso('')
    w.setError('')
    setPaso(n)
    setMaxPaso((m) => Math.max(m, n))
  }

  const siguiente = () => {
    const err = validarPaso(paso, w)
    if (err) {
      setErrorPaso(err)
      return
    }
    ir(Math.min(ULTIMO, paso + 1))
  }

  const alPasoClick = (n) => {
    // Hacia atrás siempre; hacia adelante solo hasta donde ya se llegó
    if (n <= alcanzado) ir(n)
  }

  const guardarBorrador = async () => {
    const err = validarPaso(0, w) || (form.principal?.id ? '' : 'Para guardar el borrador elegí el artista principal (paso 3).')
    if (err) {
      setErrorPaso(err)
      return
    }
    setErrorPaso('')
    await w.enviar(false)
  }

  const publicar = async () => {
    setErrorPaso('')
    await w.enviar(true)
  }

  const publicable = checklistPublicacion(form, sectores).every((i) => i.ok)
  const mensaje = errorPaso || w.error

  return (
    <div className="wz">
      <ol className="wz-steps" aria-label="Pasos del alta">
        {PASOS.map((nombre, i) => {
          const estado = i < paso ? 'done' : i === paso ? 'current' : 'todo'
          return (
            <li key={nombre}>
              <button type="button" className={`wz-step wz-step--${estado}`} onClick={() => alPasoClick(i)} disabled={i > alcanzado} aria-current={estado === 'current' ? 'step' : undefined}>
                <span className="wz-step__dot">{estado === 'done' ? <HiCheck aria-hidden /> : i + 1}</span>
                <span className="wz-step__label">{nombre}</span>
              </button>
            </li>
          )
        })}
      </ol>

      <div className="wz-body">
        <section className="wz-panel g-card">
          {paso === 0 && <PasoBasicos form={form} onChange={w.onChange} loading={loading} />}
          {paso === 1 && (
            <PasoFechaLugar
              form={form}
              onChange={w.onChange}
              loading={loading}
              lugares={lugares}
              lugaresStatus={lugaresStatus}
              lugaresError={lugaresError}
              recargarLugares={recargarLugares}
              onElegirLugar={w.onElegirLugar}
              isAdmin={isAdmin}
            />
          )}
          {paso === 2 && (
            <PasoParticipantes
              form={form}
              teloneros={teloneros}
              onPrincipalChange={w.onPrincipalChange}
              onTeloneroChange={w.onTeloneroChange}
              onAddTelonero={w.onAddTelonero}
              onRemoveTelonero={w.onRemoveTelonero}
              token={token}
              loading={loading}
            />
          )}
          {paso === 3 && (
            <PasoEntradas form={form} sectores={sectores} onChange={w.onChange} onRowChange={w.onRowChange} onPrecioATodos={w.onPrecioATodos} loading={loading} irAPaso={ir} />
          )}
          {paso === 4 && <PasoRevision form={form} sectores={sectores} teloneros={teloneros} lugar={lugar} irAPaso={ir} />}

          {mensaje && (
            <p className="g-error" role="alert">
              {mensaje}
            </p>
          )}

          <div className="wz-nav">
            <button type="button" className="btn btn-secondary" onClick={() => ir(Math.max(0, paso - 1))} disabled={paso === 0 || loading}>
              Atrás
            </button>
            <span className="wz-nav__spacer" />
            <button type="button" className="btn btn-secondary" onClick={guardarBorrador} disabled={loading}>
              {loading ? 'Guardando…' : 'Guardar borrador'}
            </button>
            {paso < ULTIMO ? (
              <button type="button" className="btn btn-primary" onClick={siguiente} disabled={loading}>
                Siguiente
              </button>
            ) : (
              <button type="button" className="btn btn-primary" onClick={publicar} disabled={loading || !publicable} title={publicable ? undefined : 'Completá los puntos pendientes para publicar'}>
                Publicar evento
              </button>
            )}
          </div>
        </section>

        <EventoPreview form={form} sectores={sectores} lugar={lugar} />
      </div>
    </div>
  )
}
