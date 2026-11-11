import type { FileResource } from './files'
import { isFileResource } from './files'

export type ProcessStatus =
  | 'CREATED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'

export type CheckpointStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'WARNING'
  | 'FAILED'
  | 'TIMEOUT'
  | 'CANCELLED'

export interface ProcessSummary {
  processId: string
  createdBy: string
  status: ProcessStatus
  startedAt: string
  finishedAt: string | null
  description: string | null
  file: FileResource
}

export type ProcessDetail = ProcessSummary

export interface Checkpoint {
  checkpointId: string
  stage: string
  areaCode: string | null
  status: CheckpointStatus
  startedAt: string
  finishedAt: string | null
  durationMs: number | null
  actorId: string | null
  errorCode: string | null
  safeMessage: string | null
  traceId: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export function isProcessPage(value: unknown): value is PageResponse<ProcessSummary> {
  if (!isObject(value)) return false
  return (
    Array.isArray(value.content) &&
    value.content.every(isProcessSummary) &&
    typeof value.page === 'number' &&
    typeof value.size === 'number' &&
    typeof value.totalElements === 'number' &&
    typeof value.totalPages === 'number'
  )
}

export function isProcessDetail(value: unknown): value is ProcessDetail {
  return isProcessSummary(value)
}

export function isCheckpointList(value: unknown): value is Checkpoint[] {
  return Array.isArray(value) && value.every(isCheckpoint)
}

function isProcessSummary(value: unknown): value is ProcessSummary {
  if (!isObject(value)) return false
  return (
    typeof value.processId === 'string' &&
    typeof value.createdBy === 'string' &&
    isProcessStatus(value.status) &&
    typeof value.startedAt === 'string' &&
    (value.finishedAt === null || typeof value.finishedAt === 'string') &&
    (value.description === null || typeof value.description === 'string') &&
    isFileResource(value.file)
  )
}

function isCheckpoint(value: unknown): value is Checkpoint {
  if (!isObject(value)) return false
  return (
    typeof value.checkpointId === 'string' &&
    typeof value.stage === 'string' &&
    (value.areaCode === null || typeof value.areaCode === 'string') &&
    isCheckpointStatus(value.status) &&
    typeof value.startedAt === 'string' &&
    (value.finishedAt === null || typeof value.finishedAt === 'string') &&
    (value.durationMs === null || typeof value.durationMs === 'number') &&
    (value.actorId === null || typeof value.actorId === 'string') &&
    (value.errorCode === null || typeof value.errorCode === 'string') &&
    (value.safeMessage === null || typeof value.safeMessage === 'string') &&
    (value.traceId === null || typeof value.traceId === 'string')
  )
}

function isProcessStatus(value: unknown): value is ProcessStatus {
  return (
    value === 'CREATED' ||
    value === 'IN_PROGRESS' ||
    value === 'COMPLETED' ||
    value === 'FAILED' ||
    value === 'CANCELLED'
  )
}

function isCheckpointStatus(value: unknown): value is CheckpointStatus {
  return (
    value === 'PENDING' ||
    value === 'RUNNING' ||
    value === 'SUCCESS' ||
    value === 'WARNING' ||
    value === 'FAILED' ||
    value === 'TIMEOUT' ||
    value === 'CANCELLED'
  )
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
