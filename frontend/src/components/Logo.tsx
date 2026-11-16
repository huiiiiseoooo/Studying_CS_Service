import { Link } from 'react-router'

export function Logo() {
  return (
    <Link to="/" className="logo" aria-label="Cstudy 처음 화면">
      <span className="logo__mark" aria-hidden="true">
        <i />
        <i />
        <i />
      </span>
      <span className="logo__text">Cstudy</span>
    </Link>
  )
}
