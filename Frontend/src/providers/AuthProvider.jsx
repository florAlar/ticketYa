import { useCallback, useMemo, useState } from 'react'
import AuthContext from '../context/AuthContext'
import * as authApi from '../api/auth'
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
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(readStoredUser)

  const persist = useCallback((nextToken, nextUser) => {
    const normalized = nextUser
      ? { ...nextUser, rol: normalizeRol(nextUser.rol) }
      : null
    setToken(nextToken)
    setUser(normalized)
    if (nextToken) localStorage.setItem(TOKEN_KEY, nextToken)
    else localStorage.removeItem(TOKEN_KEY)
    if (normalized) localStorage.setItem(USER_KEY, JSON.stringify(normalized))
    else localStorage.removeItem(USER_KEY)
  }, [])

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

  const value = useMemo(() => {
    const rol = user ? normalizeRol(user.rol) : null
    return {
      token,
      user,
      rol,
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
    }
  }, [token, user, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
