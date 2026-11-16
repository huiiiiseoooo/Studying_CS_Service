const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

/** API 호출 실패. status 0은 서버에 닿지 못한 경우(네트워크 오류)다. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
  token?: string
  signal?: AbortSignal
  keepalive?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (options.token) headers.Authorization = `Bearer ${options.token}`

  let response: Response
  try {
    response = await fetch(BASE_URL + path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
      keepalive: options.keepalive,
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
    throw new ApiError(0, 'NETWORK', '서버에 연결하지 못했습니다.')
  }

  if (!response.ok) {
    let code = 'UNKNOWN'
    let message = '요청을 처리하지 못했습니다.'
    try {
      const body = (await response.json()) as { code?: string; message?: string }
      if (body.code) code = body.code
      if (body.message) message = body.message
    } catch {
      // 본문이 JSON이 아니면 기본 문구를 쓴다.
    }
    throw new ApiError(response.status, code, message)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
