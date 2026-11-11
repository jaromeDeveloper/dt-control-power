export type FileValidationStatus = 'RECEIVED' | 'VALIDATED' | 'REJECTED'

export interface FileResource {
  fileId: string
  processId: string
  name: string
  sizeBytes: number
  contentType: string
  sha256: string
  uploadedBy: string
  uploadedAt: string
  status: FileValidationStatus
}

export interface UploadFileCommand {
  file: File
  areaCode?: string
  description?: string
}

export function isFileResource(value: unknown): value is FileResource {
  if (!isObject(value)) return false
  return (
    typeof value.fileId === 'string' &&
    typeof value.processId === 'string' &&
    typeof value.name === 'string' &&
    typeof value.sizeBytes === 'number' &&
    typeof value.contentType === 'string' &&
    typeof value.sha256 === 'string' &&
    typeof value.uploadedBy === 'string' &&
    typeof value.uploadedAt === 'string' &&
    isValidationStatus(value.status)
  )
}

function isValidationStatus(value: unknown): value is FileValidationStatus {
  return value === 'RECEIVED' || value === 'VALIDATED' || value === 'REJECTED'
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
