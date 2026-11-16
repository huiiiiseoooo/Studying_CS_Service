import { request } from './client.ts'
import type { LoginResult, Statistics } from './types.ts'

export const login = (id: string, password: string) =>
  request<LoginResult>('/api/auth/login', { method: 'POST', body: { id, password } })

export const getStatistics = (token: string, start: string, end: string, signal?: AbortSignal) =>
  request<Statistics>(`/api/admin/statistics?start=${start}&end=${end}`, { token, signal })
