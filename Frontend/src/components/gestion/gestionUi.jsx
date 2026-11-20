import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { HiDotsVertical, HiOutlineSearch } from 'react-icons/hi'

/** Encabezado de página: título, descripción corta y acciones (botón principal a la derecha). */
export function PageHeader({ title, subtitle, children }) {
  return (
    <header className="g-head">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {children && <div className="g-head__actions">{children}</div>}
    </header>
  )
}

/** Etiqueta de estado. tone: success | warning | danger | info | neutral | brand */
export function Pill({ tone = 'neutral', children }) {
  return <span className={`g-pill g-pill--${tone}`}>{children}</span>
}

/** Barra de herramientas de una tabla: búsqueda + filtros (children) + acciones a la derecha. */
export function Toolbar({ buscar, onBuscar, placeholder = 'Buscar…', children, derecha }) {
  return (
    <div className="g-toolbar">
      {onBuscar && (
        <label className="g-search">
          <HiOutlineSearch aria-hidden />
          <input
            type="search"
            value={buscar}
            onChange={(e) => onBuscar(e.target.value)}
            placeholder={placeholder}
            aria-label={placeholder}
          />
        </label>
      )}
      {children}
      {derecha && <div className="g-toolbar__right">{derecha}</div>}
    </div>
  )
}

/** Menú "⋯" de acciones por fila. items: { label, onClick?, to?, danger?, disabled?, title? } */
export function RowMenu({ items, label = 'Acciones' }) {
  const [abierto, setAbierto] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    if (!abierto) return undefined
    const cerrar = (e) => {
      if (!ref.current?.contains(e.target)) setAbierto(false)
    }
    const esc = (e) => e.key === 'Escape' && setAbierto(false)
    document.addEventListener('mousedown', cerrar)
    document.addEventListener('keydown', esc)
    return () => {
      document.removeEventListener('mousedown', cerrar)
      document.removeEventListener('keydown', esc)
    }
  }, [abierto])

  const visibles = items.filter(Boolean)
  if (!visibles.length) return null
  return (
    <div className="g-menu" ref={ref}>
      <button
        type="button"
        className="g-menu__btn"
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-label={label}
        onClick={() => setAbierto((v) => !v)}
      >
        <HiDotsVertical aria-hidden />
      </button>
      {abierto && (
        <ul className="g-menu__list" role="menu">
          {visibles.map((it) => (
            <li key={it.label} role="none">
              {it.to && !it.disabled ? (
                <Link role="menuitem" to={it.to} state={it.state} onClick={() => setAbierto(false)} className="g-menu__item">
                  {it.label}
                </Link>
              ) : (
                <button
                  type="button"
                  role="menuitem"
                  className={`g-menu__item${it.danger ? ' is-danger' : ''}`}
                  disabled={it.disabled}
                  title={it.title}
                  onClick={() => {
                    setAbierto(false)
                    it.onClick?.()
                  }}
                >
                  {it.label}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** Estado vacío de una tabla. */
export function VacioTabla({ children }) {
  return <div className="g-empty">{children}</div>
}
