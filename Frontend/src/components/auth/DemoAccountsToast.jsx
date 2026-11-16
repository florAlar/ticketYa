import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { DEMO_ACCOUNTS, ROLE_LABELS } from '../../auth/roles'
import { USE_MOCK } from '../../api/config'
import './DemoAccountsToast.css'

const DISMISS_KEY = 'ticketya_demo_toast_dismissed'
export const DEMO_TOAST_REOPEN_EVENT = 'ticketya:demo-toast-reopen'

function wasDismissed() {
  try {
    return sessionStorage.getItem(DISMISS_KEY) === '1'
  } catch {
    return false
  }
}

function clearDismissed() {
  try {
    sessionStorage.removeItem(DISMISS_KEY)
  } catch {
    /* ignore */
  }
}

/**
 * Toast flotante con cuentas demo/seed (portal → body).
 * Mismas credenciales mock FE y seed MySQL (demo1234 / org1234 / …).
 * Siempre disponible en /login (no depende de VITE_USE_MOCK).
 * Lista abierta por defecto; se puede colapsar, cerrar o reabrir.
 */
export default function DemoAccountsToast() {
  const [open, setOpen] = useState(true)
  const [visible, setVisible] = useState(() => !wasDismissed())

  useEffect(() => {
    const onReopen = () => {
      clearDismissed()
      setVisible(true)
      setOpen(true)
    }
    window.addEventListener(DEMO_TOAST_REOPEN_EVENT, onReopen)
    return () => window.removeEventListener(DEMO_TOAST_REOPEN_EVENT, onReopen)
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

  if (!visible || typeof document === 'undefined') return null

  const modeHint = USE_MOCK
    ? 'Mock local (sin gateway)'
    : 'Seed gateway / MySQL'

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
        <>
          <p className="demo-toast__hint">{modeHint}</p>
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
        </>
      ) : null}
    </aside>,
    document.body
  )
}

/** Dispara reaparición del toast (p. ej. link en LoginPage). */
export function reopenDemoAccountsToast() {
  clearDismissed()
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(DEMO_TOAST_REOPEN_EVENT))
  }
}
