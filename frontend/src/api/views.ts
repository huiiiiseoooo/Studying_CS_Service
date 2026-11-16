import { request } from './client.ts'

/**
 * 중분류 선택 1회를 기록한다. (FR-006)
 * 실패해도 탐색이 멈추면 안 되므로 결과를 기다리지 않고 오류도 삼킨다. (QC-007)
 * keepalive: 화면이 바로 바뀌어도 요청이 취소되지 않게 한다.
 */
export function recordView(mediumCategoryId: number): void {
  request<void>('/api/views', { method: 'POST', body: { mediumCategoryId }, keepalive: true }).catch(() => {})
}
