import { apiRequest } from './client'
import type {
  ProcessSemaphores,
  SemaphoreSummary,
} from '../types/operations'
import {
  isProcessSemaphores,
  isSemaphoreSummary,
} from '../types/operations'

export const semaphoresApi = {
  async getForProcess(
    processId: string,
    signal?: AbortSignal,
  ): Promise<ProcessSemaphores> {
    const params = new URLSearchParams({ processId })
    const payload = await apiRequest<unknown>(`/semaphores?${params}`, {
      signal,
    })
    if (!isProcessSemaphores(payload)) {
      throw new Error('El backend devolvió semáforos inválidos')
    }
    return payload
  },

  async summary(signal?: AbortSignal): Promise<SemaphoreSummary> {
    const payload = await apiRequest<unknown>('/semaphores/summary', { signal })
    if (!isSemaphoreSummary(payload)) {
      throw new Error('El backend devolvió un resumen de semáforos inválido')
    }
    return payload
  },
}
