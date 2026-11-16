import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { login } from '../api/admin.ts'
import { ApiError } from '../api/client.ts'
import { getToken, saveSession } from '../auth/session.ts'
import { Popup } from '../components/Popup.tsx'
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts'

/** 운영자 로그인. (UC-003) */
export function AdminLoginPage() {
  useDocumentTitle('운영자 로그인')
  const navigate = useNavigate()
  const notice = (useLocation().state as { notice?: string } | null)?.notice

  const [id, setId] = useState('')
  const [password, setPassword] = useState('')
  const [hint, setHint] = useState<string | null>(null)
  const [popup, setPopup] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (getToken()) return <Navigate to="/admin/stats" replace />

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    // 1a. 비어 있으면 서버에 요청하지 않는다.
    if (!id.trim() || !password) {
      setHint('아이디와 비밀번호를 입력해 주세요.')
      return
    }
    setHint(null)
    setBusy(true)
    try {
      const result = await login(id.trim(), password)
      saveSession(result.accessToken, result.expiresInSeconds)
      navigate('/admin/stats', { replace: true })
    } catch (error) {
      // 2a. 어느 항목이 틀렸는지는 알리지 않는다.  2b. 서버·DB 오류.
      const mismatch = error instanceof ApiError && error.status === 401
      setPopup(
        mismatch
          ? '계정이 존재하지 않거나 비밀번호가 잘못되었습니다.'
          : '일시적인 오류로 로그인할 수 없습니다. 잠시 후 다시 시도해 주세요.',
      )
      setPassword('')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="login">
      <h1 className="login__title">운영자 로그인</h1>
      {notice && (
        <p className="login__notice" role="status">
          {notice}
        </p>
      )}
      <form className="login__form" onSubmit={submit} noValidate>
        <label className="field">
          <span>아이디</span>
          <input
            type="text"
            name="username"
            autoComplete="username"
            value={id}
            onChange={(event) => setId(event.target.value)}
            autoFocus
          />
        </label>
        <label className="field">
          <span>비밀번호</span>
          <input
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        {hint && (
          <p className="field__hint" role="alert">
            {hint}
          </p>
        )}
        <button type="submit" className="btn btn--block" disabled={busy}>
          {busy ? '확인하는 중…' : '로그인'}
        </button>
      </form>
      {popup && <Popup message={popup} onClose={() => setPopup(null)} />}
    </section>
  )
}
