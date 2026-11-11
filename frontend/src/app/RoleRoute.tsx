import { Navigate, Outlet } from 'react-router-dom'
import type { UserRole } from '../types/auth'
import { useAuth } from './authContext'

export function RoleRoute({ allowed }: { allowed: readonly UserRole[] }) {
  const auth = useAuth()

  if (auth.status !== 'authenticated' || !allowed.includes(auth.user.role)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
