import { Link } from 'react-router'
import { getMediumCategories } from '../api/content.ts'
import { recordView } from '../api/views.ts'
import { BackLink } from '../components/BackLink.tsx'
import { Breadcrumb } from '../components/Breadcrumb.tsx'
import { CategoryGrid } from '../components/CategoryGrid.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { Loading, Notice } from '../components/States.tsx'
import { useAsync } from '../hooks/useAsync.ts'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'
import { useIdParam } from '../hooks/useIdParam.ts'
import { NotFoundPage } from './NotFoundPage.tsx'

/** 대분류에 속한 중분류 목록. (FR-002) 중분류를 선택하면 조회수를 1회 기록한다. (FR-006) */
export function MediumPage() {
  const majorId = useIdParam('majorId')
  const { state, reload } = useAsync(
    (signal) => (majorId === null ? Promise.reject(new Error('invalid id')) : getMediumCategories(majorId, signal)),
    [majorId],
  )
  const major = state.status === 'success' ? state.data.path[0] : undefined
  useDocumentTitle(major?.name)

  if (majorId === null) return <NotFoundPage />

  return (
    <>
      <BackLink to="/" label="대분류 목록" />
      {state.status === 'loading' && <Loading />}
      {state.status === 'error' &&
        (state.error.status === 404 ? (
          <Notice title="분류를 찾을 수 없습니다.">
            <Link to="/" className="btn">
              대분류 목록으로
            </Link>
          </Notice>
        ) : (
          <Notice title="분류 목록을 불러오지 못했습니다.">
            <button type="button" className="btn" onClick={reload}>
              다시 시도
            </button>
          </Notice>
        ))}
      {state.status === 'success' && major && (
        <>
          <Breadcrumb items={[{ label: '대분류', to: '/' }, { label: major.name }]} />
          <PageHeader eyebrow="중분류" title={major.name} lead={major.description} />
          {state.data.items.length === 0 ? (
            <Notice title="등록된 학습 항목이 없습니다.">
              <Link to="/" className="btn">
                다른 분류 선택
              </Link>
            </Notice>
          ) : (
            <CategoryGrid
              items={state.data.items}
              hrefOf={(c) => `/categories/${major.id}/${c.id}`}
              onSelect={(c) => recordView(c.id)}
            />
          )}
        </>
      )}
    </>
  )
}
