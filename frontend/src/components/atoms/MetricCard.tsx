export function MetricCard({
  label,
  value,
  tone = 'neutral',
}: {
  label: string
  value: number
  tone?: 'neutral' | 'active' | 'success' | 'danger'
}) {
  return (
    <article className={`metric-card metric-${tone}`}>
      <span>{label}</span>
      <strong>{value.toLocaleString('es-MX')}</strong>
    </article>
  )
}
