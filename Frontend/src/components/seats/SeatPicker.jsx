import { useEffect, useMemo, useRef, useState } from 'react'
import { HiChevronLeft, HiChevronRight } from 'react-icons/hi'
import {
  codigoAsiento,
  compararAsientos,
  rotuloFila,
} from '../../domain/asiento'
import './SeatPicker.css'

const GAP = 6
const MIN_SEAT = 22
const MAX_SEAT = 46

/**
 * Selector de asientos de un sector numerado, en dos pasos:
 *   1) elegir una fila (selector con asientos libres por fila y flechas para recorrerlas),
 *   2) elegir asientos de esa fila (a lo sumo `asientosPorFila`, nunca el sector entero).
 * Así un sector de 25.000 asientos no dibuja 25.000 botones.
 *
 * El tamaño de los asientos se calcula para que toda la fila entre en el espacio disponible
 * (sin scroll): ocupa el alto y el ancho que le deja el contenedor.
 *
 * Los asientos son virtuales: `ocupados` trae solo los tomados (RESERVADO|VENDIDO); el resto del
 * layout está libre. La selección (de cualquier fila) llega hasta `max`; el resumen (chips y total)
 * lo muestra quien usa el componente.
 *
 * @param {object} props
 * @param {number} props.filas
 * @param {number} props.asientosPorFila
 * @param {{fila:string,numero:number,estado?:string}[]} [props.ocupados]
 * @param {{fila:string,numero:number}[]} props.seleccion
 * @param {(sel:{fila:string,numero:number}[]) => void} props.onChange
 * @param {number} props.max
 * @param {boolean} [props.disabled]
 * @param {import('react').ReactNode} [props.acciones] botones extra junto al selector de fila
 */
const SeatPicker = ({
  filas,
  asientosPorFila,
  ocupados = [],
  seleccion,
  onChange,
  max,
  disabled = false,
  acciones = null,
}) => {
  const [filaActiva, setFilaActiva] = useState('A')
  const areaRef = useRef(null)
  const [seatSize, setSeatSize] = useState(36)

  const rotulos = useMemo(
    () => Array.from({ length: filas }, (_, i) => rotuloFila(i + 1)),
    [filas]
  )
  const filaVisible = rotulos.includes(filaActiva) ? filaActiva : rotulos[0]
  const idxFila = rotulos.indexOf(filaVisible)

  /** Mayor tamaño de asiento con el que toda la fila entra en el área disponible. */
  useEffect(() => {
    const el = areaRef.current
    if (!el) return undefined
    const medir = () => {
      const w = el.clientWidth
      const h = el.clientHeight
      if (!w || !h) return
      for (let s = MAX_SEAT; s >= MIN_SEAT; s -= 1) {
        const cols = Math.max(1, Math.floor((w + GAP) / (s + GAP)))
        const rows = Math.ceil(asientosPorFila / cols)
        if (rows * (s + GAP) - GAP <= h) {
          setSeatSize(s)
          return
        }
      }
      setSeatSize(MIN_SEAT)
    }
    medir()
    const ro = new ResizeObserver(medir)
    ro.observe(el)
    return () => ro.disconnect()
  }, [asientosPorFila])

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

  const libresEnFila = (rotulo) =>
    asientosPorFila - (tomados.get(rotulo)?.size ?? 0)

  const irAFila = (delta) => {
    const next = rotulos[Math.min(rotulos.length - 1, Math.max(0, idxFila + delta))]
    if (next) setFilaActiva(next)
  }

  return (
    <div className="seat-picker" style={{ '--seat-size': `${seatSize}px` }}>
      <div className="seat-picker__bar">
        <div className="seat-picker__row-select">
          <button
            type="button"
            className="seat-picker__nav"
            onClick={() => irAFila(-1)}
            disabled={disabled || idxFila <= 0}
            aria-label="Fila anterior"
          >
            <HiChevronLeft aria-hidden />
          </button>
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
          <button
            type="button"
            className="seat-picker__nav"
            onClick={() => irAFila(1)}
            disabled={disabled || idxFila >= rotulos.length - 1}
            aria-label="Fila siguiente"
          >
            <HiChevronRight aria-hidden />
          </button>
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
        {acciones}
      </div>

      <div className="seat-picker__stage" aria-hidden>
        Escenario / campo
      </div>

      <div className="seat-picker__area" ref={areaRef}>
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
      </div>

      {llena && (
        <p className="seat-picker__hint seat-picker__max">
          Llegaste al máximo de {max} asientos por compra.
        </p>
      )}
    </div>
  )
}

export default SeatPicker
