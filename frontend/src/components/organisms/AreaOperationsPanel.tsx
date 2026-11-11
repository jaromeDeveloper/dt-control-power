import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../../api/client'
import { eventsApi } from '../../api/eventsApi'
import { semaphoresApi } from '../../api/semaphoresApi'
import { useAuth } from '../../app/authContext'
import type {
  AreaEvent,
  AreaEventStatus,
  AreaEventType,
  ProcessSemaphores,
} from '../../types/operations'
import { formatUtcDate } from '../../utils/format'
import { Button } from '../atoms/Button'
import { SemaphoreIndicator } from '../atoms/SemaphoreIndicator'
import { AreaEventActions } from '../molecules/AreaEventActions'

type OperationsState =
  | { status: 'loading'; events: AreaEvent[]; semaphores: null; message: string }
  | {
      status: 'ready'
      events: AreaEvent[]
      semaphores: ProcessSemaphores
      message: string
    }
  | { status: 'error'; events: AreaEvent[]; semaphores: null; message: string }

export function AreaOperationsPanel({
  processId,
  onDataChanged,
}: {
  processId: string
  onDataChanged: () => void
}) {
  const auth = useAuth()
  const [reload, setReload] = useState(0)
  const [state, setState] = useState<OperationsState>({
    status: 'loading',
    events: [],
    semaphores: null,
    message: '',
  })
  const [details, setDetails] = useState<Record<string, string>>({})
  const [submittingAction, setSubmittingAction] = useState<string | null>(null)
  const [notice, setNotice] = useState<{
    kind: 'success' | 'error'
    text: string
  } | null>(null)
  const submitLock = useRef(false)
  const retryKeys = useRef(
    new Map<string, { payloadSignature: string; idempotencyKey: string }>(),
  )

  useEffect(() => {
    const controller = new AbortController()
    void Promise.all([
      eventsApi.list(processId, controller.signal),
      semaphoresApi.getForProcess(processId, controller.signal),
    ])
      .then(([events, semaphores]) =>
        setState({ status: 'ready', events, semaphores, message: '' }),
      )
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({
          status: 'error',
          events: [],
          semaphores: null,
          message:
            error instanceof Error
              ? error.message
              : 'No fue posible cargar eventos y semáforos.',
        })
      })
    return () => controller.abort()
  }, [processId, reload])

  const refresh = () => {
    setState({ status: 'loading', events: [], semaphores: null, message: '' })
    setReload((value) => value + 1)
  }

  const handleSubmit = async (
    areaCode: string,
    eventType: AreaEventType,
    status: AreaEventStatus,
  ) => {
    if (submitLock.current) return

    const actionKey = `${areaCode}:${eventType}`
    const detail = details[areaCode]?.trim() || null
    const payloadSignature = JSON.stringify({ eventType, status, detail })
    const previousAttempt = retryKeys.current.get(actionKey)
    const idempotencyKey =
      previousAttempt?.payloadSignature === payloadSignature
        ? previousAttempt.idempotencyKey
        : crypto.randomUUID()
    retryKeys.current.set(actionKey, { payloadSignature, idempotencyKey })
    submitLock.current = true
    setSubmittingAction(actionKey)
    setNotice(null)
    try {
      await eventsApi.create(
        processId,
        areaCode,
        {
          eventType,
          status,
          detail,
        },
        idempotencyKey,
      )
      setDetails((current) => ({ ...current, [areaCode]: '' }))
      retryKeys.current.delete(actionKey)
      setNotice({
        kind: 'success',
        text: `Evento ${eventType} registrado en el área ${areaCode}.`,
      })
      refresh()
      onDataChanged()
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        retryKeys.current.delete(actionKey)
        setNotice({
          kind: 'error',
          text: 'El backend rechazó la transición o detectó un conflicto de idempotencia. Se refrescaron los datos vigentes.',
        })
        refresh()
        onDataChanged()
      } else {
        setNotice({
          kind: 'error',
          text:
            error instanceof Error
              ? error.message
              : 'No fue posible registrar el evento.',
        })
      }
    } finally {
      submitLock.current = false
      setSubmittingAction(null)
    }
  }

  const canEditArea = (areaCode: string) => {
    if (auth.status !== 'authenticated') return false
    if (auth.user.role === 'ADMIN' || auth.user.role === 'SUPERVISOR') return true
    return (
      auth.user.role === 'OPERATOR' && auth.user.areaCodes.includes(areaCode)
    )
  }

  return (
    <section className="data-card" aria-labelledby="area-operations-title">
      <div className="data-card-heading">
        <div>
          <p className="label">Flujo por área</p>
          <h2 id="area-operations-title">Eventos y semáforos</h2>
        </div>
        {state.status !== 'loading' && (
          <Button variant="secondary" type="button" onClick={refresh}>
            Actualizar
          </Button>
        )}
      </div>

      {notice && (
        <p
          className={notice.kind === 'error' ? 'form-error' : 'form-success'}
          role={notice.kind === 'error' ? 'alert' : 'status'}
        >
          {notice.text}
        </p>
      )}

      <div aria-live="polite">
        {state.status === 'loading' && <p>Cargando operación por áreas…</p>}
        {state.status === 'error' && (
          <div className="error-panel" role="alert">
            <p>{state.message}</p>
          </div>
        )}
      </div>

      {state.status === 'ready' && (
        <>
          <div className="semaphore-grid" aria-label="Semáforos por área">
            {state.semaphores.areas.map((semaphore) => (
              <SemaphoreIndicator
                key={semaphore.areaCode}
                semaphore={semaphore}
              />
            ))}
          </div>

          {state.semaphores.areas.some((area) => canEditArea(area.areaCode)) && (
            <div className="area-actions-grid">
              {state.semaphores.areas
                .filter((area) => canEditArea(area.areaCode))
                .map((area) => (
                  <AreaEventActions
                    key={area.areaCode}
                    areaCode={area.areaCode}
                    detail={details[area.areaCode] ?? ''}
                    submittingAction={submittingAction}
                    onDetailChange={(value) =>
                      setDetails((current) => ({
                        ...current,
                        [area.areaCode]: value,
                      }))
                    }
                    onSubmit={(eventType, status) =>
                      void handleSubmit(area.areaCode, eventType, status)
                    }
                  />
                ))}
            </div>
          )}

          <div className="event-history">
            <h3>Historial de eventos</h3>
            {state.events.length === 0 ? (
              <p className="empty-state">Todavía no existen eventos.</p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <caption className="visually-hidden">
                    Historial de eventos del proceso
                  </caption>
                  <thead>
                    <tr>
                      <th scope="col">Fecha (UTC)</th>
                      <th scope="col">Área</th>
                      <th scope="col">Evento</th>
                      <th scope="col">Estado</th>
                      <th scope="col">Actor</th>
                      <th scope="col">Semáforo</th>
                      <th scope="col">Detalle</th>
                      <th scope="col">Trace ID</th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.events.map((event) => (
                      <tr key={event.eventId}>
                        <td>{formatUtcDate(event.occurredAt)}</td>
                        <td>{event.areaCode}</td>
                        <td>{event.eventType}</td>
                        <td>
                          <span
                            className={`process-status process-status-${event.status.toLowerCase()}`}
                          >
                            {event.status}
                          </span>
                        </td>
                        <td className="id-cell">{event.actorId}</td>
                        <td>{event.semaphore}</td>
                        <td>{event.detail || '—'}</td>
                        <td className="id-cell">{event.traceId || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </section>
  )
}
