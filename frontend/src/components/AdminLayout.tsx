import { Navigate, NavLink, Outlet, useNavigate } from 'react-router'
import { clearSession, getToken } from '../auth/session.ts'
import { Logo } from './Logo.tsx'

/**
 * 운영자 전용 화면 틀. 토큰이 없으면 화면을 그리지 않고 로그인으로 보낸다. (NFR-002)
 * 실제 차단은 서버가 하며(401), 여기서는 화면 진입만 막는다.
 */
export function AdminLayout() {
  const navigate = useNavigate()

  if (!getToken()) {
    return <Navigate to="/admin/login" replace state={{ notice: '로그인이 필요합니다.' }} />
  }

  const logout = () => {
    clearSession()
    navigate('/admin/login', { replace: true })
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar__inner">
          <Logo />
          <span className="pill pill--accent">운영자</span>
          <button type="button" className="btn btn--ghost topbar__end" onClick={logout}>
            로그아웃
          </button>
        </div>
      </header>
      <div className="admin">
        <nav className="admin__menu" aria-label="운영자 메뉴">
          <NavLink to="/admin/stats" className="admin__menu-item">
            이용 통계
          </NavLink>
        </nav>
        <main className="admin__main">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
