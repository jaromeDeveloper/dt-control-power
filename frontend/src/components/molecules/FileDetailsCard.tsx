import { Link } from 'react-router-dom'
import type { FileResource } from '../../types/files'
import { formatBytes, formatUtcDate } from '../../utils/format'

interface FileDetailsCardProps {
  file: FileResource
  showProcessLink?: boolean
}

export function FileDetailsCard({
  file,
  showProcessLink = false,
}: FileDetailsCardProps) {
  return (
    <section className="data-card" aria-labelledby={`file-${file.fileId}`}>
      <div className="data-card-heading">
        <div>
          <p className="label">Archivo</p>
          <h2 id={`file-${file.fileId}`}>{file.name}</h2>
        </div>
        <span className="resource-status">{file.status}</span>
      </div>

      <dl className="detail-grid">
        <div>
          <dt>File ID</dt>
          <dd>{file.fileId}</dd>
        </div>
        <div>
          <dt>Tamaño</dt>
          <dd>{formatBytes(file.sizeBytes)}</dd>
        </div>
        <div>
          <dt>Tipo</dt>
          <dd>{file.contentType}</dd>
        </div>
        <div>
          <dt>Usuario</dt>
          <dd>{file.uploadedBy}</dd>
        </div>
        <div>
          <dt>Fecha (UTC)</dt>
          <dd>{formatUtcDate(file.uploadedAt)}</dd>
        </div>
        <div className="detail-wide">
          <dt>SHA-256</dt>
          <dd className="hash-value">{file.sha256}</dd>
        </div>
      </dl>

      {showProcessLink && (
        <Link className="text-link" to={`/processes/${file.processId}`}>
          Abrir proceso {file.processId}
        </Link>
      )}
    </section>
  )
}
