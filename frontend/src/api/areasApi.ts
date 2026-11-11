import { apiRequest } from './client'
import type { ProductionArea } from '../types/areas'
import { isProductionAreaList } from '../types/areas'

export const areasApi = {
  async list(signal?: AbortSignal): Promise<ProductionArea[]> {
    const payload = await apiRequest<unknown>('/areas', { signal })
    if (!isProductionAreaList(payload)) {
      throw new Error('El backend devolvió un catálogo de áreas inválido')
    }
    return payload
  },
}
