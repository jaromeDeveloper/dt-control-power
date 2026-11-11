import { apiRequest } from './client'
import type { BusinessRecord } from '../types/records'
import {
  isBusinessRecord,
  isBusinessRecordList,
} from '../types/records'

export const recordsApi = {
  async listByProcess(
    processId: string,
    signal?: AbortSignal,
  ): Promise<BusinessRecord[]> {
    const params = new URLSearchParams({ processId })
    const payload = await apiRequest<unknown>(
      `/business-records?${params}`,
      { signal },
    )
    if (!isBusinessRecordList(payload)) {
      throw new Error('El backend devolvió registros de negocio inválidos')
    }
    return payload
  },

  async get(recordId: string, signal?: AbortSignal): Promise<BusinessRecord> {
    const payload = await apiRequest<unknown>(
      `/business-records/${encodeURIComponent(recordId)}`,
      { signal },
    )
    if (!isBusinessRecord(payload)) {
      throw new Error('El backend devolvió un registro de negocio inválido')
    }
    return payload
  },

  async update(recordId: string, value: string): Promise<BusinessRecord> {
    const payload = await apiRequest<unknown>(
      `/business-records/${encodeURIComponent(recordId)}`,
      { method: 'PATCH', body: JSON.stringify({ value }) },
      { csrf: true },
    )
    if (!isBusinessRecord(payload)) {
      throw new Error('El backend devolvió un registro actualizado inválido')
    }
    return payload
  },
}
