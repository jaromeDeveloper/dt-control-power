import type { ComponentProps } from 'react'

type ButtonProps = ComponentProps<'button'> & {
  variant?: 'primary' | 'secondary'
}

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      className={`button button-${variant} ${className}`.trim()}
      {...props}
    />
  )
}
