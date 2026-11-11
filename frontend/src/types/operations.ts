export type AreaEventType =
  | 'RECEIVED'
  | 'STARTED'
  | 'COMPLETED'
  | 'WARNING'
  | 'FAILED'

export type AreaEventStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'WARNING'
  | 'FAILED'

export type SemaphoreColor = 'GRAY' | 'YELLOW' | 'GREEN' | 'RED'

export type SemaphoreStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'WARNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'TIMEOUT'

export interface AreaEvent {
  eventId: string
  processId: string
  areaCode: string
  eventType: AreaEventType
  status: AreaEventStatus
  actorId: string
  occurredAt: string
  detail: string | null
  traceId: string | null
  semaphore: SemaphoreColor
}

export interface AreaSemaphore {
  areaCode: string
  color: SemaphoreColor
  status: SemaphoreStatus
  lastEventAt: string | null
}

export interface ProcessSemaphores {
  processId: string
  areas: AreaSemaphore[]
}

export interface AreaSemaphoreSummary {
  areaCode: string
  active: number
  green: number
  yellow: number
  red: number
  gray: number
}

export interface SemaphoreSummary {
  areas: AreaSemaphoreSummary[]
}

export const EVENT_ACTIONS: ReadonlyArray<{
  eventType: AreaEventType
  status: AreaEventStatus
  label: string
}> = [
  { eventType: 'RECEIVED', status: 'PENDING', label: 'Recibido' },
  { eventType: 'STARTED', status: 'RUNNING', label: 'Iniciar' },
  { eventType: 'COMPLETED', status: 'SUCCESS', label: 'Completar' },
  { eventType: 'WARNING', status: 'WARNING', label: 'Advertencia' },
  { eventType: 'FAILED', status: 'FAILED', label: 'Fallar' },
]

export function isAreaEvent(value: unknown): value is AreaEvent {
  if (!isObject(value)) return false
  return (
    typeof value.eventId === 'string' &&
    typeof value.processId === 'string' &&
    typeof value.areaCode === 'string' &&
    isAreaEventType(value.eventType) &&
    isAreaEventStatus(value.status) &&
    typeof value.actorId === 'string' &&
    typeof value.occurredAt === 'string' &&
    isNullableString(value.detail) &&
    isNullableString(value.traceId) &&
    isSemaphoreColor(value.semaphore)
  )
}

export function isAreaEventList(value: unknown): value is AreaEvent[] {
  return Array.isArray(value) && value.every(isAreaEvent)
}

export function isProcessSemaphores(
  value: unknown,
): value is ProcessSemaphores {
  return (
    isObject(value) &&
    typeof value.processId === 'string' &&
    Array.isArray(value.areas) &&
    value.areas.every(isAreaSemaphore)
  )
}

export function isSemaphoreSummary(value: unknown): value is SemaphoreSummary {
  return (
    isObject(value) &&
    Array.isArray(value.areas) &&
    value.areas.every(isAreaSemaphoreSummary)
  )
}

function isAreaSemaphore(value: unknown): value is AreaSemaphore {
  return (
    isObject(value) &&
    typeof value.areaCode === 'string' &&
    isSemaphoreColor(value.color) &&
    isSemaphoreStatus(value.status) &&
    isNullableString(value.lastEventAt)
  )
}

function isAreaSemaphoreSummary(value: unknown): value is AreaSemaphoreSummary {
  return (
    isObject(value) &&
    typeof value.areaCode === 'string' &&
    isCount(value.active) &&
    isCount(value.green) &&
    isCount(value.yellow) &&
    isCount(value.red) &&
    isCount(value.gray)
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

function isSemaphoreColor(value: unknown): value is SemaphoreColor {
  return (
    value === 'GRAY' ||
    value === 'YELLOW' ||
    value === 'GREEN' ||
    value === 'RED'
  )
}

function isSemaphoreStatus(value: unknown): value is SemaphoreStatus {
  return (
    value === 'PENDING' ||
    value === 'RUNNING' ||
    value === 'WARNING' ||
    value === 'COMPLETED' ||
    value === 'FAILED' ||
    value === 'TIMEOUT'
  )
}

function isCount(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0
}

function isNullableString(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
