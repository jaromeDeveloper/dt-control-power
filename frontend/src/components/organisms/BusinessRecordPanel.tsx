import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { recordsApi } from '../../api/recordsApi'
import { useAuth } from '../../app/authContext'
import type { BusinessRecord } from '../../types/records'
import { formatUtcDate } from '../../utils/format'
import { Button } from '../atoms/Button'

type RecordsState =
  | { status: 'loading'; records: BusinessRecord[]; message: string }
  | { status: 'ready'; records: BusinessRecord[]; message: string }
  | { status: 'error'; records: BusinessRecord[]; message: string }

export function BusinessRecordPanel({
  processId,
  onUpdated,
}: {
  processId: string
  onUpdated?: () => void
}) {
  const auth = useAuth()
  const [reload, setReload] = useState(0)
  const [state, setState] = useState<RecordsState>({
    status: 'loading',
    records: [],
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()
    void recordsApi
      .listByProcess(processId, controller.signal)
      .then((records) => setState({ status: 'ready', records, message: '' }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({
          status: 'error',
          records: [],
          message:
            error instanceof Error
              ? error.message
              : 'No fue posible cargar los registros de negocio.',
        })
      })
    return () => controller.abort()
  }, [processId, reload])

  const refresh = () => {
    setState({ status: 'loading', records: [], message: '' })
    setReload((value) => value + 1)
  }

  const isReadOnly =
    auth.status !== 'authenticated' || auth.user.role === 'AUDITOR'

  return (
    <section className="data-card" aria-labelledby="business-records-title">
      <div className="data-card-heading">
        <div>
          <p className="label">Dato funcional</p>
          <h2 id="business-records-title">Registro de negocio</h2>
        </div>
        {state.status !== 'loading' && (
          <Button variant="secondary" type="button" onClick={refresh}>
            Actualizar
          </Button>
        )}
      </div>

      <div aria-live="polite">
        {state.status === 'loading' && <p>Cargando registro…</p>}
        {state.status === 'error' && (
          <div className="error-panel" role="alert">
            <p>{state.message}</p>
          </div>
        )}
        {state.status === 'ready' && state.records.length === 0 && (
          <p className="empty-state">Este proceso todavía no tiene registros.</p>
        )}
      </div>

      {state.status === 'ready' && state.records.length > 0 && (
        <div className="record-list">
          {state.records.map((record) => (
            <RecordEditor
              key={record.id}
              record={record}
              readOnly={isReadOnly}
              onUpdated={onUpdated}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function RecordEditor({
  record,
  readOnly,
  onUpdated,
}: {
  record: BusinessRecord
  readOnly: boolean
  onUpdated?: () => void
}) {
  const [currentRecord, setCurrentRecord] = useState(record)
  const [value, setValue] = useState(currentRecord.value)
  const [isSaving, setIsSaving] = useState(false)
  const savingLock = useRef(false)
  const [message, setMessage] = useState('')
  const [isError, setIsError] = useState(false)

  useEffect(() => {
    setCurrentRecord(record)
    setValue(record.value)
  }, [record])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (savingLock.current || readOnly) return

    const nextValue = value.trim()
    if (!nextValue || nextValue.length > 200) {
      setIsError(true)
      setMessage('El valor es obligatorio y admite hasta 200 caracteres.')
      return
    }

    savingLock.current = true
    setIsSaving(true)
    setMessage('')
    setIsError(false)
    try {
      const updated = await recordsApi.update(currentRecord.id, nextValue)
      setCurrentRecord(updated)
      setValue(updated.value)
      setMessage('Registro actualizado correctamente.')
      onUpdated?.()
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        try {
          const latest = await recordsApi.get(currentRecord.id)
          setCurrentRecord(latest)
          setValue(latest.value)
          setIsError(true)
          setMessage(
            'Otra sesión actualizó el registro. Se cargó la versión más reciente; revisa el valor antes de guardar.',
          )
        } catch {
          setIsError(true)
          setMessage(
            'Otra sesión actualizó el registro y no fue posible refrescarlo. Usa Actualizar antes de intentarlo de nuevo.',
          )
        }
      } else {
        setIsError(true)
        setMessage(
          error instanceof Error
            ? error.message
            : 'No fue posible actualizar el registro.',
        )
      }
    } finally {
      savingLock.current = false
      setIsSaving(false)
    }
  }

  return (
    <article className="record-card">
      <dl className="detail-grid record-metadata">
        <div>
          <dt>Referencia</dt>
          <dd>{currentRecord.reference}</dd>
        </div>
        <div>
          <dt>Versión</dt>
          <dd>{currentRecord.version}</dd>
        </div>
        <div>
          <dt>Actualizado por</dt>
          <dd>{currentRecord.updatedBy}</dd>
        </div>
        <div>
          <dt>Actualización (UTC)</dt>
          <dd>{formatUtcDate(currentRecord.updatedAt)}</dd>
        </div>
      </dl>

      <form className="record-form" onSubmit={(event) => void handleSubmit(event)}>
        <label className="field">
          <span className="field-label">Valor</span>
          <textarea
            value={value}
            onChange={(event) => setValue(event.target.value)}
            maxLength={200}
            rows={3}
            readOnly={readOnly}
            aria-describedby={`record-help-${currentRecord.id}`}
          />
        </label>
        <div className="record-actions">
          <span id={`record-help-${currentRecord.id}`} className="field-hint">
            {readOnly
              ? 'Tu rol tiene acceso de sólo lectura.'
              : `${value.length}/200 caracteres`}
          </span>
          {!readOnly && (
            <Button
              type="submit"
              disabled={isSaving || value.trim() === currentRecord.value}
            >
              {isSaving ? 'Guardando…' : 'Guardar cambio'}
            </Button>
          )}
        </div>
        {message && (
          <p className={isError ? 'form-error' : 'form-success'} role="alert">
            {message}
          </p>
        )}
      </form>
    </article>
  )
}
