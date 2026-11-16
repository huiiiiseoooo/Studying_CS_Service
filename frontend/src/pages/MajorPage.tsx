import { getMajorCategories } from '../api/content.ts'
import { CategoryGrid } from '../components/CategoryGrid.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { Loading, Notice } from '../components/States.tsx'
import { useAsync } from '../hooks/useAsync.ts'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'

/** 대분류 목록. (FR-001) */
export function MajorPage() {
  useDocumentTitle()
  const { state, reload } = useAsync((signal) => getMajorCategories(signal), [])

  return (
    <>
      <PageHeader
        eyebrow="대분류"
        title="무엇을 공부할까요?"
        lead="분야를 고르면 주제별로 동작 과정을 애니메이션과 설명으로 볼 수 있습니다."
      />
      {state.status === 'loading' && <Loading />}
      {state.status === 'error' && (
        <Notice title="분류 목록을 불러오지 못했습니다.">
          <button type="button" className="btn" onClick={reload}>
            다시 시도
          </button>
        </Notice>
      )}
      {state.status === 'success' &&
        (state.data.items.length === 0 ? (
          <Notice title="등록된 학습 항목이 없습니다." />
        ) : (
          <CategoryGrid items={state.data.items} hrefOf={(c) => `/categories/${c.id}`} />
        ))}
    </>
  )
}
