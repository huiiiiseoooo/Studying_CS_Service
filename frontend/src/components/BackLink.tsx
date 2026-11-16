import { Link } from 'react-router'

/** 화면의 뒤로가기 버튼. 항상 바로 위 부모 분류 목록으로 간다. (QC-008) */
export function BackLink({ to, label }: { to: string; label: string }) {
  return (
    <Link to={to} className="back">
      <span aria-hidden="true">←</span> {label}
    </Link>
  )
}
