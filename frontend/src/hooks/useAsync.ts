import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/client.ts'

export type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'error'; error: ApiError }
  | { status: 'success'; data: T }

/** 화면에 들어올 때 데이터를 불러오고, reload()로 다시 시도할 수 있게 한다. */
export function useAsync<T>(load: (signal: AbortSignal) => Promise<T>, deps: readonly unknown[]) {
  const [state, setState] = useState<AsyncState<T>>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    load(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setState({ status: 'success', data })
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          error: error instanceof ApiError ? error : new ApiError(0, 'UNKNOWN', '요청을 처리하지 못했습니다.'),
        })
      })
    return () => controller.abort()
    // load는 매 렌더마다 새로 만들어지므로 호출부가 넘긴 deps로만 다시 불러온다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])
  return { state, reload }
}
