import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './authContext'

export function ProtectedRoute() {
  const auth = useAuth()
  const location = useLocation()

  if (auth.status === 'loading') {
    return (
      <main className="centered-state" aria-live="polite">
        <p>Validando sesión…</p>
      </main>
    )
  }

  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (auth.status === 'error') {
    return (
      <main className="centered-state" role="alert">
        <p>{auth.message}</p>
        <button type="button" onClick={() => window.location.reload()}>
          Reintentar
        </button>
      </main>
    )
  }

  return <Outlet />
}
