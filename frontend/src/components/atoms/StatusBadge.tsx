interface StatusBadgeProps {
  status: 'LOADING' | 'UP' | 'ERROR'
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <span className="status-dot" aria-hidden="true" />
      {status}
    </span>
  )
}
