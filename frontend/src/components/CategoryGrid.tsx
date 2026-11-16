import { Link } from 'react-router'
import type { Category } from '../api/types.ts'

interface Props {
  items: Category[]
  hrefOf: (category: Category) => string
  /** 항목을 선택(클릭)했을 때 한 번 호출된다. */
  onSelect?: (category: Category) => void
}

/** 대·중·소 분류 목록 공통 카드 그리드. active가 false인 분류는 선택할 수 없다. */
export function CategoryGrid({ items, hrefOf, onSelect }: Props) {
  return (
    <ul className="grid">
      {items.map((category, index) => {
        const body = (
          <>
            <span className="card__index">{String(index + 1).padStart(2, '0')}</span>
            <span className="card__name">{category.name}</span>
            {category.description && <span className="card__desc">{category.description}</span>}
            {category.active ? (
              <span className="card__go" aria-hidden="true">
                →
              </span>
            ) : (
              <span className="pill card__soon">준비 중</span>
            )}
          </>
        )
        return (
          <li key={category.id}>
            {category.active ? (
              <Link to={hrefOf(category)} className="card" onClick={() => onSelect?.(category)}>
                {body}
              </Link>
            ) : (
              <div className="card card--disabled" aria-disabled="true">
                {body}
              </div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
