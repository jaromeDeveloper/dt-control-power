import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import type { ProductionArea } from '../../types/areas'
import type { UploadFileCommand } from '../../types/files'
import { Button } from '../atoms/Button'

const MAX_FILE_BYTES = 5 * 1024 * 1024
const ALLOWED_EXTENSIONS = new Set(['csv', 'txt', 'json', 'pdf'])

interface UploadFormProps {
  areas: ProductionArea[]
  isSubmitting: boolean
  onSubmit: (command: UploadFileCommand) => Promise<void>
}

export function UploadForm({ areas, isSubmitting, onSubmit }: UploadFormProps) {
  const [file, setFile] = useState<File | null>(null)
  const [areaCode, setAreaCode] = useState('')
  const [description, setDescription] = useState('')
  const [validationMessage, setValidationMessage] = useState('')

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0] ?? null
    setFile(selectedFile)
    setValidationMessage(selectedFile ? validateFile(selectedFile) : '')
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!file || isSubmitting) return

    const message = validateFile(file)
    if (message) {
      setValidationMessage(message)
      return
    }

    await onSubmit({
      file,
      areaCode: areaCode || undefined,
      description: description.trim() || undefined,
    })
  }

  return (
    <form className="upload-form" onSubmit={handleSubmit}>
      <label className="field" htmlFor="process-file">
        <span className="field-label">Archivo</span>
        <input
          id="process-file"
          type="file"
          name="file"
          accept=".csv,.txt,.json,.pdf"
          onChange={handleFileChange}
          aria-describedby="file-rules file-error"
          required
          disabled={isSubmitting}
        />
        <span className="field-hint" id="file-rules">
          CSV, TXT, JSON o PDF sintético; máximo 5 MiB.
        </span>
      </label>

      <label className="field" htmlFor="area-code">
        <span className="field-label">Área inicial (opcional)</span>
        <select
          id="area-code"
          value={areaCode}
          onChange={(event) => setAreaCode(event.target.value)}
          disabled={isSubmitting}
        >
          <option value="">Sin área inicial</option>
          {areas.map((area) => (
            <option key={area.code} value={area.code}>
              {area.name}
            </option>
          ))}
        </select>
      </label>

      <label className="field" htmlFor="description">
        <span className="field-label">Descripción (opcional)</span>
        <textarea
          id="description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          maxLength={500}
          rows={3}
          disabled={isSubmitting}
        />
      </label>

      {validationMessage && (
        <p className="form-error" id="file-error" role="alert">
          {validationMessage}
        </p>
      )}

      <Button
        type="submit"
        disabled={isSubmitting || !file || Boolean(validationMessage)}
      >
        {isSubmitting ? 'Creando proceso…' : 'Subir archivo y crear proceso'}
      </Button>
    </form>
  )
}

function validateFile(file: File): string {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!ALLOWED_EXTENSIONS.has(extension)) {
    return 'Formato no permitido. Selecciona CSV, TXT, JSON o PDF.'
  }
  if (file.size > MAX_FILE_BYTES) {
    return 'El archivo supera el límite de 5 MiB.'
  }
  if (file.size === 0) {
    return 'El archivo está vacío.'
  }
  return ''
}
