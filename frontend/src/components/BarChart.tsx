import { useState } from 'react'
import type { ViewStat } from '../api/types.ts'

/** 세로축 눈금으로 쓰기 좋은 최댓값(1·2·5 × 10ⁿ)을 고른다. */
function niceMax(value: number): number {
  if (value <= 4) return 4
  const magnitude = 10 ** Math.floor(Math.log10(value))
  for (const step of [1, 2, 4, 5, 8, 10]) {
    if (value <= step * magnitude) return step * magnitude
  }
  return 10 * magnitude
}

const formatCount = (n: number) => n.toLocaleString('ko-KR')

/**
 * 중분류별 조회수 막대 그래프. 가로축: 중분류, 세로축: 조회수 합계. (UC-004)
 * 막대에 마우스를 올리거나 키보드로 초점을 주면 중분류명과 합계를 숫자로 보여 준다.
 */
export function BarChart({ stats }: { stats: ViewStat[] }) {
  const [activeId, setActiveId] = useState<number | null>(null)
  const max = niceMax(Math.max(0, ...stats.map((s) => s.totalViews)))
  const ticks = [1, 0.75, 0.5, 0.25, 0].map((ratio) => max * ratio)

  return (
    <figure className="chart">
      <div className="chart__axis-title">조회수 합계</div>
      <div className="chart__body">
        <div className="chart__ticks" aria-hidden="true">
          {ticks.map((tick) => (
            <span key={tick}>{formatCount(tick)}</span>
          ))}
        </div>
        <ul className="chart__plot">
          {stats.map((stat) => {
            const active = activeId === stat.mediumCategoryId
            return (
              <li
                key={stat.mediumCategoryId}
                className="chart__col"
                tabIndex={0}
                aria-label={`${stat.mediumCategoryName} ${formatCount(stat.totalViews)}회`}
                onMouseEnter={() => setActiveId(stat.mediumCategoryId)}
                onMouseLeave={() => setActiveId(null)}
                onFocus={() => setActiveId(stat.mediumCategoryId)}
                onBlur={() => setActiveId(null)}
              >
                <div className="chart__track">
                  <div
                    className={`chart__bar${active ? ' chart__bar--active' : ''}`}
                    style={{ height: `${(stat.totalViews / max) * 100}%` }}
                  >
                    {active && (
                      <div className="chart__tip" role="tooltip">
                        <strong>{stat.mediumCategoryName}</strong>
                        <span>{formatCount(stat.totalViews)}회</span>
                      </div>
                    )}
                  </div>
                </div>
                <span className="chart__label">{stat.mediumCategoryName}</span>
              </li>
            )
          })}
        </ul>
      </div>
      <figcaption className="chart__axis-title chart__axis-title--x">중분류</figcaption>
    </figure>
  )
}
