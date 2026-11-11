import { apiRequest } from './client'
import type { FileResource, UploadFileCommand } from '../types/files'
import { isFileResource } from '../types/files'

export const filesApi = {
  async upload(command: UploadFileCommand): Promise<FileResource> {
    const form = new FormData()
    form.append('file', command.file)
    if (command.areaCode) form.append('areaCode', command.areaCode)
    if (command.description) form.append('description', command.description)

    const payload = await apiRequest<unknown>(
      '/files',
      { method: 'POST', body: form },
      { csrf: true },
    )
    return requireFileResource(payload)
  },

  async get(fileId: string, signal?: AbortSignal): Promise<FileResource> {
    const payload = await apiRequest<unknown>(
      `/files/${encodeURIComponent(fileId)}`,
      { signal },
    )
    return requireFileResource(payload)
  },
}

function requireFileResource(payload: unknown): FileResource {
  if (!isFileResource(payload)) {
    throw new Error('El backend devolvió metadatos de archivo inválidos')
  }
  return payload
}
