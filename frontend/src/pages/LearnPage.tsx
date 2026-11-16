import { Link } from 'react-router'
import { getContent } from '../api/content.ts'
import { AnimationPlayer } from '../components/AnimationPlayer.tsx'
import { BackLink } from '../components/BackLink.tsx'
import { Breadcrumb } from '../components/Breadcrumb.tsx'
import { Explanation } from '../components/Explanation.tsx'
import { Loading, Notice } from '../components/States.tsx'
import { useAsync } from '../hooks/useAsync.ts'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'
import { useIdParam } from '../hooks/useIdParam.ts'
import { NotFoundPage } from './NotFoundPage.tsx'

/** 학습 화면: 애니메이션과 그 아래 설명. (FR-004, FR-005) */
export function LearnPage() {
  const majorId = useIdParam('majorId')
  const mediumId = useIdParam('mediumId')
  const minorId = useIdParam('minorId')
  const { state, reload } = useAsync(
    (signal) => (minorId === null ? Promise.reject(new Error('invalid id')) : getContent(minorId, signal)),
    [minorId],
  )
  const content = state.status === 'success' ? state.data : undefined
  useDocumentTitle(content?.title)

  if (majorId === null || mediumId === null || minorId === null) return <NotFoundPage />

  const [major, medium, minor] = content?.path ?? []
  const majorHref = `/categories/${major?.id ?? majorId}`
  const mediumHref = `${majorHref}/${medium?.id ?? mediumId}`

  return (
    <>
      <BackLink to={mediumHref} label="소분류 목록" />
      {state.status === 'loading' && <Loading />}
      {state.status === 'error' &&
        (state.error.status === 404 ? (
          <Notice title="해당 콘텐츠를 이용할 수 없습니다.">
            <Link to={mediumHref} className="btn">
              소분류 목록으로
            </Link>
          </Notice>
        ) : (
          <Notice title="학습 내용을 불러오지 못했습니다.">
            <button type="button" className="btn" onClick={reload}>
              다시 시도
            </button>
          </Notice>
        ))}
      {content && major && medium && minor && (
        <article className="learn">
          <Breadcrumb
            items={[
              { label: '대분류', to: '/' },
              { label: major.name, to: majorHref },
              { label: medium.name, to: mediumHref },
              { label: minor.name },
            ]}
          />
          <h1 className="learn__title">{content.title}</h1>
          <AnimationPlayer key={content.id} src={content.videoUrl} fallbackSrc={content.fallbackImageUrl} title={content.title} />
          <Explanation markdown={content.explanation} />
        </article>
      )}
    </>
  )
}
