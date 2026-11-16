import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { getStatistics } from '../api/admin.ts'
import { ApiError } from '../api/client.ts'
import type { Statistics } from '../api/types.ts'
import { clearSession, getToken } from '../auth/session.ts'
import { BarChart } from '../components/BarChart.tsx'
import { Popup } from '../components/Popup.tsx'
import { Loading } from '../components/States.tsx'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'

/** 브라우저 기준 오늘 날짜 (YYYY-MM-DD). */
function today(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

type View = { status: 'loading' } | { status: 'failed' } | { status: 'loaded'; data: Statistics }

/** 기간별 중분류 이용 통계. (UC-004) */
export function AdminStatsPage() {
  useDocumentTitle('이용 통계')
  const navigate = useNavigate()

  const [start, setStart] = useState(today)
  const [end, setEnd] = useState(today)
  const [hint, setHint] = useState<string | null>(null)
  const [view, setView] = useState<View>({ status: 'loading' })
  const [popup, setPopup] = useState(false)
  const lastRange = useRef({ start, end })
  const requestSeq = useRef(0)

  const load = useCallback(
    async (from: string, to: string) => {
      const token = getToken()
      if (!token) {
        navigate('/admin/login', { replace: true, state: { notice: '로그인이 필요합니다.' } })
        return
      }
      const seq = ++requestSeq.current
      lastRange.current = { start: from, end: to }
      setPopup(false)
      setView({ status: 'loading' })
      try {
        const data = await getStatistics(token, from, to)
        if (seq === requestSeq.current) setView({ status: 'loaded', data })
      } catch (error) {
        if (seq !== requestSeq.current) return
        if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
          // 2a. 세션(토큰)이 없거나 만료됨 → 안내 후 로그인 화면으로
          clearSession()
          navigate('/admin/login', { replace: true, state: { notice: '로그인이 필요합니다.' } })
          return
        }
        // 3a. 그래프 영역을 비우고 팝업으로 알린다. 내부 오류 내용은 보여 주지 않는다.
        setView({ status: 'failed' })
        setPopup(true)
      }
    },
    [navigate],
  )

  // 3. 처음 들어오면 기본 기간(오늘)으로 집계한다.
  useEffect(() => {
    const initial = today()
    // oxlint-disable-next-line react/set-state-in-effect -- 화면 진입 시 1회 조회
    void load(initial, initial)
  }, [load])

  const submit = (event: FormEvent) => {
    event.preventDefault()
    // 5a. 시작일이 종료일보다 늦거나 미래 날짜면 조회하지 않는다.
    const now = today()
    if (!start || !end || start > end || end > now) {
      setHint('조회 기간을 다시 선택해 주세요.')
      return
    }
    setHint(null)
    void load(start, end)
  }

  const retry = () => void load(lastRange.current.start, lastRange.current.end)
  const stats = view.status === 'loaded' ? view.data.stats : []
  const noRecords = view.status === 'loaded' && stats.every((s) => s.totalViews === 0)
  const total = stats.reduce((sum, s) => sum + s.totalViews, 0)

  return (
    <section className="stats">
      <h1 className="stats__title">이용 통계</h1>
      <p className="stats__lead">중분류를 선택한 횟수를 기간별로 합산합니다.</p>

      <form className="period" onSubmit={submit} noValidate>
        <label className="field field--inline">
          <span>시작일</span>
          <input type="date" value={start} max={today()} onChange={(event) => setStart(event.target.value)} />
        </label>
        <span className="period__dash" aria-hidden="true">
          ~
        </span>
        <label className="field field--inline">
          <span>종료일</span>
          <input type="date" value={end} max={today()} onChange={(event) => setEnd(event.target.value)} />
        </label>
        <button type="submit" className="btn" disabled={view.status === 'loading'}>
          조회
        </button>
        {hint && (
          <p className="field__hint period__hint" role="alert">
            {hint}
          </p>
        )}
      </form>

      <div className="panel">
        {view.status === 'loading' && <Loading label="집계하는 중입니다" />}
        {view.status === 'failed' && (
          <div className="panel__empty">
            <button type="button" className="btn" onClick={retry}>
              다시 시도
            </button>
          </div>
        )}
        {view.status === 'loaded' && (
          <>
            <div className="panel__head">
              <span>
                {view.data.start === view.data.end ? view.data.start : `${view.data.start} ~ ${view.data.end}`}
              </span>
              <span>
                합계 <strong>{total.toLocaleString('ko-KR')}</strong>회
              </span>
            </div>
            {noRecords && (
              <p className="panel__note" role="status">
                해당 기간의 조회 기록이 없습니다.
              </p>
            )}
            {stats.length > 0 && <BarChart stats={stats} />}
          </>
        )}
      </div>

      {popup && (
        <Popup
          message="통계를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
          onClose={() => setPopup(false)}
          actions={
            <button type="button" className="btn" onClick={retry}>
              다시 시도
            </button>
          }
        />
      )}
    </section>
  )
}
