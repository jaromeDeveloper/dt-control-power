import { useEffect, useState } from 'react'
import { healthApi } from '../../api/healthApi'
import type { HealthResponse } from '../../types/health'
import { Button } from '../atoms/Button'
import { StatusBadge } from '../atoms/StatusBadge'

type HealthState =
  | { status: 'LOADING'; data: null; message: string }
  | { status: 'UP'; data: HealthResponse; message: string }
  | { status: 'ERROR'; data: null; message: string }

const initialState: HealthState = {
  status: 'LOADING',
  data: null,
  message: '',
}

export function BackendStatusCard() {
  const [state, setState] = useState<HealthState>(initialState)

  useEffect(() => {
    const controller = new AbortController()
    void loadHealth(controller.signal, setState)
    return () => controller.abort()
  }, [])

  const refresh = () => {
    setState(initialState)
    void loadHealth(undefined, setState)
  }

  return (
    <section className="status-card" aria-live="polite">
      <div className="status-heading">
        <div>
          <p className="label">Conectividad del backend</p>
          <h2>React → Spring Boot</h2>
        </div>
        <StatusBadge status={state.status} />
      </div>

      {state.status === 'LOADING' && (
        <p className="status-message">Consultando el servicio de salud…</p>
      )}

      {state.status === 'UP' && (
        <dl className="health-details">
          <div>
            <dt>Aplicación</dt>
            <dd>{state.data.application}</dd>
          </div>
          <div>
            <dt>Última respuesta (UTC)</dt>
            <dd>{state.data.timestamp}</dd>
          </div>
        </dl>
      )}

      {state.status === 'ERROR' && (
        <div className="error-panel" role="alert">
          <p>{state.message}</p>
          <p>Comprueba que Spring Boot esté disponible.</p>
        </div>
      )}

      <Button
        variant="secondary"
        type="button"
        onClick={refresh}
        disabled={state.status === 'LOADING'}
      >
        {state.status === 'LOADING' ? 'Consultando…' : 'Volver a consultar'}
      </Button>
    </section>
  )
}

async function loadHealth(
  signal: AbortSignal | undefined,
  setState: (state: HealthState) => void,
) {
  try {
    const data = await healthApi.get(signal)
    setState({ status: 'UP', data, message: '' })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    setState({
      status: 'ERROR',
      data: null,
      message: error instanceof Error ? error.message : 'No fue posible conectar',
    })
  }
}
