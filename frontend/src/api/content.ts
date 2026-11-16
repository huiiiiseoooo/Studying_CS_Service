import { request } from './client.ts'
import type { CategoryList, Content } from './types.ts'

export const getMajorCategories = (signal?: AbortSignal) =>
  request<CategoryList>('/api/major-categories', { signal })

export const getMediumCategories = (majorId: number, signal?: AbortSignal) =>
  request<CategoryList>(`/api/major-categories/${majorId}/medium-categories`, { signal })

export const getMinorCategories = (mediumId: number, signal?: AbortSignal) =>
  request<CategoryList>(`/api/medium-categories/${mediumId}/minor-categories`, { signal })

export const getContent = (minorId: number, signal?: AbortSignal) =>
  request<Content>(`/api/minor-categories/${minorId}/content`, { signal })
