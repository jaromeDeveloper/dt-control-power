import type { PageResponse } from './processes'

export interface BusinessRecord {
  id: string
  processId: string
  reference: string
  value: string
  version: number
  updatedBy: string
  updatedAt: string
}

export type AuditOutcome = 'SUCCESS' | 'FAILED'

export interface AuditEntry {
  id: string
  recordId: string
  processId: string
  actor: string
  oldValue: string | null
  newValue: string | null
  outcome: AuditOutcome
  occurredAt: string
  requestId: string
}

export function isBusinessRecord(value: unknown): value is BusinessRecord {
  if (!isObject(value)) return false
  return (
    typeof value.id === 'string' &&
    typeof value.processId === 'string' &&
    typeof value.reference === 'string' &&
    typeof value.value === 'string' &&
    typeof value.version === 'number' &&
    typeof value.updatedBy === 'string' &&
    typeof value.updatedAt === 'string'
  )
}

export function isBusinessRecordList(
  value: unknown,
): value is BusinessRecord[] {
  return Array.isArray(value) && value.every(isBusinessRecord)
}

export function isAuditPage(value: unknown): value is PageResponse<AuditEntry> {
  if (!isObject(value)) return false
  return (
    Array.isArray(value.content) &&
    value.content.every(isAuditEntry) &&
    typeof value.page === 'number' &&
    typeof value.size === 'number' &&
    typeof value.totalElements === 'number' &&
    typeof value.totalPages === 'number'
  )
}

function isAuditEntry(value: unknown): value is AuditEntry {
  if (!isObject(value)) return false
  return (
    typeof value.id === 'string' &&
    typeof value.recordId === 'string' &&
    typeof value.processId === 'string' &&
    typeof value.actor === 'string' &&
    isNullableString(value.oldValue) &&
    isNullableString(value.newValue) &&
    (value.outcome === 'SUCCESS' || value.outcome === 'FAILED') &&
    typeof value.occurredAt === 'string' &&
    typeof value.requestId === 'string'
  )
}

function isNullableString(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
