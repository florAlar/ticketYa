import { useMemo, useState } from 'react'
import {
  codigoAsiento,
  compararAsientos,
  rotuloFila,
} from '../../domain/asiento'
import './SeatPicker.css'

/**
 * Selector de asientos de un sector numerado, en dos pasos:
 *   1) elegir una fila (lista con asientos libres por fila),
 *   2) elegir asientos de esa fila (a lo sumo `asientosPorFila`, nunca el sector entero).
 * Así un sector de 25.000 asientos no dibuja 25.000 botones.
 *
 * Los asientos son virtuales: `ocupados` trae solo los tomados (RESERVADO|VENDIDO); el resto del
 * layout está libre. La selección (de cualquier fila) se muestra como chips y llega hasta `max`.
 *
 * @param {object} props
 * @param {number} props.filas
 * @param {number} props.asientosPorFila
 * @param {{fila:string,numero:number,estado?:string}[]} [props.ocupados]
 * @param {{fila:string,numero:number}[]} props.seleccion
 * @param {(sel:{fila:string,numero:number}[]) => void} props.onChange
 * @param {number} props.max
 * @param {boolean} [props.disabled]
 */
const SeatPicker = ({
  filas,
  asientosPorFila,
  ocupados = [],
  seleccion,
  onChange,
  max,
  disabled = false,
}) => {
  const [filaActiva, setFilaActiva] = useState('A')

  const rotulos = useMemo(
    () => Array.from({ length: filas }, (_, i) => rotuloFila(i + 1)),
    [filas]
  )
  const filaVisible = rotulos.includes(filaActiva) ? filaActiva : rotulos[0]

  /** fila → (número → estado) de asientos tomados. */
  const tomados = useMemo(() => {
    const m = new Map()
    for (const a of ocupados) {
      if (!m.has(a.fila)) m.set(a.fila, new Map())
      m.get(a.fila).set(Number(a.numero), a.estado || 'OCUPADO')
    }
    return m
  }, [ocupados])

  const seleccionSet = useMemo(
    () => new Set(seleccion.map((a) => codigoAsiento(a.fila, a.numero))),
    [seleccion]
  )

  const tomadosDeFila = tomados.get(filaVisible)
  const llena = seleccion.length >= max

  const toggle = (numero) => {
    if (disabled) return
    const key = codigoAsiento(filaVisible, numero)
    if (seleccionSet.has(key)) {
      onChange(
        seleccion.filter((a) => codigoAsiento(a.fila, a.numero) !== key)
      )
      return
    }
    if (llena) return
    onChange([...seleccion, { fila: filaVisible, numero }].sort(compararAsientos))
  }

  const quitar = (a) => {
    if (disabled) return
    onChange(
      seleccion.filter(
        (x) => codigoAsiento(x.fila, x.numero) !== codigoAsiento(a.fila, a.numero)
      )
    )
  }

  const libresEnFila = (rotulo) =>
    asientosPorFila - (tomados.get(rotulo)?.size ?? 0)

  return (
    <div className="seat-picker">
      <div className="seat-picker__row-select">
        <label htmlFor="seat-picker-fila">Fila</label>
        <select
          id="seat-picker-fila"
          className="form-input"
          value={filaVisible}
          onChange={(e) => setFilaActiva(e.target.value)}
          disabled={disabled}
        >
          {rotulos.map((r) => (
            <option key={r} value={r}>
              Fila {r} — {libresEnFila(r)} libres
            </option>
          ))}
        </select>
        <span className="seat-picker__hint">
          {filas} filas × {asientosPorFila} asientos
        </span>
      </div>

      <ul className="seat-picker__legend" aria-label="Referencias">
        <li>
          <span className="seat seat--legend" /> Libre
        </li>
        <li>
          <span className="seat seat--legend seat--sel" /> Elegido
        </li>
        <li>
          <span className="seat seat--legend seat--ocupado" /> Ocupado
        </li>
      </ul>

      <div
        className="seat-picker__grid"
        role="group"
        aria-label={`Asientos de la fila ${filaVisible}`}
      >
        {Array.from({ length: asientosPorFila }, (_, i) => i + 1).map((n) => {
          const estado = tomadosDeFila?.get(n)
          const elegido = seleccionSet.has(codigoAsiento(filaVisible, n))
          const bloqueado = Boolean(estado) || (llena && !elegido)
          const cls = [
            'seat',
            estado ? 'seat--ocupado' : '',
            elegido ? 'seat--sel' : '',
          ]
            .filter(Boolean)
            .join(' ')
          return (
            <button
              key={n}
              type="button"
              className={cls}
              disabled={disabled || bloqueado}
              aria-pressed={elegido}
              aria-label={`Fila ${filaVisible} asiento ${n}${
                estado ? ' ocupado' : elegido ? ' elegido' : ' libre'
              }`}
              title={estado ? 'Ocupado' : `${filaVisible}-${n}`}
              onClick={() => toggle(n)}
            >
              {n}
            </button>
          )
        })}
      </div>

      <div className="seat-picker__chosen">
        <strong>
          Elegidos ({seleccion.length}/{max}):
        </strong>
        {seleccion.length === 0 ? (
          <span className="seat-picker__hint"> ninguno todavía</span>
        ) : (
          <ul className="seat-picker__chips">
            {seleccion.map((a) => (
              <li key={codigoAsiento(a.fila, a.numero)}>
                <button
                  type="button"
                  className="seat-picker__chip"
                  onClick={() => quitar(a)}
                  disabled={disabled}
                  title="Quitar"
                >
                  {codigoAsiento(a.fila, a.numero)} ×
                </button>
              </li>
            ))}
          </ul>
        )}
        {llena && (
          <p className="seat-picker__hint">
            Llegaste al máximo de {max} asientos por compra.
          </p>
        )}
      </div>
    </div>
  )
}

export default SeatPicker
