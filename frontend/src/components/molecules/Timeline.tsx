import type { Checkpoint } from '../../types/processes'
import { formatUtcDate } from '../../utils/format'
import { ProcessStatusLabel } from '../atoms/ProcessStatus'

interface TimelineProps {
  checkpoints: Checkpoint[]
}

export function Timeline({ checkpoints }: TimelineProps) {
  return (
    <section className="data-card" aria-labelledby="timeline-title">
      <div className="data-card-heading">
        <div>
          <p className="label">Trazabilidad funcional</p>
          <h2 id="timeline-title">Línea de tiempo</h2>
        </div>
      </div>

      {checkpoints.length === 0 ? (
        <p className="empty-state">Este proceso todavía no tiene checkpoints.</p>
      ) : (
        <ol className="timeline-list">
          {checkpoints.map((checkpoint) => (
            <li key={checkpoint.checkpointId}>
              <div className="timeline-marker" aria-hidden="true" />
              <div className="timeline-content">
                <div className="timeline-heading">
                  <strong>{checkpoint.stage}</strong>
                  <ProcessStatusLabel value={checkpoint.status} />
                </div>
                <p>
                  {formatUtcDate(checkpoint.startedAt)}
                  {checkpoint.areaCode ? ` · ${checkpoint.areaCode}` : ''}
                  {checkpoint.durationMs !== null
                    ? ` · ${checkpoint.durationMs} ms`
                    : ''}
                </p>
                {checkpoint.safeMessage && (
                  <p className="checkpoint-message">{checkpoint.safeMessage}</p>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
