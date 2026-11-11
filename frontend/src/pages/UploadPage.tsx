import { useEffect, useState } from 'react'
import { areasApi } from '../api/areasApi'
import { ApiError } from '../api/client'
import { filesApi } from '../api/filesApi'
import { FileDetailsCard } from '../components/molecules/FileDetailsCard'
import { UploadForm } from '../components/molecules/UploadForm'
import type { ProductionArea } from '../types/areas'
import type { FileResource, UploadFileCommand } from '../types/files'

type AreasState =
  | { status: 'loading'; data: ProductionArea[]; message: string }
  | { status: 'ready'; data: ProductionArea[]; message: string }
  | { status: 'error'; data: ProductionArea[]; message: string }

export function UploadPage() {
  const [areasState, setAreasState] = useState<AreasState>({
    status: 'loading',
    data: [],
    message: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [result, setResult] = useState<FileResource | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    void areasApi
      .list(controller.signal)
      .then((data) => setAreasState({ status: 'ready', data, message: '' }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setAreasState({
          status: 'error',
          data: [],
          message: toMessage(error),
        })
      })
    return () => controller.abort()
  }, [])

  const handleUpload = async (command: UploadFileCommand) => {
    if (isSubmitting) return
    setIsSubmitting(true)
    setUploadError('')
    setResult(null)
    try {
      setResult(await filesApi.upload(command))
    } catch (error) {
      setUploadError(toUploadMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="content-page">
      <header className="page-heading">
        <p className="eyebrow">Archivo → proceso</p>
        <h1>Cargar archivo</h1>
        <p>
          El backend validará el archivo, calculará SHA-256 y creará un proceso
          con checkpoints persistidos.
        </p>
      </header>

      <section className="data-card" aria-labelledby="upload-title">
        <div className="data-card-heading">
          <div>
            <p className="label">Nueva carga</p>
            <h2 id="upload-title">Datos del archivo</h2>
          </div>
        </div>

        {areasState.status === 'loading' && (
          <p className="status-message" aria-live="polite">
            Cargando áreas…
          </p>
        )}
        {areasState.status === 'error' && (
          <p className="form-error" role="alert">
            {areasState.message}
          </p>
        )}
        {areasState.status === 'ready' && (
          <UploadForm
            areas={areasState.data}
            isSubmitting={isSubmitting}
            onSubmit={handleUpload}
          />
        )}
        {uploadError && (
          <p className="form-error" role="alert">
            {uploadError}
          </p>
        )}
      </section>

      {result && <FileDetailsCard file={result} showProcessLink />}
    </main>
  )
}

function toUploadMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 413) return 'El archivo supera el límite permitido.'
    if (error.status === 400) return error.message
    return error.message
  }
  return toMessage(error)
}

function toMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : 'No fue posible conectar con el backend.'
}
