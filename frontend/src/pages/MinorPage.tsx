import { Link } from 'react-router'
import { getMinorCategories } from '../api/content.ts'
import { BackLink } from '../components/BackLink.tsx'
import { Breadcrumb } from '../components/Breadcrumb.tsx'
import { CategoryGrid } from '../components/CategoryGrid.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { Loading, Notice } from '../components/States.tsx'
import { useAsync } from '../hooks/useAsync.ts'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'
import { useIdParam } from '../hooks/useIdParam.ts'
import { NotFoundPage } from './NotFoundPage.tsx'

/** 중분류에 속한 소분류(학습 주제) 목록. (FR-003) */
export function MinorPage() {
  const majorId = useIdParam('majorId')
  const mediumId = useIdParam('mediumId')
  const { state, reload } = useAsync(
    (signal) => (mediumId === null ? Promise.reject(new Error('invalid id')) : getMinorCategories(mediumId, signal)),
    [mediumId],
  )
  const [major, medium] = state.status === 'success' ? state.data.path : []
  useDocumentTitle(medium?.name)

  if (majorId === null || mediumId === null) return <NotFoundPage />
  const parentHref = `/categories/${major?.id ?? majorId}`

  return (
    <>
      <BackLink to={parentHref} label="중분류 목록" />
      {state.status === 'loading' && <Loading />}
      {state.status === 'error' &&
        (state.error.status === 404 ? (
          <Notice title="분류를 찾을 수 없습니다.">
            <Link to={parentHref} className="btn">
              중분류 목록으로
            </Link>
          </Notice>
        ) : (
          <Notice title="분류 목록을 불러오지 못했습니다.">
            <button type="button" className="btn" onClick={reload}>
              다시 시도
            </button>
          </Notice>
        ))}
      {state.status === 'success' && major && medium && (
        <>
          <Breadcrumb
            items={[{ label: '대분류', to: '/' }, { label: major.name, to: parentHref }, { label: medium.name }]}
          />
          <PageHeader eyebrow="소분류" title={medium.name} lead={medium.description} />
          {state.data.items.length === 0 ? (
            <Notice title="등록된 학습 항목이 없습니다.">
              <Link to={parentHref} className="btn">
                다른 분류 선택
              </Link>
            </Notice>
          ) : (
            <CategoryGrid items={state.data.items} hrefOf={(c) => `/categories/${major.id}/${medium.id}/${c.id}`} />
          )}
        </>
      )}
    </>
  )
}
