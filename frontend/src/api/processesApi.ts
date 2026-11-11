import { apiRequest } from './client'
import type {
  Checkpoint,
  PageResponse,
  ProcessDetail,
  ProcessStatus,
  ProcessSummary,
} from '../types/processes'
import {
  isCheckpointList,
  isProcessDetail,
  isProcessPage,
} from '../types/processes'

export interface ProcessQuery {
  page: number
  size: number
  status?: ProcessStatus
  areaCode?: string
}

export const processesApi = {
  async list(
    query: ProcessQuery,
    signal?: AbortSignal,
  ): Promise<PageResponse<ProcessSummary>> {
    const params = new URLSearchParams({
      page: String(query.page),
      size: String(query.size),
    })
    if (query.status) params.set('status', query.status)
    if (query.areaCode) params.set('areaCode', query.areaCode)

    const payload = await apiRequest<unknown>(`/processes?${params}`, { signal })
    if (!isProcessPage(payload)) {
      throw new Error('El backend devolvió una página de procesos inválida')
    }
    return payload
  },

  async get(processId: string, signal?: AbortSignal): Promise<ProcessDetail> {
    const payload = await apiRequest<unknown>(
      `/processes/${encodeURIComponent(processId)}`,
      { signal },
    )
    if (!isProcessDetail(payload)) {
      throw new Error('El backend devolvió un proceso inválido')
    }
    return payload
  },

  async timeline(
    processId: string,
    signal?: AbortSignal,
  ): Promise<Checkpoint[]> {
    const payload = await apiRequest<unknown>(
      `/processes/${encodeURIComponent(processId)}/timeline`,
      { signal },
    )
    if (!isCheckpointList(payload)) {
      throw new Error('El backend devolvió una línea de tiempo inválida')
    }
    return payload
  },
}
