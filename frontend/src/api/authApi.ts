import { apiRequest, clearCsrfToken } from './client'
import type { AuthUser, LoginCredentials } from '../types/auth'
import { isAuthUser } from '../types/auth'

export const authApi = {
  async login(credentials: LoginCredentials): Promise<AuthUser> {
    const payload = await apiRequest<unknown>(
      '/auth/login',
      {
        method: 'POST',
        body: JSON.stringify(credentials),
      },
      { csrf: true },
    )
    return requireAuthUser(payload)
  },

  async me(signal?: AbortSignal): Promise<AuthUser> {
    const payload = await apiRequest<unknown>('/auth/me', { signal })
    return requireAuthUser(payload)
  },

  async logout(): Promise<void> {
    try {
      await apiRequest<void>('/auth/logout', { method: 'POST' }, { csrf: true })
    } finally {
      clearCsrfToken()
    }
  },
}

function requireAuthUser(payload: unknown): AuthUser {
  if (!isAuthUser(payload)) {
    throw new Error('El backend devolvió una sesión inválida')
  }
  return payload
}
