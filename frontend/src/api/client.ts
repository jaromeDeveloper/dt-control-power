const API_BASE_URL = '/api/v1'

interface ApiProblem {
  code?: string
  message?: string
  detail?: string
  title?: string
  requestId?: string
}

interface CsrfToken {
  headerName: string
  token: string
}

interface RequestOptions {
  csrf?: boolean
  retryAfterCsrfFailure?: boolean
}

let currentCsrfToken: CsrfToken | null = null
const unauthorizedListeners = new Set<() => void>()

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly requestId?: string

  constructor(status: number, problem: ApiProblem) {
    super(
      problem.message ??
        problem.detail ??
        problem.title ??
        `La solicitud falló con HTTP ${status}`,
    )
    this.name = 'ApiError'
    this.status = status
    this.code = problem.code
    this.requestId = problem.requestId
  }
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (
    init.body &&
    !(init.body instanceof FormData) &&
    !headers.has('Content-Type')
  ) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.csrf) {
    const csrf = await getCsrfToken()
    headers.set(csrf.headerName, csrf.token)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })

  if (
    response.status === 403 &&
    options.csrf &&
    options.retryAfterCsrfFailure !== false
  ) {
    clearCsrfToken()
    return apiRequest<T>(path, init, {
      ...options,
      retryAfterCsrfFailure: false,
    })
  }

  if (!response.ok) {
    if (response.status === 401) {
      unauthorizedListeners.forEach((listener) => listener())
    }
    throw await createApiError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export function clearCsrfToken() {
  currentCsrfToken = null
}

export function onUnauthorized(listener: () => void): () => void {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

async function getCsrfToken(): Promise<CsrfToken> {
  if (currentCsrfToken) return currentCsrfToken

  const response = await fetch(`${API_BASE_URL}/auth/csrf`, {
    headers: { Accept: 'application/json' },
    credentials: 'include',
  })

  if (!response.ok) {
    throw await createApiError(response)
  }

  const payload: unknown = await response.json()
  if (!isCsrfToken(payload)) {
    throw new Error('El backend devolvió un token CSRF inválido')
  }

  currentCsrfToken = payload
  return payload
}

async function createApiError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('json')) {
    try {
      const payload: unknown = await response.json()
      if (typeof payload === 'object' && payload !== null) {
        return new ApiError(response.status, payload as ApiProblem)
      }
    } catch {
      // Use the safe fallback below when the error body is malformed.
    }
  }

  return new ApiError(response.status, {})
}

function isCsrfToken(value: unknown): value is CsrfToken {
  if (typeof value !== 'object' || value === null) return false

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.headerName === 'string' &&
    typeof candidate.token === 'string'
  )
}
