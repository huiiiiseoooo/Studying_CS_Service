export interface Category {
  id: number
  name: string
  /** 1: 대분류, 2: 중분류, 3: 소분류 */
  level: number
  parentId: number | null
  description: string | null
  /** false면 '준비 중'으로 표시하고 선택할 수 없다. */
  active: boolean
}

export interface CategoryList {
  /** 현재 위치까지의 상위 분류 (대 → 중 순서) */
  path: Category[]
  items: Category[]
}

export interface Content {
  id: number
  title: string
  /** 마크다운 텍스트 */
  explanation: string
  videoUrl: string | null
  fallbackImageUrl: string | null
  /** 대 → 중 → 소 */
  path: Category[]
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface ViewStat {
  mediumCategoryId: number
  mediumCategoryName: string
  totalViews: number
}

export interface Statistics {
  start: string
  end: string
  stats: ViewStat[]
}
