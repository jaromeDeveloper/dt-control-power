import type { AreaSemaphore } from '../../types/operations'
import { formatUtcDate } from '../../utils/format'

const COLOR_LABELS: Record<AreaSemaphore['color'], string> = {
  GRAY: 'Gris',
  YELLOW: 'Amarillo',
  GREEN: 'Verde',
  RED: 'Rojo',
}

export function SemaphoreIndicator({ semaphore }: { semaphore: AreaSemaphore }) {
  return (
    <article
      className={`area-semaphore semaphore-${semaphore.color.toLowerCase()}`}
      aria-label={`Área ${semaphore.areaCode}: ${COLOR_LABELS[semaphore.color]}, ${semaphore.status}`}
    >
      <span className="semaphore-light" aria-hidden="true" />
      <div>
        <strong>Área {semaphore.areaCode}</strong>
        <span>
          {COLOR_LABELS[semaphore.color]} · {semaphore.status}
        </span>
        <small>
          {semaphore.lastEventAt
            ? `Último evento: ${formatUtcDate(semaphore.lastEventAt)}`
            : 'Sin eventos registrados'}
        </small>
      </div>
    </article>
  )
}
