import { apiRequest } from './client'
import type {
  AreaEvent,
  AreaEventStatus,
  AreaEventType,
} from '../types/operations'
import { isAreaEvent, isAreaEventList } from '../types/operations'

export interface CreateAreaEvent {
  eventType: AreaEventType
  status: AreaEventStatus
  detail?: string | null
}

export const eventsApi = {
  async list(processId: string, signal?: AbortSignal): Promise<AreaEvent[]> {
    const payload = await apiRequest<unknown>(
      `/processes/${encodeURIComponent(processId)}/events`,
      { signal },
    )
    if (!isAreaEventList(payload)) {
      throw new Error('El backend devolvió un historial de eventos inválido')
    }
    return payload
  },

  async create(
    processId: string,
    areaCode: string,
    event: CreateAreaEvent,
    idempotencyKey: string,
  ): Promise<AreaEvent> {
    const payload = await apiRequest<unknown>(
      `/processes/${encodeURIComponent(processId)}/areas/${encodeURIComponent(areaCode)}/events`,
      {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey },
        body: JSON.stringify(event),
      },
      { csrf: true },
    )
    if (!isAreaEvent(payload)) {
      throw new Error('El backend devolvió un evento de área inválido')
    }
    return payload
  },
}
