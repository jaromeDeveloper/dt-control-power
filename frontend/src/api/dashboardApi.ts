import { apiRequest } from './client'
import type { DashboardSummary } from '../types/dashboard'
import { isDashboardSummary } from '../types/dashboard'

export const dashboardApi = {
  async getSummary(signal?: AbortSignal): Promise<DashboardSummary> {
    const payload = await apiRequest<unknown>('/dashboard/summary', { signal })
    if (!isDashboardSummary(payload)) {
      throw new Error('El backend devolvió un resumen de dashboard inválido')
    }
    return payload
  },
}
