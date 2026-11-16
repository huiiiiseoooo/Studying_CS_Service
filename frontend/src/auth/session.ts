/**
 * 운영자 Access Token 보관. (TC-006)
 * sessionStorage에 두어 새로고침에는 유지되고 탭을 닫으면 사라진다.
 * 만료 시각이 지나면 없는 것으로 취급한다. (서버도 만료된 토큰을 401로 거부한다)
 */
const KEY = 'cstudy.admin'

interface Stored {
  token: string
  expiresAt: number
}

export function saveSession(token: string, expiresInSeconds: number): void {
  const stored: Stored = { token, expiresAt: Date.now() + expiresInSeconds * 1000 }
  try {
    sessionStorage.setItem(KEY, JSON.stringify(stored))
  } catch {
    // 저장소를 쓸 수 없는 환경이면 로그인 상태가 유지되지 않을 뿐이다.
  }
}

export function getToken(): string | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const stored = JSON.parse(raw) as Stored
    if (!stored.token || stored.expiresAt <= Date.now()) {
      sessionStorage.removeItem(KEY)
      return null
    }
    return stored.token
  } catch {
    return null
  }
}

export function clearSession(): void {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // 무시
  }
}
