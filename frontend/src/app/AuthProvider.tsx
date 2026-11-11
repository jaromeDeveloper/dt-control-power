import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { PropsWithChildren } from 'react'
import { authApi } from '../api/authApi'
import { ApiError, onUnauthorized } from '../api/client'
import type { LoginCredentials } from '../types/auth'
import { AuthContext } from './authContext'
import type { AuthContextValue, AuthState } from './authContext'

export function AuthProvider({ children }: PropsWithChildren) {
  const [state, setState] = useState<AuthState>({
    status: 'loading',
    user: null,
  })

  useEffect(() => {
    return onUnauthorized(() => {
      setState({ status: 'anonymous', user: null })
    })
  }, [])

  useEffect(() => {
    const controller = new AbortController()

    void authApi
      .me(controller.signal)
      .then((user) => setState({ status: 'authenticated', user }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        if (error instanceof ApiError && error.status === 401) {
          setState({ status: 'anonymous', user: null })
          return
        }
        setState({
          status: 'error',
          user: null,
          message:
            error instanceof Error
              ? error.message
              : 'No fue posible validar la sesión',
        })
      })

    return () => controller.abort()
  }, [])

  const login = useCallback(async (credentials: LoginCredentials) => {
    const user = await authApi.login(credentials)
    setState({ status: 'authenticated', user })
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error
    } finally {
      setState({ status: 'anonymous', user: null })
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, login, logout }),
    [state, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
