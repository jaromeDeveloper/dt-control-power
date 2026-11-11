import { useEffect, useState } from 'react'
import { processesApi } from '../api/processesApi'
import { Button } from '../components/atoms/Button'
import { ProcessesTable } from '../components/organisms/ProcessesTable'
import type { PageResponse, ProcessSummary } from '../types/processes'

const PAGE_SIZE = 20

type PageState =
  | { status: 'loading'; data: null; message: string }
  | { status: 'ready'; data: PageResponse<ProcessSummary>; message: string }
  | { status: 'error'; data: null; message: string }

export function ProcessesPage() {
  const [page, setPage] = useState(0)
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [state, setState] = useState<PageState>({
    status: 'loading',
    data: null,
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()
    void processesApi
      .list({ page, size: PAGE_SIZE }, controller.signal)
      .then((data) => setState({ status: 'ready', data, message: '' }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({
          status: 'error',
          data: null,
          message:
            error instanceof Error
              ? error.message
              : 'No fue posible cargar los procesos.',
        })
      })
    return () => controller.abort()
  }, [page, refreshVersion])

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
          <p className="eyebrow">Trazabilidad funcional</p>
          <h1>Procesos</h1>
          <p>Procesos creados desde cargas reales y persistidos por el backend.</p>
        </div>
        <Button variant="secondary" type="button" onClick={refresh}>
          Actualizar
        </Button>
      </header>

      <section className="data-card" aria-live="polite">
        {state.status === 'loading' && <p>Cargando procesos…</p>}
        {state.status === 'error' && (
          <div className="error-panel" role="alert">
            <p>{state.message}</p>
          </div>
        )}
        {state.status === 'ready' && state.data.content.length === 0 && (
          <p className="empty-state">Todavía no existen procesos.</p>
        )}
        {state.status === 'ready' && state.data.content.length > 0 && (
          <>
            <ProcessesTable processes={state.data.content} />
            <div className="pagination" aria-label="Paginación de procesos">
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
