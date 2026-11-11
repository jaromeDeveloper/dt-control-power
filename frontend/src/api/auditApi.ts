import { apiRequest } from './client'
import type { PageResponse } from '../types/processes'
import type { AuditEntry } from '../types/records'
import { isAuditPage } from '../types/records'

export interface AuditQuery {
  page: number
  size: number
  processId?: string
}

export const auditApi = {
  async list(
    query: AuditQuery,
    signal?: AbortSignal,
  ): Promise<PageResponse<AuditEntry>> {
    const params = new URLSearchParams({
      page: String(query.page),
      size: String(query.size),
    })
    if (query.processId) params.set('processId', query.processId)

    const payload = await apiRequest<unknown>(`/audit?${params}`, { signal })
    if (!isAuditPage(payload)) {
      throw new Error('El backend devolvió una página de auditoría inválida')
    }
    return payload
  },
}
