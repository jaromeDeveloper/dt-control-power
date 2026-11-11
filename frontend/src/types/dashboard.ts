import type {
  AreaEventStatus,
  AreaEventType,
  SemaphoreSummary,
} from './operations'
import { isSemaphoreSummary } from './operations'

export interface DashboardRecentEvent {
  processId: string
  areaCode: string
  eventType: AreaEventType
  status: AreaEventStatus
  actor: string
  occurredAt: string
}

export interface DashboardSummary {
  totalProcesses: number
  activeProcesses: number
  completedProcesses: number
  failedProcesses: number
  uploadedFiles: number
  semaphores: SemaphoreSummary
  recentEvents: DashboardRecentEvent[]
}

export function isDashboardSummary(value: unknown): value is DashboardSummary {
  if (!isObject(value)) return false
  return (
    isCount(value.totalProcesses) &&
    isCount(value.activeProcesses) &&
    isCount(value.completedProcesses) &&
    isCount(value.failedProcesses) &&
    isCount(value.uploadedFiles) &&
    isSemaphoreSummary(value.semaphores) &&
    Array.isArray(value.recentEvents) &&
    value.recentEvents.every(isDashboardRecentEvent)
  )
}

function isDashboardRecentEvent(value: unknown): value is DashboardRecentEvent {
  if (!isObject(value)) return false
  return (
    typeof value.processId === 'string' &&
    typeof value.areaCode === 'string' &&
    isAreaEventType(value.eventType) &&
    isAreaEventStatus(value.status) &&
    typeof value.actor === 'string' &&
    typeof value.occurredAt === 'string'
  )
}

function isAreaEventType(value: unknown): value is AreaEventType {
  return (
    value === 'RECEIVED' ||
    value === 'STARTED' ||
    value === 'COMPLETED' ||
    value === 'WARNING' ||
    value === 'FAILED'
  )
}

function isAreaEventStatus(value: unknown): value is AreaEventStatus {
  return (
    value === 'PENDING' ||
    value === 'RUNNING' ||
    value === 'SUCCESS' ||
    value === 'WARNING' ||
    value === 'FAILED'
  )
}

function isCount(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
