import { useState } from 'react'

interface Props {
  src: string | null
  fallbackSrc: string | null
  title: string
}

type Phase = 'loading' | 'playing' | 'failed'

/**
 * 학습 애니메이션. 들어오면 자동으로 무한 반복 재생하며 재생 제어는 없다. (QC-013)
 * 설명 텍스트와 따로 불러오므로 로딩이 늦어도 설명 표시를 막지 않는다. (QC-001)
 * 불러오지 못하면 대체 이미지와 오류 안내를 보여 준다. (FR-005, QC-006)
 * 다른 콘텐츠로 바뀔 때는 호출부가 key를 바꿔 상태를 처음부터 다시 시작시킨다.
 */
export function AnimationPlayer({ src, fallbackSrc, title }: Props) {
  const [phase, setPhase] = useState<Phase>(src ? 'loading' : 'failed')
  const [fallbackFailed, setFallbackFailed] = useState(false)


  const isVideo = src ? /\.(mp4|webm)(\?|#|$)/i.test(src) : false
  const failed = () => setPhase('failed')
  const ready = () => setPhase('playing')

  return (
    <figure className="player">
      <div className={`player__stage player__stage--${phase}`}>
        {phase !== 'failed' && src && (
          isVideo ? (
            <video
              key={src}
              className="player__media"
              src={src}
              autoPlay
              loop
              muted
              playsInline
              aria-label={`${title} 동작 애니메이션`}
              onLoadedData={ready}
              onError={failed}
            />
          ) : (
            <img
              key={src}
              className="player__media"
              src={src}
              alt={`${title} 동작 애니메이션`}
              onLoad={ready}
              onError={failed}
            />
          )
        )}
        {phase === 'loading' && (
          <div className="player__loading" role="status">
            <span className="spinner" aria-hidden="true" />
            애니메이션을 불러오는 중입니다
          </div>
        )}
        {phase === 'failed' && fallbackSrc && !fallbackFailed && (
          <img
            className="player__media"
            src={fallbackSrc}
            alt={`${title} 대체 이미지`}
            onError={() => setFallbackFailed(true)}
          />
        )}
        {phase === 'failed' && (!fallbackSrc || fallbackFailed) && (
          <div className="player__empty">표시할 이미지가 없습니다</div>
        )}
      </div>
      {phase === 'failed' && (
        <figcaption className="player__error" role="alert">
          애니메이션을 불러오지 못했습니다. {fallbackSrc && !fallbackFailed ? '대체 이미지와 ' : ''}아래 설명으로
          학습할 수 있습니다.
        </figcaption>
      )}
    </figure>
  )
}
