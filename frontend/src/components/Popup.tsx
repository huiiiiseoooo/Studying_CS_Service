import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'

interface Props {
  message: string
  onClose: () => void
  /** 닫기 외에 추가로 둘 버튼 (예: 다시 시도) */
  actions?: ReactNode
}

/** 안내 팝업. Esc 또는 [확인]으로 닫는다. */
export function Popup({ message, onClose, actions }: Props) {
  const closeRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    closeRef.current?.focus()
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="popup" role="presentation" onClick={onClose}>
      <div
        className="popup__box"
        role="alertdialog"
        aria-modal="true"
        aria-describedby="popup-message"
        onClick={(event) => event.stopPropagation()}
      >
        <p id="popup-message">{message}</p>
        <div className="popup__actions">
          {actions}
          <button ref={closeRef} type="button" className={actions ? 'btn btn--ghost' : 'btn'} onClick={onClose}>
            {actions ? '닫기' : '확인'}
          </button>
        </div>
      </div>
    </div>
  )
}
