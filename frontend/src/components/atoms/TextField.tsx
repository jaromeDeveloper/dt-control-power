import type { ComponentProps } from 'react'

interface TextFieldProps extends ComponentProps<'input'> {
  label: string
  hint?: string
}

export function TextField({ label, hint, id, ...props }: TextFieldProps) {
  const inputId = id ?? props.name
  const hintId = hint && inputId ? `${inputId}-hint` : undefined

  return (
    <label className="field" htmlFor={inputId}>
      <span className="field-label">{label}</span>
      <input id={inputId} aria-describedby={hintId} {...props} />
      {hint && (
        <span className="field-hint" id={hintId}>
          {hint}
        </span>
      )}
    </label>
  )
}
