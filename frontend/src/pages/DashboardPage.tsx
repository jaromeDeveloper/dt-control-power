import { useEffect, useState } from 'react'
import { dashboardApi } from '../api/dashboardApi'
import { Button } from '../components/atoms/Button'
import { SemaphoreSummaryCard } from '../components/molecules/SemaphoreSummaryCard'
import { DashboardMetrics } from '../components/organisms/DashboardMetrics'
import { RecentEventsTable } from '../components/organisms/RecentEventsTable'
import type { DashboardSummary } from '../types/dashboard'

const POLLING_INTERVAL_MS = 5_000

type DashboardState =
  | { status: 'loading'; data: null; message: string }
  | {
      status: 'ready'
      data: DashboardSummary
      message: string
      isRefreshing: boolean
    }
  | { status: 'error'; data: DashboardSummary | null; message: string }

export function DashboardPage() {
  const [requestVersion, setRequestVersion] = useState(0)
  const [state, setState] = useState<DashboardState>({
    status: 'loading',
    data: null,
    message: '',
  })

  useEffect(() => {
    let disposed = false
    let timer: number | undefined
    let controller: AbortController | null = null

    const load = async () => {
      const requestController = new AbortController()
      controller = requestController
      setState((current) =>
        current.data
          ? {
              status: 'ready',
              data: current.data,
              message: '',
              isRefreshing: true,
            }
          : { status: 'loading', data: null, message: '' },
      )

      try {
        const data = await dashboardApi.getSummary(requestController.signal)
        if (!disposed) {
          setState({
            status: 'ready',
            data,
            message: '',
            isRefreshing: false,
          })
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return
        if (!disposed) {
          setState((current) => ({
            status: 'error',
            data: current.data,
            message:
              error instanceof Error
                ? error.message
                : 'No fue posible actualizar el dashboard.',
          }))
        }
      } finally {
        if (!disposed) {
          timer = window.setTimeout(() => void load(), POLLING_INTERVAL_MS)
        }
      }
    }

    void load()
    return () => {
      disposed = true
      if (timer !== undefined) window.clearTimeout(timer)
      controller?.abort()
    }
  }, [requestVersion])

  const refresh = () => {
    setState((current) =>
      current.data
        ? {
            status: 'ready',
            data: current.data,
            message: '',
            isRefreshing: true,
          }
        : current,
    )
    setRequestVersion((value) => value + 1)
  }

  const data = state.data
  const isRefreshing =
    state.status === 'loading' ||
    (state.status === 'ready' && state.isRefreshing)

  return (
    <main className="content-page dashboard-page">
      <header className="page-heading page-heading-actions">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>Control operativo</h1>
          <p>Indicadores reales de procesos, archivos y operación por áreas.</p>
        </div>
        <Button
          variant="secondary"
          type="button"
          onClick={refresh}
          disabled={isRefreshing}
        >
          {isRefreshing ? 'Actualizando…' : 'Actualizar'}
        </Button>
      </header>

      <div aria-live="polite">
        {state.status === 'loading' && <p>Cargando dashboard…</p>}
        {state.status === 'error' && (
          <div className="form-error" role="alert">
            <p>{state.message}</p>
            {data && <p>Se conservan los últimos datos disponibles.</p>}
          </div>
        )}
      </div>

      {data && (
        <>
          {data.totalProcesses === 0 && (
            <p className="empty-state dashboard-empty">
              Todavía no existen procesos. Los indicadores se actualizarán
              automáticamente cuando haya actividad.
            </p>
          )}

          <DashboardMetrics summary={data} />

          <section
            className="data-card"
            aria-labelledby="dashboard-semaphores-title"
          >
            <div className="data-card-heading">
              <div>
                <p className="label">Estado por área</p>
                <h2 id="dashboard-semaphores-title">Resumen de semáforos</h2>
              </div>
            </div>
            {data.semaphores.areas.length === 0 ? (
              <p className="empty-state">No hay áreas para mostrar.</p>
            ) : (
              <div className="dashboard-semaphore-grid">
                {data.semaphores.areas.map((area) => (
                  <SemaphoreSummaryCard key={area.areaCode} area={area} />
                ))}
              </div>
            )}
          </section>

          <section className="data-card" aria-labelledby="recent-events-title">
            <div className="data-card-heading">
              <div>
                <p className="label">Actividad</p>
                <h2 id="recent-events-title">Últimos eventos</h2>
              </div>
            </div>
            {data.recentEvents.length === 0 ? (
              <p className="empty-state">Todavía no existen eventos.</p>
            ) : (
              <RecentEventsTable events={data.recentEvents} />
            )}
          </section>
        </>
      )}
    </main>
  )
}
