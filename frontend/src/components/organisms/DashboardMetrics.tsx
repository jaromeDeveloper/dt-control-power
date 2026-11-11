import type { DashboardSummary } from '../../types/dashboard'
import { MetricCard } from '../atoms/MetricCard'

export function DashboardMetrics({ summary }: { summary: DashboardSummary }) {
  return (
    <section className="metric-grid" aria-label="Indicadores de operación">
      <MetricCard label="Procesos totales" value={summary.totalProcesses} />
      <MetricCard
        label="Procesos activos"
        value={summary.activeProcesses}
        tone="active"
      />
      <MetricCard
        label="Procesos completados"
        value={summary.completedProcesses}
        tone="success"
      />
      <MetricCard
        label="Procesos fallidos"
        value={summary.failedProcesses}
        tone="danger"
      />
      <MetricCard label="Archivos cargados" value={summary.uploadedFiles} />
    </section>
  )
}
