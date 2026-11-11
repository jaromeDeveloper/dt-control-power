import { Link } from 'react-router-dom'
import type { DashboardRecentEvent } from '../../types/dashboard'
import { formatUtcDate } from '../../utils/format'

export function RecentEventsTable({
  events,
}: {
  events: DashboardRecentEvent[]
}) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <caption className="visually-hidden">Últimos eventos por área</caption>
        <thead>
          <tr>
            <th scope="col">Fecha (UTC)</th>
            <th scope="col">Proceso</th>
            <th scope="col">Área</th>
            <th scope="col">Evento</th>
            <th scope="col">Estado</th>
            <th scope="col">Actor</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr
              key={`${event.processId}:${event.areaCode}:${event.eventType}:${event.occurredAt}`}
            >
              <td>{formatUtcDate(event.occurredAt)}</td>
              <td className="id-cell">
                <Link
                  className="text-link"
                  to={`/processes/${encodeURIComponent(event.processId)}`}
                >
                  {event.processId}
                </Link>
              </td>
              <td>{event.areaCode}</td>
              <td>{event.eventType}</td>
              <td>
                <span
                  className={`process-status process-status-${event.status.toLowerCase()}`}
                >
                  {event.status}
                </span>
              </td>
              <td>{event.actor}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
