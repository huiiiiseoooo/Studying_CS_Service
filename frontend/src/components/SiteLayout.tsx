import { Link, Outlet } from 'react-router'
import { Logo } from './Logo.tsx'

/** 사용자 화면 공통 틀: 머리글 + 본문 + 바닥글. */
export function SiteLayout() {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar__inner">
          <Logo />
          <span className="topbar__tagline">움직이는 그림으로 배우는 CS 기초</span>
        </div>
      </header>
      <main className="main">
        <Outlet />
      </main>
      <footer className="footer">
        <span>Cstudy</span>
        <Link to="/admin/login" className="footer__link">
          운영자
        </Link>
      </footer>
    </div>
  )
}
