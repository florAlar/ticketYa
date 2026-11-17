/** Estado del toast de cuentas demo (módulo sin componentes para Fast Refresh). */

const DISMISS_KEY = 'ticketya_demo_toast_dismissed'
export const DEMO_TOAST_REOPEN_EVENT = 'ticketya:demo-toast-reopen'

export function wasDemoToastDismissed() {
  try {
    return sessionStorage.getItem(DISMISS_KEY) === '1'
  } catch {
    return false
  }
}

export function clearDemoToastDismissed() {
  try {
    sessionStorage.removeItem(DISMISS_KEY)
  } catch {
    /* ignore */
  }
}

export function dismissDemoToast() {
  try {
    sessionStorage.setItem(DISMISS_KEY, '1')
  } catch {
    /* ignore */
  }
}

/** Dispara reaparición del toast (p. ej. link en LoginPage). */
export function reopenDemoAccountsToast() {
  clearDemoToastDismissed()
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(DEMO_TOAST_REOPEN_EVENT))
  }
}
