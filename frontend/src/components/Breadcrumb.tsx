import { Link } from 'react-router'

export interface Crumb {
  label: string
  to?: string
}

/** 현재 분류 위치. 마지막 항목이 현재 화면이다. */
export function Breadcrumb({ items }: { items: Crumb[] }) {
  return (
    <nav className="crumbs" aria-label="현재 위치">
      <ol>
        {items.map((item, index) => (
          <li key={index}>
            {item.to ? <Link to={item.to}>{item.label}</Link> : <span aria-current="page">{item.label}</span>}
          </li>
        ))}
      </ol>
    </nav>
  )
}
