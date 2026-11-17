import { useCallback, useEffect, useMemo, useState } from 'react'
import AuthContext from '../context/AuthContext'
import * as authApi from '../api/auth'
import { AUTH_EXPIRED_EVENT } from '../api/http'
import {
  ROLES,
  canBuyTickets,
  canManageAllEvents,
  canManageEvents,
  canManageSalesOps,
  canManageStaff,
  canOperatePlatform,
  canScanAccess,
  normalizeRol,
} from '../auth/roles'

const TOKEN_KEY = 'ticketya_token'
const USER_KEY = 'ticketya_user'

function readStoredToken() {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

function readStoredUser() {
  try {
    const raw = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
    if (!raw) return null
    return { ...raw, rol: normalizeRol(raw.rol) }
  } catch {
    return null
  }
}

export default function AuthProvider({ children }) {
  const [token, setToken] = useState(readStoredToken)
  const [user, setUser] = useState(readStoredUser)
  // Si hay token guardado, validamos la sesión contra /usuarios/me antes de
  // dejar pasar a rutas protegidas (evita tokens vencidos "fantasma").
  const [initializing, setInitializing] = useState(() => Boolean(readStoredToken()))

  const persist = useCallback((nextToken, nextUser) => {
    const normalized = nextUser
      ? { ...nextUser, rol: normalizeRol(nextUser.rol) }
      : null
    setToken(nextToken)
    setUser(normalized)
    setInitializing(false)
    try {
      if (nextToken) localStorage.setItem(TOKEN_KEY, nextToken)
      else localStorage.removeItem(TOKEN_KEY)
      if (normalized) localStorage.setItem(USER_KEY, JSON.stringify(normalized))
      else localStorage.removeItem(USER_KEY)
    } catch {
      /* storage lleno o bloqueado: la sesión sigue en memoria */
    }
  }, [])

  // Rehidratación: token presente → confirmar identidad y rol reales.
  useEffect(() => {
    if (!token) return undefined

    let cancelled = false

    ;(async () => {
      try {
        const me = await authApi.getMe({ token })
        if (!cancelled) {
          persist(token, me)
          setInitializing(false)
        }
      } catch (err) {
        if (cancelled) return
        // Token inválido/expirado → limpiar. Error de red → mantener sesión
        // local para no expulsar al usuario si el gateway está caído.
        if (err?.status === 401 || err?.status === 403) {
          persist(null, null)
        }
        setInitializing(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [token, persist])

  // Un 401 en cualquier request autenticada cierra la sesión de forma global.
  useEffect(() => {
    const onExpired = () => {
      persist(null, null)
      setInitializing(false)
    }
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired)
  }, [persist])

  const login = useCallback(
    async (credentials) => {
      const data = await authApi.login(credentials)
      persist(data.token, data.user)
      return data.user
    },
    [persist]
  )

  const register = useCallback(
    async (payload) => {
      const data = await authApi.register(payload)
      persist(data.token, data.user)
      return data.user
    },
    [persist]
  )

  const logout = useCallback(async () => {
    try {
      if (token) await authApi.logout({ token })
    } catch {
      /* mock o red: igual se limpia sesión local */
    }
    persist(null, null)
  }, [persist, token])

  /** Fusiona campos de perfil en la sesión local (post PATCH /usuarios/me). */
  const updateUser = useCallback(
    (partial) => {
      if (!token || !partial) return null
      const base = user || {}
      const next = {
        ...base,
        ...partial,
        id: partial.id ?? base.id,
        email: partial.email ?? base.email,
        rol: normalizeRol(partial.rol ?? base.rol),
      }
      persist(token, next)
      return next
    },
    [persist, token, user]
  )

  const value = useMemo(() => {
    const rol = user ? normalizeRol(user.rol) : null
    return {
      token,
      user,
      rol,
      initializing,
      isAuthenticated: Boolean(token),
      isAdmin: rol === ROLES.ADMIN,
      isOrganizador: rol === ROLES.ORGANIZADOR,
      isCliente: rol === ROLES.CLIENTE,
      isAcceso: rol === ROLES.ACCESO,
      canManageEvents: canManageEvents(user),
      canManageAllEvents: canManageAllEvents(user),
      canManageStaff: canManageStaff(user),
      canManageSalesOps: canManageSalesOps(user),
      canOperatePlatform: canOperatePlatform(user),
      canScanAccess: canScanAccess(user),
      canBuyTickets: canBuyTickets(user),
      login,
      register,
      logout,
      updateUser,
    }
  }, [token, user, initializing, login, register, logout, updateUser])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
