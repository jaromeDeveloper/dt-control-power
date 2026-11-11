export interface HealthResponse {
  application: string
  status: 'UP'
  timestamp: string
}

export function isHealthResponse(value: unknown): value is HealthResponse {
  if (typeof value !== 'object' || value === null) return false

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.application === 'string' &&
    candidate.status === 'UP' &&
    typeof candidate.timestamp === 'string'
  )
}
