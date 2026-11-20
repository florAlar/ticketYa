import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../../hooks/useAuth'
import useConfirm from '../../hooks/useConfirm'
import useAsync from '../../hooks/useAsync'
import {
  actualizarParticipante,
  crearParticipanteAdmin,
  eliminarParticipante,
  listarParticipantes,
} from '../../api/participantes'
import {
  TIPOS_PARTICIPANTE,
  labelTipoParticipante,
  participantePayload,
  participanteVacio,
  validarParticipante,
} from '../../domain/participante'
import { FEATURE, GATE_HINT } from '../../config/featureGates'
import GateButton from '../../components/ui/GateButton'
import { ErrorState, LoadingState } from '../../components/ui/AsyncState'
import { PageHeader, Pill, RowMenu, Toolbar, VacioTabla } from '../../components/gestion/gestionUi'
import './GestionPage.css'

/**
 * Participantes (PAR-01) — solo ADMIN. Listado con búsqueda y filtro por tipo, alta, edición y baja
 * (409 si el participante está en algún evento: en ese caso solo se edita).
 * La banda principal NO es un atributo del participante: es el rol PRINCIPAL/TELONERO que se elige
 * por evento en el formulario del evento.
 */
const GestionParticipantesPage = () => {
  const { token } = useAuth()
  const confirm = useConfirm()
  const wired = FEATURE.participantes
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [form, setForm] = useState(null) // null = cerrado; { id?, ...campos }
  const [saving, setSaving] = useState(false)

  const load = useCallback(
    async (signal) => {
      const data = await listarParticipantes({ q: '', tipo: '' }, { token, signal })
      return Array.isArray(data) ? data : []
    },
    [token]
  )

  const {
    data: items = [],
    status,
    error: loadError,
    reload,
  } = useAsync(load, [token], { enabled: wired })

  const onGuardar = async (e) => {
    e.preventDefault()
    const invalido = validarParticipante(form)
    if (invalido) {
      setError(invalido)
      return
    }
    setSaving(true)
    setError('')
    setAviso('')
    try {
      const payload = participantePayload(form)
      if (form.id) {
        await actualizarParticipante(form.id, payload, { token })
        setAviso(`«${payload.nombre}» actualizado.`)
      } else {
        await crearParticipanteAdmin(payload, { token })
        setAviso(`«${payload.nombre}» creado.`)
      }
      setForm(null)
      await reload()
    } catch (err) {
      setError(
        err.status === 409
          ? 'Ya existe un participante con ese nombre: buscalo en la lista y editalo.'
          : err.message || 'No se pudo guardar'
      )
    } finally {
      setSaving(false)
    }
  }

  const onEliminar = async (p) => {
    const ok = await confirm({
      title: `¿Eliminar a «${p.nombre}» del catálogo?`,
      confirmLabel: 'Eliminar',
      tone: 'danger',
    })
    if (!ok) return
    setBusyId(p.id)
    setError('')
    setAviso('')
    try {
      await eliminarParticipante(p.id, { token })
      setAviso(`«${p.nombre}» eliminado.`)
      await reload()
    } catch (err) {
      setError(
        err.status === 409
          ? `«${p.nombre}» participa en eventos: no se puede eliminar, solo editar.`
          : err.message || 'No se pudo eliminar'
      )
    } finally {
      setBusyId(null)
    }
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const [fq, setFq] = useState('')
  const [fTipo, setFTipo] = useState('')
  const lista = (items || []).filter((p) => {
    if (fTipo && p.tipo !== fTipo) return false
    return !fq || String(p.nombre || '').toLowerCase().includes(fq.trim().toLowerCase())
  })

  return (
    <>
      <PageHeader
        title="Participantes"
        subtitle="Bandas, artistas, elencos, equipos y pilotos. Los organizadores los eligen al armar un evento."
      >
        <GateButton
          enabled={wired}
          type="button"
          className="btn btn-primary"
          onClick={() => {
            setError('')
            setForm(participanteVacio())
          }}
        >
          Nuevo participante
        </GateButton>
      </PageHeader>

      {!wired && <VacioTabla>La gestión de participantes necesita el backend real: no hay versión mock.</VacioTabla>}
      {aviso && <p className="g-ok" role="status">{aviso}</p>}
      {error && <p className="g-error" role="alert">{error}</p>}

      {wired && form && (
        <form className="g-card g-form" style={{ marginBottom: '1.25rem' }} onSubmit={onGuardar}>
          <h2 className="g-card__title" style={{ margin: 0 }}>
            {form.id ? 'Editar participante' : 'Nuevo participante'}
          </h2>
          <div className="g-form__grid">
            <div className="form-group">
              <label htmlFor="par-nombre">Nombre *</label>
              <input id="par-nombre" className="form-input" value={form.nombre} onChange={set('nombre')} maxLength={200} required />
            </div>
            <div className="form-group">
              <label htmlFor="par-tipo">Tipo *</label>
              <select id="par-tipo" className="form-input" value={form.tipo} onChange={set('tipo')}>
                {TIPOS_PARTICIPANTE.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group g-span-2">
              <label htmlFor="par-desc">Descripción</label>
              <textarea id="par-desc" className="form-input" rows={2} value={form.descripcion} onChange={set('descripcion')} />
            </div>
          </div>
          <div className="g-row-actions" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-secondary" onClick={() => setForm(null)} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar'}
            </button>
          </div>
        </form>
      )}

      {status === 'loading' && <LoadingState label="Cargando participantes…" />}
      {status === 'error' && <ErrorState error={loadError} onRetry={reload} />}

      {wired && status === 'success' && (
        <>
          <Toolbar buscar={fq} onBuscar={setFq} placeholder="Buscar por nombre…">
            <select className="form-input" value={fTipo} onChange={(e) => setFTipo(e.target.value)} aria-label="Filtrar por tipo">
              <option value="">Todos los tipos</option>
              {TIPOS_PARTICIPANTE.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </Toolbar>
          {lista.length === 0 ? (
            <VacioTabla>{items.length === 0 ? 'Todavía no hay participantes cargados.' : 'Ningún participante coincide con el filtro.'}</VacioTabla>
          ) : (
            <div className="g-table-wrap">
              <table className="g-table">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Descripción</th>
                    <th className="is-actions">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {lista.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <div className="g-cell-main">
                          <strong>{p.nombre}</strong>
                        </div>
                      </td>
                      <td>
                        <Pill tone="brand">{labelTipoParticipante(p.tipo)}</Pill>
                      </td>
                      <td className="g-muted">{p.descripcion || '—'}</td>
                      <td className="is-actions">
                        <RowMenu
                          label={'Acciones de ' + p.nombre}
                          items={[
                            {
                              label: 'Editar',
                              onClick: () => {
                                setError('')
                                setForm({
                                  id: p.id,
                                  nombre: p.nombre || '',
                                  tipo: p.tipo || 'BANDA',
                                  descripcion: p.descripcion || '',
                                })
                              },
                            },
                            { label: 'Eliminar', danger: true, disabled: busyId === p.id, onClick: () => onEliminar(p) },
                          ]}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  )
}

export default GestionParticipantesPage
