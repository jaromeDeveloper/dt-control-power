import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../api/client'
import type { LoginCredentials } from '../../types/auth'
import { Button } from '../atoms/Button'
import { TextField } from '../atoms/TextField'

interface LoginFormProps {
  onSubmit: (credentials: LoginCredentials) => Promise<void>
}

export function LoginForm({ onSubmit }: LoginFormProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSubmitting) return

    setIsSubmitting(true)
    setErrorMessage('')
    try {
      await onSubmit({ username: username.trim(), password })
    } catch (error) {
      setErrorMessage(toLoginMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="login-form" onSubmit={handleSubmit} noValidate>
      <TextField
        id="username"
        name="username"
        label="Usuario"
        autoComplete="username"
        value={username}
        onChange={(event) => setUsername(event.target.value)}
        required
        disabled={isSubmitting}
      />
      <TextField
        id="password"
        name="password"
        type="password"
        label="Contraseña"
        autoComplete="current-password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        required
        disabled={isSubmitting}
      />

      {errorMessage && (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      )}

      <Button type="submit" disabled={isSubmitting || !username || !password}>
        {isSubmitting ? 'Iniciando sesión…' : 'Iniciar sesión'}
      </Button>
    </form>
  )
}

function toLoginMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Usuario o contraseña incorrectos.'
    if (error.status === 403) return 'La sesión no pudo validarse. Intenta de nuevo.'
    return error.message
  }
  return error instanceof Error
    ? error.message
    : 'No fue posible conectar con el backend.'
}
