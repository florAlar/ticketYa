import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { DEMO_ACCOUNTS, ROLE_LABELS } from '../../auth/roles'
import { USE_MOCK } from '../../api/config'
import './DemoAccountsToast.css'

const DISMISS_KEY = 'ticketya_demo_toast_dismissed'

/**
 * Toast flotante con cuentas mock (portal → body).
 * Lista abierta por defecto; se puede colapsar o cerrar.
 */
export default function DemoAccountsToast() {
  const [open, setOpen] = useState(true)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (!USE_MOCK) return
    try {
      if (sessionStorage.getItem(DISMISS_KEY) === '1') return
    } catch {
      /* ignore */
    }
    setVisible(true)
  }, [])

  const dismiss = useCallback((e) => {
    e?.stopPropagation?.()
    setVisible(false)
    setOpen(false)
    try {
      sessionStorage.setItem(DISMISS_KEY, '1')
    } catch {
      /* ignore */
    }
  }, [])

  const toggle = useCallback((e) => {
    e.preventDefault()
    e.stopPropagation()
    setOpen((v) => !v)
  }, [])

  if (!USE_MOCK || !visible || typeof document === 'undefined') return null

  return createPortal(
    <aside
      className={`demo-toast ${open ? 'is-open' : ''}`}
      role="complementary"
      aria-label="Cuentas de prueba"
    >
      <div className="demo-toast__bar">
        <button
          type="button"
          className="demo-toast__toggle"
          aria-expanded={open}
          onClick={toggle}
        >
          <span className="demo-toast__dot" aria-hidden />
          Cuentas demo
          <span className="demo-toast__chev" aria-hidden>
            {open ? '▾' : '▸'}
          </span>
        </button>
        <button
          type="button"
          className="demo-toast__close"
          onClick={dismiss}
          aria-label="Cerrar ayuda demo"
        >
          ×
        </button>
      </div>

      {open ? (
        <ul className="demo-toast__list">
          {DEMO_ACCOUNTS.map((acc) => (
            <li key={acc.email} className="demo-toast__item">
              <span className="demo-toast__role">
                {ROLE_LABELS[acc.rol] || acc.rol}
              </span>
              <span className="demo-toast__creds">
                {acc.email}
                <span className="demo-toast__sep">·</span>
                {acc.password}
              </span>
            </li>
          ))}
        </ul>
      ) : null}
    </aside>,
    document.body
  )
}
