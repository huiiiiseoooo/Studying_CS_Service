import { Link } from 'react-router'
import { Notice } from '../components/States.tsx'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'

export function NotFoundPage() {
  useDocumentTitle('페이지를 찾을 수 없습니다')
  return (
    <Notice title="페이지를 찾을 수 없습니다.">
      <Link to="/" className="btn">
        처음 화면으로
      </Link>
    </Notice>
  )
}
