import { useEffect, useState, type FormEvent } from 'react'
import { auditApi } from '../api/auditApi'
import { Button } from '../components/atoms/Button'
import { AuditTable } from '../components/organisms/AuditTable'
import type { PageResponse } from '../types/processes'
import type { AuditEntry } from '../types/records'

const PAGE_SIZE = 20

type AuditState =
  | { status: 'loading'; data: null; message: string }
  | { status: 'ready'; data: PageResponse<AuditEntry>; message: string }
  | { status: 'error'; data: null; message: string }

export function AuditPage() {
  const [processFilter, setProcessFilter] = useState('')
  const [appliedFilter, setAppliedFilter] = useState('')
  const [page, setPage] = useState(0)
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [state, setState] = useState<AuditState>({
    status: 'loading',
    data: null,
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()
    void auditApi
      .list(
        {
          page,
          size: PAGE_SIZE,
          processId: appliedFilter || undefined,
        },
        controller.signal,
      )
      .then((data) => setState({ status: 'ready', data, message: '' }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({
          status: 'error',
          data: null,
          message:
            error instanceof Error
              ? error.message
              : 'No fue posible cargar la auditoría.',
        })
      })
    return () => controller.abort()
  }, [appliedFilter, page, refreshVersion])

  const applyFilter = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setState({ status: 'loading', data: null, message: '' })
    setPage(0)
    setAppliedFilter(processFilter.trim())
    setRefreshVersion((value) => value + 1)
  }

  const clearFilter = () => {
    setProcessFilter('')
    setState({ status: 'loading', data: null, message: '' })
    setPage(0)
    setAppliedFilter('')
  }

  const changePage = (nextPage: number) => {
    setState({ status: 'loading', data: null, message: '' })
    setPage(nextPage)
  }

  const refresh = () => {
    setState({ status: 'loading', data: null, message: '' })
    setRefreshVersion((value) => value + 1)
  }

  return (
    <main className="content-page">
      <header className="page-heading page-heading-actions">
        <div>
          <p className="eyebrow">Trazabilidad de cambios</p>
          <h1>Auditoría</h1>
          <p>Historial real de actualizaciones de registros de negocio.</p>
        </div>
        <Button variant="secondary" type="button" onClick={refresh}>
          Actualizar
        </Button>
      </header>

      <section className="data-card" aria-labelledby="audit-filter-title">
        <div className="data-card-heading">
          <div>
            <p className="label">Consulta</p>
            <h2 id="audit-filter-title">Filtrar eventos</h2>
          </div>
        </div>
        <form className="audit-filter" onSubmit={applyFilter}>
          <label className="field">
            <span className="field-label">ID de proceso</span>
            <input
              value={processFilter}
              onChange={(event) => setProcessFilter(event.target.value)}
              placeholder="UUID del proceso"
            />
          </label>
          <Button type="submit">Aplicar filtro</Button>
          {appliedFilter && (
            <Button variant="secondary" type="button" onClick={clearFilter}>
              Limpiar
            </Button>
          )}
        </form>
      </section>

      <section className="data-card" aria-live="polite">
        {state.status === 'loading' && <p>Cargando auditoría…</p>}
        {state.status === 'error' && (
          <div className="error-panel" role="alert">
            <p>{state.message}</p>
          </div>
        )}
        {state.status === 'ready' && state.data.content.length === 0 && (
          <p className="empty-state">
            No existen eventos para los criterios seleccionados.
          </p>
        )}
        {state.status === 'ready' && state.data.content.length > 0 && (
          <>
            <AuditTable entries={state.data.content} />
            <div className="pagination" aria-label="Paginación de auditoría">
              <Button
                variant="secondary"
                type="button"
                onClick={() => changePage(page - 1)}
                disabled={page === 0}
              >
                Anterior
              </Button>
              <span>
                Página {state.data.page + 1} de {state.data.totalPages}
              </span>
              <Button
                variant="secondary"
                type="button"
                onClick={() => changePage(page + 1)}
                disabled={page + 1 >= state.data.totalPages}
              >
                Siguiente
              </Button>
            </div>
          </>
        )}
      </section>
    </main>
  )
}
