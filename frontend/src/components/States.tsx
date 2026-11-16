import type { ReactNode } from 'react'

export function Loading({ label = '불러오는 중입니다' }: { label?: string }) {
  return (
    <div className="state" role="status">
      <span className="spinner" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

interface NoticeProps {
  title: string
  children?: ReactNode
}

/** 오류·빈 목록·없는 화면에 쓰는 안내 상자. children에는 다음에 할 수 있는 행동(버튼)을 넣는다. */
export function Notice({ title, children }: NoticeProps) {
  return (
    <div className="state state--notice" role="alert">
      <p className="state__title">{title}</p>
      {children && <div className="state__actions">{children}</div>}
    </div>
  )
}
