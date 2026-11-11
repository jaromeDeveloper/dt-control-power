import type { AuditEntry } from '../../types/records'
import { formatUtcDate } from '../../utils/format'

export function AuditTable({ entries }: { entries: AuditEntry[] }) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <caption className="visually-hidden">Eventos de auditoría</caption>
        <thead>
          <tr>
            <th scope="col">Fecha (UTC)</th>
            <th scope="col">Proceso</th>
            <th scope="col">Actor</th>
            <th scope="col">Cambio</th>
            <th scope="col">Resultado</th>
            <th scope="col">Request ID</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id}>
              <td>{formatUtcDate(entry.occurredAt)}</td>
              <td className="id-cell">{entry.processId}</td>
              <td>{entry.actor}</td>
              <td>
                <span className="audit-value">{entry.oldValue ?? '—'}</span>
                {' → '}
                <span className="audit-value">{entry.newValue ?? '—'}</span>
              </td>
              <td>
                <span
                  className={`process-status process-status-${entry.outcome.toLowerCase()}`}
                >
                  {entry.outcome}
                </span>
              </td>
              <td className="id-cell">{entry.requestId}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
