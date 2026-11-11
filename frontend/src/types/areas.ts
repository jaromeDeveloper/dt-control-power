export interface ProductionArea {
  code: string
  name: string
  warningAfterSeconds: number
  timeoutAfterSeconds: number
}

export function isProductionAreaList(value: unknown): value is ProductionArea[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        typeof item === 'object' &&
        item !== null &&
        typeof item.code === 'string' &&
        typeof item.name === 'string' &&
        typeof item.warningAfterSeconds === 'number' &&
        typeof item.timeoutAfterSeconds === 'number',
    )
  )
}
