import type { AreaEventStatus, AreaEventType } from '../../types/operations'
import { EVENT_ACTIONS } from '../../types/operations'
import { Button } from '../atoms/Button'

export function AreaEventActions({
  areaCode,
  detail,
  submittingAction,
  onDetailChange,
  onSubmit,
}: {
  areaCode: string
  detail: string
  submittingAction: string | null
  onDetailChange: (value: string) => void
  onSubmit: (eventType: AreaEventType, status: AreaEventStatus) => void
}) {
  return (
    <article className="area-actions">
      <h3>Acciones área {areaCode}</h3>
      <label className="field">
        <span className="field-label">Detalle opcional</span>
        <textarea
          value={detail}
          onChange={(event) => onDetailChange(event.target.value)}
          maxLength={500}
          rows={2}
          disabled={submittingAction !== null}
        />
        <span className="field-hint">{detail.length}/500 caracteres</span>
      </label>
      <div className="event-action-buttons">
        {EVENT_ACTIONS.map((action) => {
          const actionKey = `${areaCode}:${action.eventType}`
          const isSubmitting = submittingAction === actionKey
          return (
            <Button
              key={action.eventType}
              variant={action.eventType === 'FAILED' ? 'secondary' : 'primary'}
              type="button"
              disabled={submittingAction !== null}
              onClick={() => onSubmit(action.eventType, action.status)}
            >
              {isSubmitting ? 'Registrando…' : action.label}
            </Button>
          )
        })}
      </div>
    </article>
  )
}
