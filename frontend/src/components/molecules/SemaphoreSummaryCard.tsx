import type { AreaSemaphoreSummary } from '../../types/operations'

const COUNTS: ReadonlyArray<{
  key: 'green' | 'yellow' | 'red' | 'gray'
  label: string
}> = [
  { key: 'green', label: 'Verde' },
  { key: 'yellow', label: 'Amarillo' },
  { key: 'red', label: 'Rojo' },
  { key: 'gray', label: 'Gris' },
]

export function SemaphoreSummaryCard({ area }: { area: AreaSemaphoreSummary }) {
  return (
    <article className="dashboard-semaphore-card">
      <div className="dashboard-semaphore-heading">
        <h3>Área {area.areaCode}</h3>
        <span>{area.active.toLocaleString('es-MX')} activos</span>
      </div>
      <ul aria-label={`Conteo de semáforos del área ${area.areaCode}`}>
        {COUNTS.map((count) => (
          <li key={count.key}>
            <span
              className={`summary-color summary-color-${count.key}`}
              aria-hidden="true"
            />
            <span>{count.label}</span>
            <strong>{area[count.key].toLocaleString('es-MX')}</strong>
          </li>
        ))}
      </ul>
    </article>
  )
}
