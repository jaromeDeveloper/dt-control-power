export type UserRole = 'ADMIN' | 'OPERATOR' | 'SUPERVISOR' | 'AUDITOR'

export interface AuthUser {
  id: string
  username: string
  displayName: string
  role: UserRole
  areaCodes: string[]
}

export interface LoginCredentials {
  username: string
  password: string
}

export function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== 'object' || value === null) return false

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.id === 'string' &&
    typeof candidate.username === 'string' &&
    typeof candidate.displayName === 'string' &&
    isUserRole(candidate.role) &&
    Array.isArray(candidate.areaCodes) &&
    candidate.areaCodes.every((areaCode) => typeof areaCode === 'string')
  )
}

function isUserRole(value: unknown): value is UserRole {
  return (
    value === 'ADMIN' ||
    value === 'OPERATOR' ||
    value === 'SUPERVISOR' ||
    value === 'AUDITOR'
  )
}
