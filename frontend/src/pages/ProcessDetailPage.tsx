import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { processesApi } from '../api/processesApi'
import { ProcessStatusLabel } from '../components/atoms/ProcessStatus'
import { FileDetailsCard } from '../components/molecules/FileDetailsCard'
import { Timeline } from '../components/molecules/Timeline'
import { BusinessRecordPanel } from '../components/organisms/BusinessRecordPanel'
import { AreaOperationsPanel } from '../components/organisms/AreaOperationsPanel'
import type { Checkpoint, ProcessDetail } from '../types/processes'
import { formatUtcDate } from '../utils/format'

type DetailState =
  | { status: 'loading'; process: null; timeline: Checkpoint[]; message: string }
  | {
      status: 'ready'
      process: ProcessDetail
      timeline: Checkpoint[]
      message: string
    }
  | { status: 'error'; process: null; timeline: Checkpoint[]; message: string }

export function ProcessDetailPage() {
  const { id } = useParams<{ id: string }>()

  if (!id) {
    return (
      <main className="content-page">
        <p className="form-error" role="alert">
          El identificador del proceso es obligatorio.
        </p>
      </main>
    )
  }

  return <ProcessDetailLoader key={id} processId={id} />
}

function ProcessDetailLoader({ processId }: { processId: string }) {
  const [timelineRefresh, setTimelineRefresh] = useState(0)
  const [state, setState] = useState<DetailState>({
    status: 'loading',
    process: null,
    timeline: [],
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()
    void Promise.all([
      processesApi.get(processId, controller.signal),
      processesApi.timeline(processId, controller.signal),
    ])
      .then(([process, timeline]) =>
        setState({ status: 'ready', process, timeline, message: '' }),
      )
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({
          status: 'error',
          process: null,
          timeline: [],
          message:
            error instanceof Error
              ? error.message
              : 'No fue posible cargar el proceso.',
        })
      })
    return () => controller.abort()
  }, [processId])

  useEffect(() => {
    if (timelineRefresh === 0) return
    const controller = new AbortController()
    void Promise.all([
      processesApi.get(processId, controller.signal),
      processesApi.timeline(processId, controller.signal),
    ])
      .then(([process, timeline]) =>
        setState((current) =>
          current.status === 'ready'
            ? { ...current, process, timeline }
            : current,
        ),
      )
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        // The successful mutation remains visible; a manual page refresh can retry process details.
      })
    return () => controller.abort()
  }, [processId, timelineRefresh])

  if (state.status === 'loading') {
    return (
      <main className="content-page" aria-live="polite">
        <p>Cargando detalle del proceso…</p>
      </main>
    )
  }

  if (state.status === 'error') {
    return (
      <main className="content-page">
        <div className="error-panel" role="alert">
          <p>{state.message}</p>
        </div>
        <Link className="text-link" to="/processes">
          Volver a procesos
        </Link>
      </main>
    )
  }

  return (
    <main className="content-page">
      <header className="page-heading process-heading">
        <div>
          <p className="eyebrow">Detalle de proceso</p>
          <h1>{state.process.processId}</h1>
          <p>
            Creado por {state.process.createdBy} · Inicio{' '}
            {formatUtcDate(state.process.startedAt)}
          </p>
        </div>
        <ProcessStatusLabel value={state.process.status} />
      </header>

      <section className="data-card" aria-labelledby="process-summary-title">
        <div className="data-card-heading">
          <div>
            <p className="label">Resumen</p>
            <h2 id="process-summary-title">Estado del proceso</h2>
          </div>
        </div>
        <dl className="detail-grid">
          <div>
            <dt>Inicio (UTC)</dt>
            <dd>{formatUtcDate(state.process.startedAt)}</dd>
          </div>
          <div>
            <dt>Finalización (UTC)</dt>
            <dd>{formatUtcDate(state.process.finishedAt)}</dd>
          </div>
          <div className="detail-wide">
            <dt>Descripción</dt>
            <dd>{state.process.description || 'Sin descripción'}</dd>
          </div>
        </dl>
      </section>

      <FileDetailsCard file={state.process.file} />
      <AreaOperationsPanel
        processId={state.process.processId}
        onDataChanged={() => setTimelineRefresh((value) => value + 1)}
      />
      <BusinessRecordPanel
        processId={state.process.processId}
        onUpdated={() => setTimelineRefresh((value) => value + 1)}
      />
      <Timeline checkpoints={state.timeline} />
    </main>
  )
}
