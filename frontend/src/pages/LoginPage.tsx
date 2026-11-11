import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../app/authContext'
import { LoginForm } from '../components/molecules/LoginForm'
import type { LoginCredentials } from '../types/auth'

export function LoginPage() {
  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const redirectPath = getRedirectPath(location.state)

  const handleLogin = async (credentials: LoginCredentials) => {
    await auth.login(credentials)
    navigate(redirectPath, { replace: true })
  }

  return (
    <main className="login-page">
      <section className="login-intro" aria-labelledby="login-title">
        <p className="eyebrow">Process Observability Lab</p>
        <h1 id="login-title">DT Control Power</h1>
        <p>
          Accede al laboratorio para seguir procesos, checkpoints y eventos
          operativos con trazabilidad de extremo a extremo.
        </p>
      </section>

      <section className="login-card" aria-labelledby="login-form-title">
        <p className="label">Acceso controlado</p>
        <h2 id="login-form-title">Iniciar sesión</h2>
        <p className="muted-copy">
          Utiliza una cuenta sintética provisionada para la demostración.
        </p>
        <LoginForm onSubmit={handleLogin} />
      </section>
    </main>
  )
}

function getRedirectPath(state: unknown): string {
  if (typeof state !== 'object' || state === null) return '/'
  const from = (state as Record<string, unknown>).from
  return typeof from === 'string' && from.startsWith('/') ? from : '/'
}
