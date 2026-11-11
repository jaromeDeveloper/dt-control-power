import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { useAuth } from '../../app/authContext'
import { Button } from '../atoms/Button'

export function AppShell() {
  const auth = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')

  if (auth.status !== 'authenticated') return null

  const handleLogout = async () => {
    setIsLoggingOut(true)
    setLogoutError('')
    try {
      await auth.logout()
    } catch (error) {
      setLogoutError(
        error instanceof ApiError
          ? error.message
          : 'No fue posible cerrar la sesión.',
      )
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="app-layout">
      <header className="app-header">
        <div>
          <p className="app-kicker">Process Observability Lab</p>
          <span className="app-name">DT Control Power</span>
        </div>
        <div className="session-summary">
          <div>
            <strong>{auth.user.displayName}</strong>
            <span>{auth.user.role}</span>
          </div>
          <Button
            variant="secondary"
            type="button"
            onClick={() => void handleLogout()}
            disabled={isLoggingOut}
          >
            {isLoggingOut ? 'Cerrando…' : 'Cerrar sesión'}
          </Button>
        </div>
        {logoutError && (
          <p className="header-error" role="alert">
            {logoutError}
          </p>
        )}
      </header>

      <nav className="app-nav" aria-label="Navegación principal">
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        {auth.user.role !== 'AUDITOR' && (
          <NavLink to="/upload">Cargar archivo</NavLink>
        )}
        <NavLink to="/processes">Procesos</NavLink>
        {auth.user.role !== 'OPERATOR' && (
          <NavLink to="/audit">Auditoría</NavLink>
        )}
      </nav>

      <Outlet />
    </div>
  )
}
