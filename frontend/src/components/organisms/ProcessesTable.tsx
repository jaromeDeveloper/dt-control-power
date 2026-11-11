import { Link } from 'react-router-dom'
import type { ProcessSummary } from '../../types/processes'
import { formatUtcDate } from '../../utils/format'
import { ProcessStatusLabel } from '../atoms/ProcessStatus'

interface ProcessesTableProps {
  processes: ProcessSummary[]
}

export function ProcessesTable({ processes }: ProcessesTableProps) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Proceso</th>
            <th scope="col">Archivo</th>
            <th scope="col">Creador</th>
            <th scope="col">Inicio (UTC)</th>
            <th scope="col">Estado</th>
            <th scope="col">
              <span className="visually-hidden">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {processes.map((process) => (
            <tr key={process.processId}>
              <td className="id-cell">{process.processId}</td>
              <td>{process.file.name}</td>
              <td>{process.createdBy}</td>
              <td>{formatUtcDate(process.startedAt)}</td>
              <td>
                <ProcessStatusLabel value={process.status} />
              </td>
              <td>
                <Link className="text-link" to={`/processes/${process.processId}`}>
                  Ver detalle
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
