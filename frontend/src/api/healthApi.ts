import { apiRequest } from './client'
import type { HealthResponse } from '../types/health'
import { isHealthResponse } from '../types/health'

export const healthApi = {
  async get(signal?: AbortSignal): Promise<HealthResponse> {
    const payload = await apiRequest<unknown>('/health', { signal })
    if (!isHealthResponse(payload)) {
      throw new Error('El backend devolvió una respuesta de salud inválida')
    }
    return payload
  },
}
