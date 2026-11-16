import type { ReactNode } from 'react'

interface Props {
  eyebrow?: string
  title: string
  lead?: string | null
  children?: ReactNode
}

export function PageHeader({ eyebrow, title, lead, children }: Props) {
  return (
    <header className="page-head">
      {eyebrow && <p className="page-head__eyebrow">{eyebrow}</p>}
      <h1 className="page-head__title">{title}</h1>
      {lead && <p className="page-head__lead">{lead}</p>}
      {children}
    </header>
  )
}
