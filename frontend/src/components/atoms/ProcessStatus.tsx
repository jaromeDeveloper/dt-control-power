import type { CheckpointStatus, ProcessStatus } from '../../types/processes'

interface ProcessStatusProps {
  value: ProcessStatus | CheckpointStatus
}

export function ProcessStatusLabel({ value }: ProcessStatusProps) {
  return (
    <span className={`process-status process-status-${value.toLowerCase()}`}>
      {value}
    </span>
  )
}
